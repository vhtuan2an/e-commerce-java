package com.e_comerce.backend.service;

import com.e_comerce.backend.payload.dto.MomoRequestDTO;
import com.e_comerce.backend.payload.response.MomoCreatePaymentResponse;

public interface OrderService {
    MomoCreatePaymentResponse createMomoPaymentFromCart();
    void handleMomoIpn(MomoRequestDTO request);
}
