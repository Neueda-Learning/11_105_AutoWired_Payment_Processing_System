package com.payment.server.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payment.server.dto.FeeRuleRequest;
import com.payment.server.exception.FeeRuleNotFoundException;
import com.payment.server.model.TransactionFeeRule;
import com.payment.server.repository.TransactionFeeRuleRepository;

import jakarta.validation.Valid;

/**
 * Admin API for managing configurable transaction fee rules
 * (see new-docs/payment-system-v2-design.md, section 4 & 8).
 */
@RestController
@RequestMapping("/api/fee-rules")
public class FeeRuleController {

    private final TransactionFeeRuleRepository feeRuleRepository;

    public FeeRuleController(TransactionFeeRuleRepository feeRuleRepository) {
        this.feeRuleRepository = feeRuleRepository;
    }

    @GetMapping
    public ResponseEntity<List<TransactionFeeRule>> getAllRules() {
        return ResponseEntity.ok(feeRuleRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<TransactionFeeRule> createRule(@Valid @RequestBody FeeRuleRequest request) {
        TransactionFeeRule rule = new TransactionFeeRule();
        rule.setPaymentMethod(request.getPaymentMethod());
        rule.setMinAmount(request.getMinAmount());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setFeeType(request.getFeeType());
        rule.setFeeValue(request.getFeeValue());
        rule.setMinFeeCap(request.getMinFeeCap());
        rule.setMaxFeeCap(request.getMaxFeeCap());
        rule.setActive(request.isActive());

        feeRuleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(rule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionFeeRule> updateRule(@PathVariable int id,
            @Valid @RequestBody FeeRuleRequest request) {
        TransactionFeeRule rule = feeRuleRepository.findById(id);
        if (rule == null) {
            throw new FeeRuleNotFoundException(id);
        }

        rule.setPaymentMethod(request.getPaymentMethod());
        rule.setMinAmount(request.getMinAmount());
        rule.setMaxAmount(request.getMaxAmount());
        rule.setFeeType(request.getFeeType());
        rule.setFeeValue(request.getFeeValue());
        rule.setMinFeeCap(request.getMinFeeCap());
        rule.setMaxFeeCap(request.getMaxFeeCap());
        rule.setActive(request.isActive());

        feeRuleRepository.update(rule);
        return ResponseEntity.ok(rule);
    }
}
