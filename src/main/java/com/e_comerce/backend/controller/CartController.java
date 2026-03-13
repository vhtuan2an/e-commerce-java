package com.e_comerce.backend.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.e_comerce.backend.payload.dto.CartDTO;
import com.e_comerce.backend.service.CartService;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;

    @PostMapping("/product/{productId}/quantity/{quantity}")
    // @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CartDTO> addProductToCart(
        @PathVariable Long productId, 
        @PathVariable Integer quantity
    ) {
        CartDTO cartDTO = cartService.addProductToCart(productId, quantity);
        return new ResponseEntity<CartDTO>(cartDTO, HttpStatus.CREATED);
    }
    
    @GetMapping("/")
    public ResponseEntity<List<CartDTO>> getCarts() {
        List<CartDTO> cartDTOs = cartService.getAllCarts();
        return new ResponseEntity<>(cartDTOs, HttpStatus.FOUND);
    }

    @GetMapping("/user")
    public ResponseEntity<CartDTO> getUserCart() {
        CartDTO cartDTO = cartService.getUserCart();
        return new ResponseEntity<>(cartDTO, HttpStatus.FOUND);
    }

    @PutMapping("/product/{productId}/quantity/{quantity}")
    public ResponseEntity<CartDTO> updateProductQuantity(
        @PathVariable Long productId, 
        @PathVariable Integer quantity
    ) {
        CartDTO cartDTO = cartService.updateProductQuantity(productId, quantity);
        return new ResponseEntity<>(cartDTO, HttpStatus.OK);
    }
    
    @DeleteMapping("/{cartId}/product/{productId}")
    public ResponseEntity<String> deleteProductFromCart(
        @PathVariable Long cartId, 
        @PathVariable Long productId
    ) {
        String response = cartService.deleteProductFromCart(cartId, productId);
        return new ResponseEntity<String>(response, HttpStatus.OK);
    }
}
