package com.e_comerce.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.e_comerce.backend.config.MomoConfig;
import com.e_comerce.backend.exception.APIException;
import com.e_comerce.backend.payload.dto.MomoRequestDTO;
import com.e_comerce.backend.payload.response.MomoCreatePaymentResponse;

@Service
public class MomoGatewayServiceImpl implements MomoGatewayService {

    private final MomoConfig momoConfig;
    private final RestClient restClient;

    public MomoGatewayServiceImpl(MomoConfig momoConfig, RestClient.Builder restClientBuilder) {
        this.momoConfig = momoConfig;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public MomoCreatePaymentResponse createPayment(Long orderId, Long amount) {
        String requestId = "REQ_" + orderId + "_" + System.currentTimeMillis();
        String momoOrderId = "ORDER_" + orderId + "_" + System.currentTimeMillis();
        String orderInfo = "Thanh toan don hang #" + orderId;
        String extraData = "";

        // Raw hash string — fields MUST be in alphabetical order (MoMo v2 spec)
        String rawHash = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + momoConfig.getIpnUrl()
                + "&orderId=" + momoOrderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + momoConfig.getPartnerCode()
                + "&redirectUrl=" + momoConfig.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + momoConfig.getRequestType();

        String signature = signHmacSHA256(rawHash, momoConfig.getSecretKey());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", momoConfig.getPartnerCode());
        payload.put("accessKey", momoConfig.getAccessKey());
        payload.put("requestId", requestId);
        payload.put("amount", amount);
        payload.put("orderId", momoOrderId);
        payload.put("orderInfo", orderInfo);
        payload.put("redirectUrl", momoConfig.getRedirectUrl());
        payload.put("ipnUrl", momoConfig.getIpnUrl());
        payload.put("extraData", extraData);
        payload.put("requestType", momoConfig.getRequestType());
        payload.put("signature", signature);
        payload.put("lang", "vi");

        try {
            MomoCreatePaymentResponse response = restClient.post()
                    .uri(momoConfig.getEndpointCreate())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(MomoCreatePaymentResponse.class);

            if (response == null) {
                throw new APIException("MoMo returned empty response");
            }
            return response;
        } catch (RestClientException ex) {
            throw new APIException("Failed to call MoMo API: " + ex.getMessage());
        }
    }

    @Override
    public boolean verifyIpnSignature(MomoRequestDTO request) {
        // Raw hash string for IPN verification — fields in alphabetical order (MoMo v2 spec)
        String rawHash = "accessKey=" + momoConfig.getAccessKey()
                + "&amount=" + request.getAmount()
                + "&extraData=" + request.getExtraData()
                + "&message=" + request.getMessage()
                + "&orderId=" + request.getOrderId()
                + "&orderInfo=" + request.getOrderInfo()
                + "&orderType=" + request.getOrderType()
                + "&partnerCode=" + request.getPartnerCode()
                + "&payType=" + request.getPayType()
                + "&requestId=" + request.getRequestId()
                + "&responseTime=" + request.getResponseTime()
                + "&resultCode=" + request.getResultCode()
                + "&transId=" + request.getTransId();

        String expectedSignature = signHmacSHA256(rawHash, momoConfig.getSecretKey());
        return expectedSignature.equals(request.getSignature());
    }

    private String signHmacSHA256(String data, String secretKey) {
        try {
            Mac hmacSha256 = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmacSha256.init(keySpec);
            byte[] hash = hmacSha256.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new APIException("Cannot sign MoMo request: " + ex.getMessage());
        }
    }
}
