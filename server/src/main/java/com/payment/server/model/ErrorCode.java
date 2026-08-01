package com.payment.server.model;

public class ErrorCode {

    private String code;
    private String description;
    private int httpStatus;
    private String severity;

    public ErrorCode() {
    }

    public ErrorCode(String code, String description, int httpStatus, String severity) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
        this.severity = severity;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
