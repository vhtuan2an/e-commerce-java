package com.e_comerce.backend.payload.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MomoCreatePaymentResponse {
    private String partnerCode;
    private String orderId;
    private String requestId;
    private Long amount;
    private Integer resultCode;
    private String message;
    private String payUrl;
    private String shortLink;
    private Long transId;
}
