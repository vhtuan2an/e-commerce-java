package com.e_comerce.backend.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.e_comerce.backend.exception.APIException;
import com.e_comerce.backend.model.Cart;
import com.e_comerce.backend.model.CartItem;
import com.e_comerce.backend.model.Order;
import com.e_comerce.backend.model.OrderItem;
import com.e_comerce.backend.model.Payment;
import com.e_comerce.backend.payload.dto.MomoRequestDTO;
import com.e_comerce.backend.payload.response.MomoCreatePaymentResponse;
import com.e_comerce.backend.repository.CartItemRepository;
import com.e_comerce.backend.repository.CartRepository;
import com.e_comerce.backend.repository.OrderRepository;
import com.e_comerce.backend.repository.PaymentRepository;
import com.e_comerce.backend.util.AuthUtil;

import jakarta.transaction.Transactional;

@Service
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final MomoGatewayService momoGatewayService;
    private final AuthUtil authUtil;

    public OrderServiceImpl(
            CartRepository cartRepository,
            CartItemRepository cartItemRepository,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            MomoGatewayService momoGatewayService,
            AuthUtil authUtil) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.momoGatewayService = momoGatewayService;
        this.authUtil = authUtil;
    }

    @Override
    @Transactional
    public MomoCreatePaymentResponse createMomoPaymentFromCart() {
        // 1. Lấy cart của user đang đăng nhập
        Cart cart = cartRepository.findCartByUserId(authUtil.loggedInUserId());
        if (cart == null || cart.getCartItems().isEmpty()) {
            throw new APIException("Cart is empty. Please add products before placing an order.");
        }

        // 2. Tạo Order, lưu trước để lấy orderId (dùng cho MoMo momoOrderId)
        Order order = new Order();
        order.setEmail(authUtil.loggedInUser().getEmail());
        order.setOrderDate(LocalDate.now());
        order.setStatus("WAITING_PAYMENT");
        order.setTotalAmount(Math.round(cart.getTotalPrice()));
        order = orderRepository.save(order);

        // 3. Tạo OrderItems từ CartItems
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(cartItem.getProduct());
            item.setQuantity(cartItem.getQuantity());
            item.setPrice(cartItem.getProductPrice());
            item.setOrderedProductPrice(cartItem.getProductPrice());
            orderItems.add(item);
        }
        order.setOrderItems(orderItems);
        order = orderRepository.save(order);  // cascade PERSIST saves OrderItems

        // 4. Gọi MoMo API
        MomoCreatePaymentResponse momoResponse =
                momoGatewayService.createPayment(order.getOrderId(), order.getTotalAmount());

        // 5. Lưu Payment record
        Payment payment = new Payment();
        payment.setProvider("MOMO");
        payment.setPaymentMethod("captureWallet");
        payment.setAmount(order.getTotalAmount());
        payment.setPaymentStatus("PENDING");
        payment.setMomoOrderId(momoResponse.getOrderId());
        payment.setMomoRequestId(momoResponse.getRequestId());
        payment.setPayUrl(momoResponse.getPayUrl());
        payment.setResultCode(momoResponse.getResultCode());
        payment.setMessage(momoResponse.getMessage());
        payment = paymentRepository.save(payment);

        // 6. Liên kết Payment vào Order
        order.setPayment(payment);
        orderRepository.save(order);

        // 7. Xóa cart sau khi tạo đơn thành công
        cartItemRepository.deleteAll(cart.getCartItems());
        cart.getCartItems().clear();
        cart.setTotalPrice(0.0);
        cartRepository.save(cart);

        return momoResponse;
    }

    @Override
    @Transactional
    public void handleMomoIpn(MomoRequestDTO request) {
        // 1. Verify chữ ký từ MoMo
        if (!momoGatewayService.verifyIpnSignature(request)) {
            throw new APIException("Invalid MoMo IPN signature");
        }

        // 2. Tìm payment theo momoOrderId
        Payment payment = paymentRepository.findByMomoOrderId(request.getOrderId())
                .orElseThrow(() -> new APIException("Payment not found for MoMo order: " + request.getOrderId()));

        // 3. Idempotency — bỏ qua nếu đã xử lý thành công trước đó
        if ("SUCCESS".equals(payment.getPaymentStatus())) {
            return;
        }

        // 4. Kiểm tra amount khớp
        if (!payment.getAmount().equals(request.getAmount())) {
            throw new APIException("MoMo IPN amount mismatch for order: " + request.getOrderId());
        }

        // 5. Cập nhật thông tin từ callback
        payment.setResultCode(request.getResultCode());
        payment.setMessage(request.getMessage());
        payment.setMomoTransId(request.getTransId());

        Order order = payment.getOrder();

        // 6. Cập nhật trạng thái theo resultCode
        if (request.getResultCode() == 0) {
            payment.setPaymentStatus("SUCCESS");
            order.setStatus("PAID");
        } else {
            payment.setPaymentStatus("FAILED");
            order.setStatus("PAYMENT_FAILED");
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
    }
}
