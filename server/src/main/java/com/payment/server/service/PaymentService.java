package com.payment.server.service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payment.server.dto.PaymentStats;
import com.payment.server.exception.DuplicatePaymentException;
import com.payment.server.exception.InvalidStatusTransitionException;
import com.payment.server.exception.PaymentNotFoundException;
import com.payment.server.exception.PaymentValidationException;
import com.payment.server.model.BankAccount;
import com.payment.server.model.Payment;
import com.payment.server.model.PaymentStatusHistory;
import com.payment.server.repository.BankAccountRepository;
import com.payment.server.repository.PaymentRepository;
import com.payment.server.repository.PaymentStatusHistoryRepository;

@Service
public class PaymentService {

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_VALIDATED = "VALIDATED";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    // Payments scoring at or above this risk score are held in SENT for a
    // bank admin to manually complete/fail, instead of auto-completing.
    private static final int HIGH_RISK_REVIEW_THRESHOLD = 70;

    /**
     * Exposes the high-risk review threshold to other layers (e.g. the
     * flagged-transactions API) without duplicating the magic number.
     */
    public static int getHighRiskReviewThreshold() {
        return HIGH_RISK_REVIEW_THRESHOLD;
    }

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
    private final FeeCalculationService feeCalculationService;
    private final BankAccountRepository bankAccountRepository;
    private final CurrencyConversionService currencyConversionService;

    public PaymentService(PaymentRepository paymentRepository,
            PaymentStatusHistoryRepository historyRepository,
            PaymentValidationService validationService,
            RiskScoringService riskScoringService,
            FeeCalculationService feeCalculationService,
            BankAccountRepository bankAccountRepository,
            CurrencyConversionService currencyConversionService) {
        this.paymentRepository = paymentRepository;
        this.historyRepository = historyRepository;
        this.validationService = validationService;
        this.riskScoringService = riskScoringService;
        this.feeCalculationService = feeCalculationService;
        this.bankAccountRepository = bankAccountRepository;
        this.currencyConversionService = currencyConversionService;
    }

    public List<Payment> getAllPayments(String status) {
        if (status == null || status.isBlank()) {
            return paymentRepository.findAll();
        }
        return paymentRepository.findByStatus(status);
    }

