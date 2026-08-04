package com.payment.server.model;

public class Customer {

    private int id;
    private String name;
    private String accountNumber;
    private String country;
    // USER or ADMIN. Defaults to USER for backward-compatible constructors.
    private String role;
    // The user's own UPI address, used as the implicit sender for UPI payments.
    private String ownUpiId;
    // The user's own bank name, used as the implicit sender for net banking payments.
    private String ownBankName;

    public Customer() {
    }

    public Customer(String name, String accountNumber, String country) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.country = country;
        this.role = "USER";
    }

    public Customer(int id, String name, String accountNumber, String country) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.country = country;
        this.role = "USER";
    }

    public Customer(String name, String accountNumber, String country, String role, String ownUpiId,
            String ownBankName) {
        this.name = name;
        this.accountNumber = accountNumber;
        this.country = country;
        this.role = role;
        this.ownUpiId = ownUpiId;
        this.ownBankName = ownBankName;
    }

    public Customer(int id, String name, String accountNumber, String country, String role, String ownUpiId,
            String ownBankName) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.country = country;
        this.role = role;
        this.ownUpiId = ownUpiId;
        this.ownBankName = ownBankName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getOwnUpiId() {
        return ownUpiId;
    }

    public void setOwnUpiId(String ownUpiId) {
        this.ownUpiId = ownUpiId;
    }

    public String getOwnBankName() {
        return ownBankName;
    }

    public void setOwnBankName(String ownBankName) {
        this.ownBankName = ownBankName;
    }
}
