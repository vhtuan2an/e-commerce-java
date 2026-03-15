package com.e_comerce.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.e_comerce.backend.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
