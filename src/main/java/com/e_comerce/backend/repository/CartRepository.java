package com.e_comerce.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.e_comerce.backend.model.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    @Query("SELECT c FROM Cart c WHERE c.user.userId = :userId")
    Cart findCartByUserId(Long userId);

    // Join fetch to avoid lazy loading
    @Query("SELECT c FROM Cart c JOIN FETCH c.cartItems ci WHERE ci.product.productId = :productId")
    List<Cart> findCartsByProductId(Long productId);
}
