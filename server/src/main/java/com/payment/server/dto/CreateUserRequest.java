package com.payment.server.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request body for {@code POST /api/users} - register a user
 * (name, email, phone, set PIN). See payment-system-v2-design.md section 8.
 */
public class CreateUserRequest {

    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    private String phone;

    // Plain PIN supplied at registration time; hashed immediately by the
    // service layer and never persisted or returned in raw form.
    @NotBlank(message = "pin is required")
    @Pattern(regexp = "\\d{4,6}", message = "pin must be 4-6 digits")
    private String pin;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }
}
