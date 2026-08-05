package com.payment.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Dynamic currency conversion service. USD is the base currency (rate =
 * 1.00); every other currency's rate is expressed as "1 USD = X of that
 * currency". Bank ledgers (BankAccount.balance) are always tracked in INR,
 * so payments made in a different currency are converted to INR before the
 * source/destination balances are debited/credited.
 *
 * Rates are held in-memory and can be updated at runtime (e.g. by a bank
 * admin endpoint) to simulate a live FX feed - see updateRate(). In a real
 * system this would instead poll an external FX-rate provider on a
 * schedule.
 */
@Service
public class CurrencyConversionService {

    public static final String BASE_CURRENCY = "USD";
    public static final String LEDGER_CURRENCY = "INR";

    // "1 USD = X <currency>" - illustrative approximate rates for demo
    // purposes. INR defaults to 95 per the platform's base assumption.
    private final Map<String, BigDecimal> ratesToUsd = new ConcurrentHashMap<>(Map.of(
            "USD", new BigDecimal("1"),
            "INR", new BigDecimal("95"),
            "EUR", new BigDecimal("0.92"),
            "GBP", new BigDecimal("0.79")));

    /**
     * Returns "1 USD = X <currency>" for the given currency, or null if unknown.
     */
    public BigDecimal getRateToUsd(String currency) {
        if (currency == null) {
            return null;
        }
        return ratesToUsd.get(currency.toUpperCase());
    }

    /** Updates (or adds) the "1 USD = X <currency>" rate at runtime. */
    public void updateRate(String currency, BigDecimal rateToUsd) {
        if (currency == null || rateToUsd == null || rateToUsd.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Invalid currency rate");
        }
        ratesToUsd.put(currency.toUpperCase(), rateToUsd);
    }

    public Map<String, BigDecimal> getAllRates() {
        return Map.copyOf(ratesToUsd);
    }

    /**
     * Converts an amount from one currency to another, going through USD as
     * the common base: amount -> USD -> target currency.
     */
    public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null) {
            return null;
        }
        if (fromCurrency == null || toCurrency == null) {
            return amount;
        }
        String from = fromCurrency.toUpperCase();
        String to = toCurrency.toUpperCase();
        if (from.equals(to)) {
            return amount;
        }

        BigDecimal fromRate = ratesToUsd.get(from);
        BigDecimal toRate = ratesToUsd.get(to);
        if (fromRate == null || toRate == null) {
            // Unknown currency - fall back to a no-op conversion rather than
            // failing the payment outright.
            return amount;
        }

        // amount (from) -> USD
        BigDecimal amountInUsd = amount.divide(fromRate, 10, RoundingMode.HALF_UP);
        // USD -> to
        return amountInUsd.multiply(toRate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Converts an amount in the given currency into the bank's ledger currency
     * (INR).
     */
    public BigDecimal toLedgerCurrency(BigDecimal amount, String currency) {
        return convert(amount, currency, LEDGER_CURRENCY);
    }
}
