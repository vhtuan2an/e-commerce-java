package com.e_comerce.backend.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.e_comerce.backend.model.Product;
import com.e_comerce.backend.payload.dto.ProductDTO;
import com.e_comerce.backend.payload.response.ProductResponse;

public interface ProductService {
    ProductDTO addProduct(ProductDTO productDTO, Long categoryId);
    ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String query, String category);
    ProductResponse getProductsByCategory(Long categoryId);
    ProductResponse searchProducts(String query);
    ProductDTO updateProduct(Long productId, ProductDTO productDTO);
    ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException;
    ProductDTO deleteProduct(Long productId);
}
