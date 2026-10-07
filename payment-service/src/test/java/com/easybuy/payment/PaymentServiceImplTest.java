package com.easybuy.payment;

import com.easybuy.common.dto.constants.PaymentMethod;
import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.common.events.PaymentEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.payment.config.RazorpayConfig;
import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.dto.PaymentVerificationRequest;
import com.easybuy.payment.entity.Transaction;
import com.easybuy.payment.producer.PaymentEventProducer;
import com.easybuy.payment.repository.PaymentRepository;
import com.easybuy.payment.service.implementations.PaymentServiceImpl;
import com.razorpay.RazorpayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private RazorpayClient razorpayClient;

    @Mock
    private RazorpayConfig razorpayConfig;

    @Mock
    private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        lenient().when(razorpayConfig.getCurrency()).thenReturn("INR");
        lenient().when(razorpayConfig.getKeyId()).thenReturn("rzp_test_mockKeyId123456");
        lenient().when(razorpayConfig.getKeySecret()).thenReturn("mockSecretKeyRazorpay987654");
        lenient().when(razorpayConfig.getWebhookSecret()).thenReturn("mockWebhookSecret123456");
    }

    @Test
    @DisplayName("processPayment: Creates Razorpay order in mock mode successfully")
    void testProcessPayment_MockMode_Success() {
        when(razorpayConfig.isMockMode()).thenReturn(true);
        when(paymentRepository.findByOrderId(101L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        PaymentRequest request = PaymentRequest.builder()
                .orderId(101L)
                .totalAmount(new BigDecimal("1500.00"))
                .paymentMethod(PaymentMethod.ONLINE)
                .paymentDetails("Card")
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(101L, response.getOrderId());
        assertEquals(new BigDecimal("1500.00"), response.getAmount());
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertTrue(response.getPaymentGatewayOrderId().startsWith("order_mock_"));
        verify(paymentRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("processPayment: Handles OFFLINE (Cash on Delivery) without gateway call")
    void testProcessPayment_Offline_Success() {
        when(paymentRepository.findByOrderId(102L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(2L);
            return t;
        });

        PaymentRequest request = PaymentRequest.builder()
                .orderId(102L)
                .totalAmount(new BigDecimal("500.00"))
                .paymentMethod(PaymentMethod.OFFLINE)
                .build();

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals(PaymentStatus.PENDING, response.getStatus());
        assertEquals(PaymentMethod.OFFLINE, response.getPaymentMethod());
        assertTrue(response.getPaymentGatewayTxnId().startsWith("COD_"));
    }

    @Test
    @DisplayName("verifyPayment: Successful verification in mock mode marks PAID and emits Kafka event")
    void testVerifyPayment_MockMode_Success() {
        when(razorpayConfig.isMockMode()).thenReturn(true);

        Transaction existingTxn = new Transaction();
        existingTxn.setId(10L);
        existingTxn.setOrderId(101L);
        existingTxn.setAmount(new BigDecimal("1500.00"));
        existingTxn.setPaymentMethod(PaymentMethod.ONLINE);
        existingTxn.setStatus(PaymentStatus.PENDING);
        existingTxn.setPaymentGatewayOrderId("order_mock_12345");
        existingTxn.setTransactionId("TXN_123456");

        when(paymentRepository.findByPaymentGatewayOrderId("order_mock_12345")).thenReturn(Optional.of(existingTxn));
        when(paymentRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentVerificationRequest verifyRequest = PaymentVerificationRequest.builder()
                .orderId(101L)
                .razorpayOrderId("order_mock_12345")
                .razorpayPaymentId("pay_mock_98765")
                .razorpaySignature("mock_valid_signature")
                .build();

        PaymentResponse response = paymentService.verifyPayment(verifyRequest);

        assertNotNull(response);
        assertEquals(PaymentStatus.PAID, response.getStatus());
        assertEquals("pay_mock_98765", response.getPaymentGatewayTxnId());

        verify(paymentEventProducer, times(1)).publishPaymentEvent(any(Transaction.class), eq(PaymentStatus.PAID));
    }

    @Test
    @DisplayName("verifyPayment: Failed verification marks transaction FAILED and throws BusinessException")
    void testVerifyPayment_Failure_MarksFailed() {
        when(razorpayConfig.isMockMode()).thenReturn(true);

        Transaction existingTxn = new Transaction();
        existingTxn.setId(10L);
        existingTxn.setOrderId(101L);
        existingTxn.setAmount(new BigDecimal("1500.00"));
        existingTxn.setPaymentMethod(PaymentMethod.ONLINE);
        existingTxn.setStatus(PaymentStatus.PENDING);
        existingTxn.setPaymentGatewayOrderId("order_mock_12345");

        when(paymentRepository.findByPaymentGatewayOrderId("order_mock_12345")).thenReturn(Optional.of(existingTxn));
        when(paymentRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentVerificationRequest verifyRequest = PaymentVerificationRequest.builder()
                .orderId(101L)
                .razorpayOrderId("order_mock_12345")
                .razorpayPaymentId("pay_mock_98765")
                .razorpaySignature("mock_fail_signature") // triggers failure in mock mode
                .build();

        assertThrows(BusinessException.class, () -> paymentService.verifyPayment(verifyRequest));

        assertEquals(PaymentStatus.FAILED, existingTxn.getStatus());
        verify(paymentEventProducer, times(1)).publishPaymentEvent(any(Transaction.class), eq(PaymentStatus.FAILED));
    }

    @Test
    @DisplayName("handleWebhook: payment.captured updates transaction to PAID")
    void testHandleWebhook_PaymentCaptured_Success() {
        when(razorpayConfig.isMockMode()).thenReturn(true);

        Transaction txn = new Transaction();
        txn.setId(1L);
        txn.setOrderId(200L);
        txn.setStatus(PaymentStatus.PENDING);
        txn.setPaymentGatewayOrderId("order_test_999");

        when(paymentRepository.findByPaymentGatewayOrderId("order_test_999")).thenReturn(Optional.of(txn));
        when(paymentRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String webhookPayload = """
                {
                  "event": "payment.captured",
                  "payload": {
                    "payment": {
                      "entity": {
                        "id": "pay_test_888",
                        "order_id": "order_test_999"
                      }
                    }
                  }
                }
                """;

        paymentService.handleWebhook(webhookPayload, "mock_signature");

        assertEquals(PaymentStatus.PAID, txn.getStatus());
        assertEquals("pay_test_888", txn.getPaymentGatewayTxnId());
        verify(paymentEventProducer, times(1)).publishPaymentEvent(any(Transaction.class), eq(PaymentStatus.PAID));
    }
}
