package com.payment.server.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.payment.server.exception.InvalidStatusTransitionException;
import com.payment.server.exception.PaymentNotFoundException;
import com.payment.server.exception.PaymentValidationException;
import com.payment.server.model.Payment;
import com.payment.server.model.PaymentStatusHistory;
import com.payment.server.repository.PaymentRepository;
import com.payment.server.repository.PaymentStatusHistoryRepository;

@Service
public class PaymentService {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private static final Map<String, Set<String>> VALID_TRANSITIONS = Map.of(
            STATUS_CREATED, Set.of(STATUS_VALIDATED, STATUS_FAILED),
            STATUS_VALIDATED, Set.of(STATUS_SENT, STATUS_FAILED),
            STATUS_SENT, Set.of(STATUS_COMPLETED, STATUS_FAILED),
            STATUS_COMPLETED, Set.of(),
            STATUS_FAILED, Set.of());

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository historyRepository;
    private final PaymentValidationService validationService;
    private final RiskScoringService riskScoringService;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository historyRepository,
            PaymentValidationService validationService,
            RiskScoringService riskScoringService) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
        this.riskScoringService = riskScoringService;
    }

    public List<Payment> getAllPayments(String status) {
        if (status == null || status.isBlank()) {
            return paymentRepository.findAll();
        }
        return paymentRepository.findByStatus(status);
    }

    public Payment getPaymentById(int id) {
        Payment payment = paymentRepository.findById(id);
        if (payment == null) {
            throw new PaymentNotFoundException(id);
        }
        return payment;
    }

    public List<PaymentStatusHistory> getHistory(int id) {
        // ensures payment exists, throws if not found
        getPaymentById(id);
        return historyRepository.findByPaymentId(id);
    }

    public Payment createPayment(Payment payment) {
        LocalDateTime now = LocalDateTime.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setStatus(STATUS_CREATED);

        int id = paymentRepository.save(payment);
        historyRepository.save(id, STATUS_CREATED, null, "Payment created" + paymentMethodDetail(payment));

        // Run validation
        PaymentValidationService.ValidationResult result = validationService.validate(payment);

        // Raw card details are only needed for validation - never persist or
        // return them beyond this point (see Payment model for masked fields).
        payment.setCardNumber(null);
        payment.setCardHolderName(null);

        if (!result.isValid()) {
            payment.setStatus(STATUS_FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.updateStatus(id, STATUS_FAILED);
            historyRepository.save(id, STATUS_FAILED, STATUS_CREATED,
                    "Validation failed: " + String.join(", ", result.getErrors()));
            throw new PaymentValidationException(result.getErrors());
        }

        // Validation passed
        payment.setStatus(STATUS_VALIDATED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.updateStatus(id, STATUS_VALIDATED);
        historyRepository.save(id, STATUS_VALIDATED, STATUS_CREATED, "Payment passed validation");

        // Risk scoring
        int riskScore = riskScoringService.scorePayment(payment);
        payment.setRiskScore(riskScore);
        paymentRepository.updateRiskScore(id, riskScore);

        return payment;
    }

    private String paymentMethodDetail(Payment payment) {
        if (payment.getPaymentMethod() == null) {
            return "";
        }
        return switch (payment.getPaymentMethod()) {
            case "UPI" -> payment.getUpiId() != null ? " (UPI ID: " + payment.getUpiId() + ")" : "";
            case "NETBANKING" -> payment.getBankName() != null ? " (Bank: " + payment.getBankName() + ")" : "";
            case "CREDIT_CARD" ->
                payment.getCardLast4() != null ? " (Card ending in " + payment.getCardLast4() + ")" : "";
            default -> "";
        };
    }

    public Payment transitionStatus(int id, String newStatus, String notes) {
        Payment payment = getPaymentById(id);
        String currentStatus = payment.getStatus();

        boolean allowed = STATUS_FAILED.equals(newStatus)
                || VALID_TRANSITIONS.getOrDefault(currentStatus, Set.of()).contains(newStatus);

        if (!allowed) {
            throw new InvalidStatusTransitionException(currentStatus, newStatus);
        }

        payment.setStatus(newStatus);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.updateStatus(id, newStatus);
        historyRepository.save(id, newStatus, currentStatus, notes);

        return payment;
    }
}
