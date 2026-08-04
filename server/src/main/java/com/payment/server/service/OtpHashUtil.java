package com.payment.server.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Minimal one-way hashing utility for PINs and OTP codes - these must never
 * be stored in plain form (see payment-system-v2-design.md section 10).
 * Uses SHA-256 since no dedicated password-hashing dependency (e.g. Spring
 * Security Crypto / BCrypt) is present in this project's pom.xml.
 */
public final class OtpHashUtil {

    private OtpHashUtil() {
    }

    public static String hash(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static boolean matches(String raw, String hash) {
        if (raw == null || hash == null) {
            return false;
        }
        return hash.equals(hash(raw));
    }
}
