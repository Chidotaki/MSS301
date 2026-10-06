package com.fudn.customer_service.dto;

import com.fudn.customer_service.model.Customer;
import com.fudn.customer_service.model.CustomerStatus;

import java.time.LocalDate;

public record CustomerResponse(
        Long customerId,
        String customerName,
        String telephone,
        String email,
        LocalDate customerBirthday,
        CustomerStatus customerStatus
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getCustomerName(),
                customer.getTelephone(),
                customer.getEmail(),
                customer.getCustomerBirthday(),
                customer.getCustomerStatus()
        );
    }
}