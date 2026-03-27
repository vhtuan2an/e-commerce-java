package com.e_comerce.backend.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.e_comerce.backend.exception.APIException;
import com.e_comerce.backend.exception.ResourceNotFoundException;
import com.e_comerce.backend.model.Cart;
import com.e_comerce.backend.model.CartItem;
import com.e_comerce.backend.model.Category;
import com.e_comerce.backend.model.Product;
import com.e_comerce.backend.payload.dto.CartDTO;
import com.e_comerce.backend.payload.dto.CartItemDTO;
import com.e_comerce.backend.payload.dto.ProductDTO;
import com.e_comerce.backend.payload.response.ProductResponse;
import com.e_comerce.backend.repository.CartRepository;
import com.e_comerce.backend.repository.CategoryRepository;
import com.e_comerce.backend.repository.ProductRepository;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Autowired
    private CartRepository cartRepository;

    @Value("${project.image}")
    private String imagePath;

    private static final String DEFAULT_IMAGE_URL = "http://localhost:8080/images/default-product.jpg";

    private ProductDTO convertToDTO(Product product) {
        ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
        productDTO.setImage(DEFAULT_IMAGE_URL);
        return productDTO;
    }

    @Override
    public ProductDTO addProduct(ProductDTO productDTO, Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        
        Optional<Product> existingProduct = productRepository.findByProductName(productDTO.getProductName());
        if (existingProduct.isPresent()) {
            throw new APIException("Product with the same name already exists");
        }
        
        // boolean isProductExists = false;
        // List<Product> products = category.getProducts();
        // for (Product product : products) {
        //     if (product.getProductName().equalsIgnoreCase(productDTO.getProductName())) {
        //         isProductExists = true;
        //         break;
        //     }
        // }

        // if (isProductExists) {
        //     throw new APIException("Product with the same name already exists in this category");
        // }

        Product product = modelMapper.map(productDTO, Product.class);
        product.setCategory(category);
        product.setImage("http://localhost:8080/images/default-image.jpg");
        double specialPrice = product.getPrice() - (product.getPrice() * product.getDiscount() / 100);
        product.setSpecialPrice(specialPrice);  
        Product savedProduct = productRepository.save(product);
        return convertToDTO(savedProduct);
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String query, String category) {
        Sort sort = sortOrder.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
        
        Pageable pageDetails = PageRequest.of(pageNumber, pageSize, sort);
        Specification<Product> spec = (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (query != null && !query.isEmpty()) {
            spec = spec.and((root, query1, criteriaBuilder) -> 
                criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + query.toLowerCase() + "%")
            );
        }

        if (category != null && !category.isEmpty() && !category.equalsIgnoreCase("all")) {
            spec = spec.and((root, query1, criteriaBuilder) -> 
                criteriaBuilder.equal(root.get("category").get("categoryName"), category)
            );
        }
        
        Page<Product> productsPage = productRepository.findAll(spec, pageDetails);
        
        if (productsPage.isEmpty()) {
            throw new APIException("No product found");
        }

        List<Product> products = productsPage.getContent();
        
        List<ProductDTO> productDTOs = products.stream()
                .map(this::convertToDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOs);
        productResponse.setPageNumber(pageDetails.getPageNumber());
        productResponse.setPageSize(pageDetails.getPageSize()); 
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setIsLast(productsPage.isLast());
        return productResponse;
    }

    @Override
    public ProductResponse getProductsByCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        List<Product> products = productRepository.findByCategoryOrderByPriceAsc(category);
        if (products.isEmpty()) {
            throw new APIException("No products found for category with id " + categoryId);
        }
        List<ProductDTO> productDTOs = products.stream()
                .map(this::convertToDTO)
                .toList();
                
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOs);
        return productResponse;
    }

    @Override
    public ProductResponse searchProducts(String query) {
        List<Product> products = productRepository.findByProductNameContainingIgnoreCase(query);
        if (products.isEmpty()) {
            throw new APIException("No products found matching query: " + query);
        }
        List<ProductDTO> productDTOs = products.stream()
                .map(this::convertToDTO)
                .toList();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDTOs);
        return productResponse;
    }

    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        // Config model mapper to skip null values
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.map(productDTO, existingProduct);

        if (productDTO.getPrice() != null && productDTO.getDiscount() != null) {
            double specialPrice = existingProduct.getPrice() - (existingProduct.getPrice() * existingProduct.getDiscount() / 100);
            existingProduct.setSpecialPrice(specialPrice);
        }

        Product updatedProduct = productRepository.save(existingProduct);

        List<Cart> carts = cartRepository.findCartsByProductId(productId);

        carts.forEach(cart -> {
            CartItem cartItem = cart.getCartItems().stream()
                     .filter(ci -> ci.getProduct().getProductId().equals(productId))
                     .findFirst()
                     .orElseThrow(() -> new ResourceNotFoundException("CartItem", "productId", productId));                

            double oldItemTotal = cartItem.getProductPrice() * cartItem.getQuantity();
            cartItem.setDiscount(updatedProduct.getDiscount());
            cartItem.setProductPrice(updatedProduct.getSpecialPrice());

            double newItemTotal = updatedProduct.getSpecialPrice() * cartItem.getQuantity();
            cart.setTotalPrice(cart.getTotalPrice() - oldItemTotal + newItemTotal);

            cartRepository.save(cart);
        }); 

        return convertToDTO(updatedProduct);
    }

    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product existingProduct = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        productRepository.delete(existingProduct);
        return convertToDTO(existingProduct);
    }

    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product existingProduct = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Simulate image upload and get the image URL
        String path = imagePath + productId;
        String imageUrl = fileService.uploadImage(path, image);
        
        existingProduct.setImage(imageUrl);
        Product updatedProduct = productRepository.save(existingProduct);
        return convertToDTO(updatedProduct);
    }    
}