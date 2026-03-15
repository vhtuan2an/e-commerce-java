package com.e_comerce.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.e_comerce.backend.model.Payment;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByMomoOrderId(String momoOrderId);
    Optional<Payment> findByMomoRequestId(String momoRequestId);
}
