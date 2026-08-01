package com.payment.server.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.payment.server.model.Customer;

@Repository
public class CustomerRepository {

    private final List<Customer> customers = new ArrayList<>();

    public CustomerRepository() {
        customers.add(new Customer(1, "Alice Johnson", "ACC1001", "US"));
        customers.add(new Customer(2, "Bob Smith", "ACC1002", "US"));
        customers.add(new Customer(3, "Charlie Brown", "ACC1003", "GB"));
        customers.add(new Customer(4, "Diana Prince", "ACC1004", "GB"));
        customers.add(new Customer(5, "Ethan Hunt", "ACC1005", "IN"));
    }

    public List<Customer> findAll() {
        return new ArrayList<>(customers);
    }

    public Customer findById(int id) {
        return customers.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Customer findByAccountNumber(String accountNumber) {
        return customers.stream()
                .filter(c -> c.getAccountNumber().equalsIgnoreCase(accountNumber))
                .findFirst()
                .orElse(null);
    }
}
