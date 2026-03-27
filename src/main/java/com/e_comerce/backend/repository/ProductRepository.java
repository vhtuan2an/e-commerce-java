package com.e_comerce.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.e_comerce.backend.model.Category;
import com.e_comerce.backend.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    List<Product> findByCategoryOrderByPriceAsc(Category category);
    List<Product> findByProductNameContainingIgnoreCase(String name);
    Optional<Product> findByProductName(String name);
}
