package com.payment.server.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/payments/{id}/authenticate} - submit
 * the PIN or OTP code to verify the payer before validation runs.
 */
public class AuthenticatePaymentRequest {

    // Either pin or otp is required depending on the challenge's method,
    // enforced in the service layer rather than bean validation here.
    private String pin;
    private String otp;

    @NotBlank(message = "method is required")
    private String method; // PIN / OTP

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }
}
