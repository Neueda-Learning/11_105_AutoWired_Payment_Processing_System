package com.payment.server.model;

public class Customer {

    private int id;
    private String name;
    private String accountNumber;
    private String country;

    public Customer() {
    }

    public Customer(int id, String name, String accountNumber, String country) {
        this.id = id;
        this.name = name;
        this.accountNumber = accountNumber;
        this.country = country;
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
}
