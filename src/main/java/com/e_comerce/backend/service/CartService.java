package com.e_comerce.backend.service;

import java.util.List;

import com.e_comerce.backend.payload.dto.CartDTO;

import jakarta.transaction.Transactional;

public interface CartService {
    public CartDTO addProductToCart(Long productId, int quantity);
    public List<CartDTO> getAllCarts();
    public CartDTO getUserCart();

    @Transactional
    public CartDTO updateProductQuantity(Long productId, int quantity);

    public String deleteProductFromCart(Long cartId, Long productId);
    
}