    /**
     * Fraud/risk monitoring feed for bank admins - every payment whose risk
     * score met or exceeded the high-risk review threshold, regardless of
     * whether it's still awaiting review (SENT), was manually approved
     * (COMPLETED), or was manually rejected (FAILED). Sorted highest risk
     * first, then most recent. See payment-system-v2-design.md section 9.
     */
    public List<Payment> getFlaggedPayments() {
        return paymentRepository.findAll().stream()
                .filter(p -> p.getRiskScore() >= HIGH_RISK_REVIEW_THRESHOLD)
                .sorted(java.util.Comparator
                        .comparingInt(Payment::getRiskScore).reversed()
                        .thenComparing(java.util.Comparator.comparing(Payment::getCreatedAt).reversed()))
                .toList();
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

    @Transactional
    public Payment createPayment(Payment payment) {
        checkIdempotency(payment);

        LocalDateTime now = LocalDateTime.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setStatus(STATUS_CREATED);

        int id = paymentRepository.save(payment);
        historyRepository.save(id, STATUS_CREATED, null, "Payment created" + paymentMethodDetail(payment));

        processValidationRiskAndFee(payment, id);

        return payment;
    }

    /**
     * Creates a payment in CREATED status with authenticationStatus PENDING,
     * but does NOT run validation/risk/fee yet - those only run once the
     * payer completes the PIN/OTP auth gate. See AuthenticationService and
     * payment-system-v2-design.md section 5.
     */
    @Transactional
    public Payment createPendingPayment(Payment payment) {
        checkIdempotency(payment);

        LocalDateTime now = LocalDateTime.now();
        payment.setCreatedAt(now);
        payment.setUpdatedAt(now);
        payment.setStatus(STATUS_CREATED);
        payment.setAuthenticationStatus("PENDING");
        if (payment.getGrossAmount() == null) {
            payment.setGrossAmount(payment.getAmount());
        }

        int id = paymentRepository.save(payment);
        historyRepository.save(id, STATUS_CREATED, null,
                "Payment initiated, awaiting authentication" + paymentMethodDetail(payment));

        return payment;
    }

    /**
     * Called by AuthenticationService once the payer's PIN/OTP challenge is
     * verified - marks authenticationStatus VERIFIED and continues into the
     * existing validate -> risk score -> fee pipeline.
     */
    @Transactional
    public Payment completeAuthenticatedPayment(int id) {
        Payment payment = getPaymentById(id);
        payment.setAuthenticationStatus("VERIFIED");
        paymentRepository.updateAuthenticationStatus(id, "VERIFIED");
        historyRepository.save(id, STATUS_CREATED, STATUS_CREATED, "Payment authentication verified");

        processValidationRiskAndFee(payment, id);
        return payment;
    }

    /**
     * Called by AuthenticationService when PIN/OTP verification ultimately
     * fails (e.g. max attempts exceeded or OTP expired) - marks the payment
     * FAILED with an AUTH_FAILED-style audit trail.
     */
    @Transactional
    public Payment failAuthentication(int id, String reason) {
        Payment payment = getPaymentById(id);
        payment.setAuthenticationStatus("FAILED");
        paymentRepository.updateAuthenticationStatus(id, "FAILED");
        payment.setStatus(STATUS_FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.updateStatus(id, STATUS_FAILED);
        historyRepository.save(id, STATUS_FAILED, STATUS_CREATED, "Authentication failed: " + reason);
        return payment;
    }

    private void checkIdempotency(Payment payment) {
        if (payment.getIdempotencyKey() != null && !payment.getIdempotencyKey().isBlank()) {
            String idempotencyKey = payment.getIdempotencyKey().trim();
            payment.setIdempotencyKey(idempotencyKey);
            Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                throw new DuplicatePaymentException(existing.getId());
            }
        }
    }

    /**
     * Shared tail of the payment lifecycle: validation, risk scoring, and
     * dynamic fee calculation. Used both by the legacy synchronous
     * createPayment flow and by the new auth-gated flow (after PIN/OTP
     * verification succeeds).
     */
    private void processValidationRiskAndFee(Payment payment, int id) {
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
        int riskScore = riskScoringService.scorePayment(payment, id);
        payment.setRiskScore(riskScore);
        paymentRepository.updateRiskScore(id, riskScore);

        // Dynamic fee calculation (bank's revenue per transaction) - see
        // FeeCalculationService / TransactionFeeRule.
        FeeCalculationService.FeeResult feeResult = feeCalculationService.calculateFee(
                payment.getPaymentMethod(), payment.getAmount());
        payment.setFeeAmount(feeResult.getFeeAmount());
        payment.setFeePercentage(feeResult.getFeePercentage());
        payment.setNetAmount(feeResult.getNetAmount());
        paymentRepository.updateFee(id, feeResult.getFeeAmount(), feeResult.getFeePercentage(),
                feeResult.getNetAmount());
        historyRepository.save(id, STATUS_VALIDATED, STATUS_VALIDATED,
                "Fee calculated: " + feeResult.getFeeAmount() + " (" + feeResult.getFeePercentage()
                        + "%), net amount: " + feeResult.getNetAmount());

        // Simulated processing/transmission - see payment-system-v2-design.md
        // section 2 ("no real payment networks are integrated, simulate the
        // processing internally") and step 7-8 of the payer→payee walkthrough.
        // A payment only completes automatically when its risk score is below
        // the manual-review threshold; high-risk payments are left in SENT
        // for a human (bank admin) to complete or fail via transitionStatus.
        payment.setStatus(STATUS_SENT);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.updateStatus(id, STATUS_SENT);
        historyRepository.save(id, STATUS_SENT, STATUS_VALIDATED,
                "Payment transmitted to destination (simulated)");

        if (riskScore < HIGH_RISK_REVIEW_THRESHOLD) {
            payment.setStatus(STATUS_COMPLETED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.updateStatus(id, STATUS_COMPLETED);
            applyBalanceTransfer(payment);
            historyRepository.save(id, STATUS_COMPLETED, STATUS_SENT,
                    "Payment confirmed and completed (simulated settlement)");
        } else {
            historyRepository.save(id, STATUS_SENT, STATUS_SENT,
                    "Held for manual review: risk score " + riskScore + " exceeds automatic-completion threshold");
        }
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
        if (STATUS_COMPLETED.equals(newStatus)) {
            applyBalanceTransfer(payment);
        }
        historyRepository.save(id, newStatus, currentStatus, notes);

        return payment;
    }

    /**
     * Debits the source bank account and credits the destination bank
     * account once a payment reaches COMPLETED. Bank ledgers
     * (BankAccount.balance) are always tracked in INR, so amounts in the
     * payment's own currency are first dynamically converted to INR (via
     * CurrencyConversionService, USD-based cross rates) before adjusting
     * balances. The destination receives the net amount (amount minus the
     * bank's fee); the source pays the full gross amount. Accounts that
     * don't resolve to a v2 BankAccount (e.g. legacy Customer-only
     * accounts) are silently skipped since they have no tracked balance.
     */
    private void applyBalanceTransfer(Payment payment) {
        if (payment.getAmount() == null) {
            return;
        }

        String currency = payment.getCurrency() != null ? payment.getCurrency() : "INR";

        BankAccount source = bankAccountRepository.findByAccountNumber(payment.getSourceAccount());
        if (source != null) {
            BigDecimal debitInLedgerCurrency = currencyConversionService.toLedgerCurrency(payment.getAmount(),
                    currency);
            BigDecimal newSourceBalance = source.getBalance().subtract(debitInLedgerCurrency);
            bankAccountRepository.updateBalance(source.getId(), newSourceBalance);
        }

        BankAccount destination = bankAccountRepository.findByAccountNumber(payment.getDestinationAccount());
        if (destination != null) {
            BigDecimal creditAmount = payment.getNetAmount() != null
                    ? payment.getNetAmount()
                    : payment.getAmount();
            BigDecimal creditInLedgerCurrency = currencyConversionService.toLedgerCurrency(creditAmount, currency);
            BigDecimal newDestinationBalance = destination.getBalance().add(creditInLedgerCurrency);
            bankAccountRepository.updateBalance(destination.getId(), newDestinationBalance);
        }
    }

    public PaymentStats getStats() {
        List<Payment> payments = paymentRepository.findAll();

        long totalCount = payments.size();
        BigDecimal totalVolume = payments.stream()
                .map(Payment::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long completed = payments.stream().filter(p -> STATUS_COMPLETED.equals(p.getStatus())).count();
        long failed = payments.stream().filter(p -> STATUS_FAILED.equals(p.getStatus())).count();
        long finished = completed + failed;
        double successRate = finished > 0 ? (completed * 100.0) / finished : 0.0;

        double avgRiskScore = totalCount > 0
                ? payments.stream().mapToInt(Payment::getRiskScore).average().orElse(0.0)
                : 0.0;

        Map<String, Long> statusCounts = new java.util.HashMap<>();
        for (Payment p : payments) {
            statusCounts.merge(p.getStatus(), 1L, Long::sum);
        }

        BigDecimal totalFeesCollected = payments.stream()
                .filter(p -> STATUS_COMPLETED.equals(p.getStatus()))
                .map(Payment::getFeeAmount)
                .filter(f -> f != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentStats(totalCount, totalVolume, successRate, avgRiskScore, statusCounts,
                totalFeesCollected);
    }
}
