package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payment.server.exception.DuplicatePaymentException;
import com.payment.server.exception.InvalidStatusTransitionException;
import com.payment.server.exception.PaymentValidationException;
import com.payment.server.model.Payment;
import com.payment.server.repository.PaymentRepository;
import com.payment.server.repository.PaymentStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusHistoryRepository historyRepository;

    @Mock
    private PaymentValidationService validationService;

    @Mock
    private RiskScoringService riskScoringService;

    @Test
    void createPaymentWhenValidationPassesPersistsValidatedPaymentWithRiskScore() {
        PaymentService service = new PaymentService(paymentRepository, historyRepository, validationService, riskScoringService);
        Payment payment = buildPayment("SRC-100", "DST-200", "USD", "1000.00");

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(101);
            return 101;
        });
        when(validationService.validate(any(Payment.class)))
                .thenReturn(new PaymentValidationService.ValidationResult(true, List.of()));
        when(riskScoringService.scorePayment(any(Payment.class), anyInt())).thenReturn(65);

        Payment created = service.createPayment(payment);

        assertEquals(101, created.getId());
        assertEquals(PaymentService.STATUS_VALIDATED, created.getStatus());
        assertEquals(65, created.getRiskScore());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        verify(paymentRepository).updateStatus(101, PaymentService.STATUS_VALIDATED);
        verify(paymentRepository).updateRiskScore(101, 65);

        InOrder order = inOrder(historyRepository);
        order.verify(historyRepository).save(101, PaymentService.STATUS_CREATED, null, "Payment created");
        order.verify(historyRepository).save(101, PaymentService.STATUS_VALIDATED, PaymentService.STATUS_CREATED,
                "Payment passed validation");
    }

    @Test
    void createPaymentWhenValidationFailsMarksFailedAndThrows() {
        PaymentService service = new PaymentService(paymentRepository, historyRepository, validationService, riskScoringService);
        Payment payment = buildPayment("SRC-100", "DST-200", "USD", "1000.00");

        when(paymentRepository.save(any(Payment.class))).thenReturn(202);
        when(validationService.validate(any(Payment.class))).thenReturn(
                new PaymentValidationService.ValidationResult(false, List.of("Currency is not supported")));

        PaymentValidationException ex = assertThrows(PaymentValidationException.class, () -> service.createPayment(payment));

        assertTrue(ex.getErrors().contains("Currency is not supported"));
        verify(paymentRepository).updateStatus(202, PaymentService.STATUS_FAILED);
        verify(historyRepository).save(202, PaymentService.STATUS_FAILED, PaymentService.STATUS_CREATED,
                "Validation failed: Currency is not supported");
        verify(riskScoringService, never()).scorePayment(any(Payment.class), anyInt());
        verify(paymentRepository, never()).updateRiskScore(eq(202), any(Integer.class));
    }

    @Test
    void createPaymentWhenIdempotencyKeyAlreadyExistsThrowsDuplicatePaymentException() {
        PaymentService service = new PaymentService(paymentRepository, historyRepository, validationService, riskScoringService);
        Payment payment = buildPayment("SRC-100", "DST-200", "USD", "1000.00");
        payment.setIdempotencyKey("idem-key-1");

        Payment existing = new Payment();
        existing.setId(345);
        when(paymentRepository.findByIdempotencyKey("idem-key-1")).thenReturn(existing);

        DuplicatePaymentException ex = assertThrows(DuplicatePaymentException.class,
                () -> service.createPayment(payment));

        assertEquals(345, ex.getExistingPaymentId());
        verify(paymentRepository, never()).save(any(Payment.class));
        verify(historyRepository, never()).save(anyInt(), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void transitionStatusAllowsValidTransitionAndStoresHistory() {
        PaymentService service = new PaymentService(paymentRepository, historyRepository, validationService, riskScoringService);
        Payment existing = new Payment();
        existing.setId(7);
        existing.setStatus(PaymentService.STATUS_CREATED);

        when(paymentRepository.findById(7)).thenReturn(existing);

        Payment updated = service.transitionStatus(7, PaymentService.STATUS_VALIDATED, "ok");

        assertEquals(PaymentService.STATUS_VALIDATED, updated.getStatus());
        verify(paymentRepository).updateStatus(7, PaymentService.STATUS_VALIDATED);
        verify(historyRepository).save(7, PaymentService.STATUS_VALIDATED, PaymentService.STATUS_CREATED, "ok");
    }

    @Test
    void transitionStatusRejectsInvalidTransition() {
        PaymentService service = new PaymentService(paymentRepository, historyRepository, validationService, riskScoringService);
        Payment existing = new Payment();
        existing.setId(8);
        existing.setStatus(PaymentService.STATUS_CREATED);

        when(paymentRepository.findById(8)).thenReturn(existing);

        assertThrows(InvalidStatusTransitionException.class,
                () -> service.transitionStatus(8, PaymentService.STATUS_COMPLETED, "skip"));

        verify(paymentRepository, never()).updateStatus(any(Integer.class), any(String.class));
        verify(historyRepository, never()).save(any(Integer.class), any(String.class), any(String.class), any(String.class));
    }

    private Payment buildPayment(String source, String destination, String currency, String amount) {
        Payment payment = new Payment();
        payment.setSourceAccount(source);
        payment.setDestinationAccount(destination);
        payment.setCurrency(currency);
        payment.setAmount(new BigDecimal(amount));
        payment.setPaymentMethod("UPI");
        payment.setReference("ref-1");
        return payment;
    }
}
