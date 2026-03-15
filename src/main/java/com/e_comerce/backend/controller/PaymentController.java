package com.e_comerce.backend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.e_comerce.backend.payload.dto.MomoRequestDTO;
import com.e_comerce.backend.payload.response.MomoCreatePaymentResponse;
import com.e_comerce.backend.service.OrderService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private OrderService orderService;

    /**
     * Tạo giao dịch MoMo từ cart của user đang đăng nhập.
     * Trả về payUrl để frontend redirect sang trang thanh toán MoMo.
     */
    @PostMapping("/momo/create")
    public ResponseEntity<MomoCreatePaymentResponse> createMomoPayment() {
        return ResponseEntity.ok(orderService.createMomoPaymentFromCart());
    }

    /**
     * MoMo gọi endpoint này (server-to-server) sau khi giao dịch hoàn tất.
     * Đây là nguồn sự thật để cập nhật trạng thái đơn hàng.
     * Phải trả về HTTP 204 trong vòng 15 giây.
     */
    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> momoIpn(@RequestBody MomoRequestDTO request) {
        orderService.handleMomoIpn(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * MoMo redirect user về URL này sau khi thanh toán (phía trình duyệt).
     * KHÔNG dùng để chốt đơn hàng — chỉ dùng để hiển thị trạng thái cho user.
     */
    @GetMapping("/momo/return")
    public ResponseEntity<Map<String, Object>> momoReturn(
            @RequestParam String orderId,
            @RequestParam Integer resultCode,
            @RequestParam(required = false) String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("orderId", orderId);
        response.put("resultCode", resultCode);
        response.put("success", resultCode == 0);
        response.put("message", message != null ? message : "");
        return ResponseEntity.ok(response);
    }
}
