package com.payment.server.service;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CurrencyConversionServiceTest {

    @Test
    void getRateToUsdReturnsKnownRate() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal usdRate = service.getRateToUsd("USD");
        assertEquals(new BigDecimal("1"), usdRate);

        BigDecimal inrRate = service.getRateToUsd("INR");
        assertEquals(new BigDecimal("95"), inrRate);
    }

    @Test
    void getRateToUsdReturnsCaseInsensitive() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal rate1 = service.getRateToUsd("usd");
        BigDecimal rate2 = service.getRateToUsd("USD");

        assertEquals(rate1, rate2);
    }

    @Test
    void getRateToUsdReturnsNullForUnknownCurrency() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal rate = service.getRateToUsd("XYZ");
        assertNull(rate);
    }

    @Test
    void getRateToUsdReturnsNullForNullCurrency() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal rate = service.getRateToUsd(null);
        assertNull(rate);
    }

    @Test
    void updateRateAddsOrUpdatesRate() {
        CurrencyConversionService service = new CurrencyConversionService();

        service.updateRate("JPY", new BigDecimal("150"));

        BigDecimal rate = service.getRateToUsd("JPY");
        assertEquals(new BigDecimal("150"), rate);
    }

    @Test
    void updateRateThrowsForInvalidRate() {
        CurrencyConversionService service = new CurrencyConversionService();

        assertThrows(IllegalArgumentException.class, () -> service.updateRate("JPY", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> service.updateRate("JPY", new BigDecimal("-1")));
        assertThrows(IllegalArgumentException.class, () -> service.updateRate(null, new BigDecimal("100")));
        assertThrows(IllegalArgumentException.class, () -> service.updateRate("JPY", null));
    }

    @Test
    void getAllRatesReturnsAllRates() {
        CurrencyConversionService service = new CurrencyConversionService();

        Map<String, BigDecimal> rates = service.getAllRates();

        assertTrue(rates.containsKey("USD"));
        assertTrue(rates.containsKey("INR"));
        assertTrue(rates.containsKey("EUR"));
        assertTrue(rates.containsKey("GBP"));
    }

    @Test
    void convertBetweenSameCurrencyReturnsOriginalAmount() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal amount = new BigDecimal("1000.00");
        BigDecimal converted = service.convert(amount, "USD", "USD");

        assertEquals(amount, converted);
    }

    @Test
    void convertFromUsdToInr() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal usdAmount = new BigDecimal("100.00");
        BigDecimal inrAmount = service.convert(usdAmount, "USD", "INR");

        // 100 USD * 95 = 9500 INR
        assertEquals(new BigDecimal("9500.00"), inrAmount);
    }

    @Test
    void convertFromInrToUsd() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal inrAmount = new BigDecimal("9500.00");
        BigDecimal usdAmount = service.convert(inrAmount, "INR", "USD");

        // 9500 INR / 95 = 100 USD
        assertEquals(new BigDecimal("100.00"), usdAmount);
    }

    @Test
    void convertBetweenNonUsdCurrencies() {
        CurrencyConversionService service = new CurrencyConversionService();

        // EUR to INR: 100 EUR -> USD -> INR
        // 100 EUR / 0.92 = 108.6956... USD
        // 108.6956... USD * 95 = 10326.09 INR (rounded)
        BigDecimal eurAmount = new BigDecimal("100.00");
        BigDecimal inrAmount = service.convert(eurAmount, "EUR", "INR");

        assertTrue(inrAmount.compareTo(new BigDecimal("10326.00")) >= 0);
        assertTrue(inrAmount.compareTo(new BigDecimal("10327.00")) <= 0);
    }

    @Test
    void convertReturnsOriginalAmountForUnknownCurrency() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal amount = new BigDecimal("1000.00");

        // Unknown 'from' currency
        BigDecimal result1 = service.convert(amount, "XYZ", "USD");
        assertEquals(amount, result1);

        // Unknown 'to' currency
        BigDecimal result2 = service.convert(amount, "USD", "XYZ");
        assertEquals(amount, result2);
    }

    @Test
    void convertHandlesNullAmount() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal result = service.convert(null, "USD", "INR");
        assertNull(result);
    }

    @Test
    void convertHandlesNullCurrencies() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal amount = new BigDecimal("1000.00");

        BigDecimal result1 = service.convert(amount, null, "USD");
        assertEquals(amount, result1);

        BigDecimal result2 = service.convert(amount, "USD", null);
        assertEquals(amount, result2);
    }

    @Test
    void toLedgerCurrencyConvertsToInr() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal usdAmount = new BigDecimal("100.00");
        BigDecimal inrAmount = service.toLedgerCurrency(usdAmount, "USD");

        assertEquals(new BigDecimal("9500.00"), inrAmount);
    }

    @Test
    void toLedgerCurrencyReturnsOriginalWhenAlreadyInr() {
        CurrencyConversionService service = new CurrencyConversionService();

        BigDecimal inrAmount = new BigDecimal("1000.00");
        BigDecimal result = service.toLedgerCurrency(inrAmount, "INR");

        assertEquals(inrAmount, result);
    }

    @Test
    void convertRoundsToTwoDecimalPlaces() {
        CurrencyConversionService service = new CurrencyConversionService();

        // 1 USD to EUR: 1 / 1 * 0.92 = 0.92
        BigDecimal eurAmount = service.convert(new BigDecimal("1.00"), "USD", "EUR");
        assertEquals(2, eurAmount.scale());
    }
}
