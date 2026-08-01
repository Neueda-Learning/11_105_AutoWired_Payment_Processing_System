package com.payment.server.model;

import java.time.LocalDateTime;

public class PaymentStatusHistory {

    private int id;
    private int paymentId;
    private String status;
    private String previousStatus;
    private LocalDateTime timestamp;
    private String notes;

    public PaymentStatusHistory() {
    }

    public PaymentStatusHistory(int id, int paymentId, String status, String previousStatus,
            LocalDateTime timestamp, String notes) {
        this.id = id;
        this.paymentId = paymentId;
        this.status = status;
        this.previousStatus = previousStatus;
        this.timestamp = timestamp;
        this.notes = notes;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(int paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
