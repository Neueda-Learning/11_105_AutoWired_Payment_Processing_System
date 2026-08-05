package com.payment.server.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code PUT /api/users/{id}/pin} - change the payer's
 * PIN from the customer profile section. Requires the current PIN to be
 * supplied so someone with a stale session can't silently change it.
 */
public class UpdatePinRequest {

    @NotBlank(message = "currentPin is required")
    private String currentPin;

    @NotBlank(message = "newPin is required")
    @Pattern(regexp = "\\d{4,6}", message = "newPin must be 4-6 digits")
    private String newPin;

    public String getCurrentPin() {
        return currentPin;
    }

    public void setCurrentPin(String currentPin) {
        this.currentPin = currentPin;
    }

    public String getNewPin() {
        return newPin;
    }

    public void setNewPin(String newPin) {
        this.newPin = newPin;
    }
}
