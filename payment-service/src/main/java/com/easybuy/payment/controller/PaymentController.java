package com.easybuy.payment.controller;

import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.dto.PaymentVerificationRequest;
import com.easybuy.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Create / Initiate a Razorpay payment order.
     */
    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponse> createPaymentOrder(@Valid @RequestBody PaymentRequest paymentRequest) {
        log.info("REST request to initiate payment for Order ID: {}", paymentRequest.getOrderId());
        PaymentResponse response = paymentService.processPayment(paymentRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Verify payment signature received from Razorpay Checkout modal after user pays.
     */
    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@Valid @RequestBody PaymentVerificationRequest verificationRequest) {
        log.info("REST request to verify payment for Razorpay Order ID: {}", verificationRequest.getRazorpayOrderId());
        PaymentResponse response = paymentService.verifyPayment(verificationRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Razorpay Webhook listener for asynchronous events (payment.captured, payment.failed, order.paid).
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signatureHeader) {
        log.info("Received Razorpay webhook request");
        paymentService.handleWebhook(payload, signatureHeader);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook processed successfully"));
    }

    /**
     * Get payment transaction by Order ID.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable Long orderId) {
        PaymentResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get payment transaction by Transaction ID.
     */
    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<PaymentResponse> getPaymentByTransactionId(@PathVariable String transactionId) {
        PaymentResponse response = paymentService.getPaymentByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }
}
