package com.example.test.service;

import com.example.test.entity.Customer;
import com.example.test.exception.NotFoundException;
import com.example.test.request.CustomerRequest;

public interface CustomerService {

    Customer getCustomerById(Long id) throws Exception;

    Customer createCustomer(CustomerRequest request) throws Exception;
    void deleteCustomer(long id) throws NotFoundException;
}
