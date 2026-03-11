package com.e_comerce.backend.service;

import java.util.List;

import com.e_comerce.backend.payload.dto.CartDTO;

public interface CartService {
    public CartDTO addProductToCart(Long productId, int quantity);
    public List<CartDTO> getAllCarts();
    public CartDTO getUserCart();
}
