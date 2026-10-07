package com.easybuy.payment;

import com.easybuy.common.dto.constants.PaymentMethod;
import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.payment.controller.PaymentController;
import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.dto.PaymentVerificationRequest;
import com.easybuy.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController).build();
    }

    @Test
    @DisplayName("POST /api/v1/payments/create-order returns 201 Created")
    void testCreatePaymentOrder_Success() throws Exception {
        PaymentRequest request = PaymentRequest.builder()
                .orderId(123L)
                .totalAmount(new BigDecimal("999.00"))
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentDetails("UPI")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .orderId(123L)
                .amount(new BigDecimal("999.00"))
                .status(PaymentStatus.PENDING)
                .paymentGatewayOrderId("order_mock_abc123")
                .transactionId("TXN_12345")
                .currency("INR")
                .build();

        when(paymentService.processPayment(any(PaymentRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/create-order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(123))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentGatewayOrderId").value("order_mock_abc123"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/verify returns 200 OK")
    void testVerifyPayment_Success() throws Exception {
        PaymentVerificationRequest verifyRequest = PaymentVerificationRequest.builder()
                .orderId(123L)
                .razorpayOrderId("order_mock_abc123")
                .razorpayPaymentId("pay_mock_xyz789")
                .razorpaySignature("sig_mock_123")
                .build();

        PaymentResponse response = PaymentResponse.builder()
                .orderId(123L)
                .status(PaymentStatus.PAID)
                .paymentGatewayTxnId("pay_mock_xyz789")
                .paymentGatewayOrderId("order_mock_abc123")
                .build();

        when(paymentService.verifyPayment(any(PaymentVerificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/payments/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paymentGatewayTxnId").value("pay_mock_xyz789"));
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook returns 200 OK")
    void testWebhook_Success() throws Exception {
        String payload = "{\"event\":\"payment.captured\"}";

        doNothing().when(paymentService).handleWebhook(anyString(), any());

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Razorpay-Signature", "sample_sig")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        verify(paymentService, times(1)).handleWebhook(anyString(), eq("sample_sig"));
    }

    @Test
    @DisplayName("GET /api/v1/payments/order/{orderId} returns payment details")
    void testGetPaymentByOrderId_Success() throws Exception {
        PaymentResponse response = PaymentResponse.builder()
                .orderId(555L)
                .status(PaymentStatus.PAID)
                .amount(new BigDecimal("499.00"))
                .transactionId("TXN_555")
                .build();

        when(paymentService.getPaymentByOrderId(555L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/order/555"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(555))
                .andExpect(jsonPath("$.status").value("PAID"));
    }
}
