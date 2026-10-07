package com.easybuy.payment.service.implementations;

import com.easybuy.common.dto.constants.PaymentMethod;
import com.easybuy.common.dto.constants.PaymentStatus;
import com.easybuy.common.events.PaymentEvent;
import com.easybuy.common.exceptions.customException.BusinessException;
import com.easybuy.common.exceptions.customException.ResourceNotFoundException;
import com.easybuy.payment.config.RazorpayConfig;
import com.easybuy.payment.dto.PaymentRequest;
import com.easybuy.payment.dto.PaymentResponse;
import com.easybuy.payment.dto.PaymentVerificationRequest;
import com.easybuy.payment.entity.Transaction;
import com.easybuy.payment.producer.PaymentEventProducer;
import com.easybuy.payment.repository.PaymentRepository;
import com.easybuy.payment.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository transactionRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayConfig razorpayConfig;
    private final PaymentEventProducer paymentEventProducer;

    /**
     * Initiates payment for an order.
     * For ONLINE payments, creates a Razorpay Order and saves a PENDING transaction.
     */
    @Override
    public PaymentResponse processPayment(PaymentRequest paymentRequest) {
        log.info("Processing payment initiation for Order ID: {} with amount: {}",
                paymentRequest.getOrderId(), paymentRequest.getTotalAmount());

        if (paymentRequest.getOrderId() == null) {
            throw new BusinessException("Order ID cannot be null");
        }
        if (paymentRequest.getTotalAmount() == null || paymentRequest.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero");
        }

        // Check if a transaction already exists for this order
        Optional<Transaction> existingTxnOpt = transactionRepository.findByOrderId(paymentRequest.getOrderId());
        if (existingTxnOpt.isPresent()) {
            Transaction existingTxn = existingTxnOpt.get();
            if (existingTxn.getStatus() == PaymentStatus.PAID) {
                log.info("Order ID: {} has already been PAID via transaction: {}",
                        paymentRequest.getOrderId(), existingTxn.getTransactionId());
                return transactionToPaymentResponse(existingTxn);
            }
        }

        Transaction transaction = existingTxnOpt.orElseGet(Transaction::new);
        transaction.setOrderId(paymentRequest.getOrderId());
        transaction.setAmount(paymentRequest.getTotalAmount());
        transaction.setPaymentMethod(paymentRequest.getPaymentMethod());
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId("TXN_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        }

        // Offline / COD payment handling
        if (paymentRequest.getPaymentMethod() == PaymentMethod.OFFLINE) {
            transaction.setStatus(PaymentStatus.PENDING);
            transaction.setPaymentGatewayTxnId("COD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            Transaction saved = transactionRepository.save(transaction);
            log.info("Offline / COD payment initialized for Order ID: {}", paymentRequest.getOrderId());
            return transactionToPaymentResponse(saved);
        }

        // Online Payment via Razorpay
        String razorpayOrderId;
        long amountInPaise = paymentRequest.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        if (razorpayConfig.isMockMode()) {
            log.warn("[MOCK MODE] Razorpay mock credentials in use. Simulating order creation without calling Razorpay server. " +
                    "To enable live Razorpay API calls, provide valid keys in application.properties or environment variables.");
            razorpayOrderId = "order_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        } else {
            try {
                JSONObject orderRequest = new JSONObject();
                orderRequest.put("amount", amountInPaise);
                orderRequest.put("currency", razorpayConfig.getCurrency());
                orderRequest.put("receipt", "rcpt_" + paymentRequest.getOrderId() + "_" + (System.currentTimeMillis() % 100000));

                JSONObject notes = new JSONObject();
                notes.put("orderId", String.valueOf(paymentRequest.getOrderId()));
                notes.put("paymentMethod", paymentRequest.getPaymentMethod().name());
                orderRequest.put("notes", notes);

                log.info("Calling Razorpay orders.create with amount: {} paise, currency: {}", amountInPaise, razorpayConfig.getCurrency());
                Order razorpayOrder = razorpayClient.orders.create(orderRequest);
                razorpayOrderId = razorpayOrder.get("id");
                log.info("Razorpay order successfully created with Razorpay Order ID: {}", razorpayOrderId);
            } catch (RazorpayException e) {
                log.error("Failed to create order on Razorpay for Order ID {}: {}", paymentRequest.getOrderId(), e.getMessage(), e);
                transaction.setStatus(PaymentStatus.FAILED);
                transactionRepository.save(transaction);
                throw new BusinessException("Razorpay order creation failed: " + e.getMessage());
            }
        }

        transaction.setPaymentGatewayOrderId(razorpayOrderId);
        transaction.setStatus(PaymentStatus.PENDING);
        Transaction saved = transactionRepository.save(transaction);

        log.info("Transaction recorded in PENDING state. Order ID: {}, Razorpay Order ID: {}, Txn ID: {}",
                saved.getOrderId(), saved.getPaymentGatewayOrderId(), saved.getTransactionId());

        return transactionToPaymentResponse(saved);
    }

    /**
     * Verifies cryptographic signature received from Razorpay Checkout modal after payment.
     * On success: Updates status to PAID and emits Kafka PAYMENT_EVENT.
     * On failure: Updates status to FAILED, emits Kafka PAYMENT_EVENT, and throws BusinessException.
     */
    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest verificationRequest) {
        log.info("Verifying Razorpay payment signature for Razorpay Order ID: {}, Payment ID: {}",
                verificationRequest.getRazorpayOrderId(), verificationRequest.getRazorpayPaymentId());

        Transaction transaction = transactionRepository.findByPaymentGatewayOrderId(verificationRequest.getRazorpayOrderId())
                .orElseGet(() -> {
                    if (verificationRequest.getOrderId() != null) {
                        return transactionRepository.findByOrderId(verificationRequest.getOrderId())
                                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for Order ID: " + verificationRequest.getOrderId()));
                    }
                    throw new ResourceNotFoundException("Transaction not found for Razorpay Order ID: " + verificationRequest.getRazorpayOrderId());
                });

        // Idempotency check: If already paid, return directly
        if (transaction.getStatus() == PaymentStatus.PAID) {
            log.info("Payment for Order ID: {} is already verified and marked as PAID.", transaction.getOrderId());
            return transactionToPaymentResponse(transaction);
        }

        boolean isValid = false;
        if (razorpayConfig.isMockMode()) {
            log.warn("[MOCK MODE] Validating signature in mock mode.");
            String sig = verificationRequest.getRazorpaySignature();
            // In mock mode: reject if signature explicitly says "fail" or "invalid", otherwise approve
            isValid = !(sig != null && (sig.toLowerCase().contains("fail") || sig.toLowerCase().contains("invalid")));
        } else {
            try {
                JSONObject attributes = new JSONObject();
                attributes.put("razorpay_order_id", verificationRequest.getRazorpayOrderId());
                attributes.put("razorpay_payment_id", verificationRequest.getRazorpayPaymentId());
                attributes.put("razorpay_signature", verificationRequest.getRazorpaySignature());

                isValid = Utils.verifyPaymentSignature(attributes, razorpayConfig.getKeySecret());
            } catch (RazorpayException e) {
                log.error("Error during Razorpay signature verification: {}", e.getMessage(), e);
                isValid = false;
            }
        }

        if (isValid) {
            log.info("Razorpay signature verified successfully for Order ID: {}. Marking as PAID.", transaction.getOrderId());
            transaction.setStatus(PaymentStatus.PAID);
            transaction.setPaymentGatewayTxnId(verificationRequest.getRazorpayPaymentId());
            transaction.setPaymentGatewaySignature(verificationRequest.getRazorpaySignature());
            Transaction saved = transactionRepository.save(transaction);

            // Publish success event to Kafka so cart-order-service updates order status to PAID
            paymentEventProducer.publishPaymentEvent(saved, PaymentStatus.PAID);

            return transactionToPaymentResponse(saved);
        } else {
            log.warn("Razorpay signature verification FAILED for Order ID: {}. Marking as FAILED.", transaction.getOrderId());
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setPaymentGatewayTxnId(verificationRequest.getRazorpayPaymentId());
            transaction.setPaymentGatewaySignature(verificationRequest.getRazorpaySignature());
            Transaction saved = transactionRepository.save(transaction);

            // Publish failed event to Kafka so cart-order-service cancels order & releases inventory stock
            paymentEventProducer.publishPaymentEvent(saved, PaymentStatus.FAILED);

            throw new BusinessException("Payment verification failed: Invalid signature or payment declined");
        }
    }

    /**
     * Handles Razorpay webhook callbacks for asynchronous payment events.
     * (e.g. payment.captured, order.paid, payment.failed)
     */
    @Override
    public void handleWebhook(String payload, String signatureHeader) {
        log.info("Processing Razorpay webhook callback");

        if (!razorpayConfig.isMockMode()) {
            if (signatureHeader == null || signatureHeader.isBlank()) {
                throw new BusinessException("Missing X-Razorpay-Signature header in webhook request");
            }
            try {
                boolean isValid = Utils.verifyWebhookSignature(payload, signatureHeader, razorpayConfig.getWebhookSecret());
                if (!isValid) {
                    log.error("Invalid Razorpay webhook signature detected");
                    throw new BusinessException("Webhook signature verification failed");
                }
            } catch (RazorpayException e) {
                log.error("Error verifying webhook signature: {}", e.getMessage(), e);
                throw new BusinessException("Webhook signature verification failed: " + e.getMessage());
            }
        }

        try {
            JSONObject eventJson = new JSONObject(payload);
            String event = eventJson.optString("event");
            log.info("Razorpay webhook event received: {}", event);

            JSONObject payloadObj = eventJson.optJSONObject("payload");
            if (payloadObj == null) {
                log.warn("Webhook payload container object is missing");
                return;
            }

            JSONObject paymentObj = payloadObj.optJSONObject("payment");
            JSONObject paymentEntity = paymentObj != null ? paymentObj.optJSONObject("entity") : null;

            if (paymentEntity == null) {
                log.warn("Payment entity missing in webhook payload for event: {}", event);
                return;
            }

            String razorpayOrderId = paymentEntity.optString("order_id");
            String razorpayPaymentId = paymentEntity.optString("id");

            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                log.warn("Webhook event {} did not contain order_id", event);
                return;
            }

            Optional<Transaction> txnOpt = transactionRepository.findByPaymentGatewayOrderId(razorpayOrderId);
            if (txnOpt.isEmpty()) {
                log.warn("No transaction found in database for Razorpay Order ID: {}", razorpayOrderId);
                return;
            }

            Transaction transaction = txnOpt.get();

            if ("payment.captured".equalsIgnoreCase(event) || "order.paid".equalsIgnoreCase(event)) {
                if (transaction.getStatus() != PaymentStatus.PAID) {
                    log.info("Webhook event {} confirmed payment for Order ID: {}", event, transaction.getOrderId());
                    transaction.setStatus(PaymentStatus.PAID);
                    transaction.setPaymentGatewayTxnId(razorpayPaymentId);
                    Transaction saved = transactionRepository.save(transaction);
                    paymentEventProducer.publishPaymentEvent(saved, PaymentStatus.PAID);
                }
            } else if ("payment.failed".equalsIgnoreCase(event)) {
                if (transaction.getStatus() != PaymentStatus.PAID) {
                    log.warn("Webhook event payment.failed for Order ID: {}", transaction.getOrderId());
                    transaction.setStatus(PaymentStatus.FAILED);
                    transaction.setPaymentGatewayTxnId(razorpayPaymentId);
                    Transaction saved = transactionRepository.save(transaction);
                    paymentEventProducer.publishPaymentEvent(saved, PaymentStatus.FAILED);
                }
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Error processing webhook payload: {}", e.getMessage(), e);
            throw new BusinessException("Error processing webhook payload: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByOrderId(Long orderId) {
        Transaction transaction = transactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for Order ID: " + orderId));
        return transactionToPaymentResponse(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found for Transaction ID: " + transactionId));
        return transactionToPaymentResponse(transaction);
    }

    private PaymentResponse transactionToPaymentResponse(Transaction transaction) {
        return PaymentResponse.builder()
                .id(transaction.getId())
                .orderId(transaction.getOrderId())
                .amount(transaction.getAmount())
                .paymentGatewayTxnId(transaction.getPaymentGatewayTxnId())
                .paymentGatewayOrderId(transaction.getPaymentGatewayOrderId())
                .paymentGatewaySignature(transaction.getPaymentGatewaySignature())
                .transactionId(transaction.getTransactionId())
                .createdAt(transaction.getCreatedAt())
                .status(transaction.getStatus())
                .updatedAt(transaction.getUpdatedAt())
                .paymentMethod(transaction.getPaymentMethod())
                .currency(razorpayConfig.getCurrency())
                .razorpayKeyId(razorpayConfig.getKeyId())
                .build();
    }
}
