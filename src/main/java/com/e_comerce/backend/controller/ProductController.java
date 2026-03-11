package com.e_comerce.backend.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.e_comerce.backend.config.AppConstants;
import com.e_comerce.backend.model.Product;
import com.e_comerce.backend.payload.dto.ProductDTO;
import com.e_comerce.backend.payload.response.ProductResponse;
import com.e_comerce.backend.service.ProductService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;




@RestController
@RequestMapping("/api")
public class ProductController {

    @Autowired
    ProductService productService;

    @PostMapping("/admin/{categoryId}/product")
    public ResponseEntity<ProductDTO> addProduct(@Valid @RequestBody ProductDTO productDTO, 
                                                @PathVariable Long categoryId) {
        ProductDTO saveProductDTO = productService.addProduct(productDTO, categoryId);
        return new ResponseEntity<>(saveProductDTO, HttpStatus.CREATED);
    }

    @GetMapping("/public/products")
    public ResponseEntity<ProductResponse> getAllProduct(
        @RequestParam (name = "pageNumber", 
            defaultValue = AppConstants.DEFAULT_PAGE_NUMBER, 
            required = false) 
            Integer pageNumber,
        @RequestParam (name = "pageSize", 
            defaultValue = AppConstants.DEFAULT_PAGE_SIZE,
            required = false)
            Integer pageSize,
        @RequestParam (name = "sortBy", 
            defaultValue = AppConstants.SORT_BY_DEFAULT,
            required = false)
            String sortBy,
        @RequestParam (name = "sortOrder", 
            defaultValue = AppConstants.SORT_ORDER_DEFAULT,
            required = false)
            String sortOrder
    ) {
        ProductResponse productResponse = productService.getAllProducts(pageNumber, pageSize, sortBy, sortOrder);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/categories/{categoryId}/products")
    public ResponseEntity<ProductResponse> getProductByCategory(
        @PathVariable Long categoryId
    ) {
        ProductResponse productResponse = productService.getProductsByCategory(categoryId);
        return new ResponseEntity<>(productResponse, HttpStatus.OK);
    }

    @GetMapping("/public/products/search")
    public ResponseEntity<ProductResponse> searchProducts(
        @RequestParam (name = "query", defaultValue = "") String query
    ) {
        ProductResponse productResponse = productService.searchProducts(query);
        return new ResponseEntity<>(productResponse, HttpStatus.FOUND);
    }
    
    @PutMapping("/admin/product/{productId}")
    public ResponseEntity<ProductDTO> updateProduct(
        @PathVariable Long productId, 
        @Valid @RequestBody ProductDTO productDTO) {
        ProductDTO updatedProduct = productService.updateProduct(productId, productDTO);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @PutMapping("/admin/product/{productId}/image")
    public ResponseEntity<ProductDTO> updateProductImage(
        @PathVariable Long productId, 
        @RequestParam("image") MultipartFile image) throws IOException {
        ProductDTO updatedProduct = productService.updateProductImage(productId, image);
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }
    
    @DeleteMapping("/admin/product/{productId}")
    public ResponseEntity<ProductDTO> deleteProduct(@PathVariable Long productId) {
        ProductDTO deletedProduct = productService.deleteProduct(productId);
        return new ResponseEntity<>(deletedProduct, HttpStatus.OK);
    }
}
