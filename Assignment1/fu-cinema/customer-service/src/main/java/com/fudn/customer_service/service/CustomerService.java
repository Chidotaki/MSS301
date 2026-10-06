package com.fudn.customer_service.service;

import com.fudn.customer_service.dto.*;
import com.fudn.customer_service.exception.ApiException;
import com.fudn.customer_service.model.Customer;
import com.fudn.customer_service.model.CustomerStatus;
import com.fudn.customer_service.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    // ===================== CUSTOMER =====================

    @Transactional
    public CustomerResponse register(RegisterRequest request) {
        ensureEmailAvailable(request.email(), null);

        Customer customer = new Customer();
        customer.setCustomerName(request.customerName());
        customer.setTelephone(request.telephone());
        customer.setEmail(request.email());
        customer.setCustomerBirthday(request.customerBirthday());
        customer.setCustomerStatus(CustomerStatus.ACTIVE);
        customer.setPassword(
                passwordEncoder.encode(request.password())
        );

        Customer saved = customerRepository.save(customer);

        log.info(
                "Customer registered: id={}, email={}",
                saved.getCustomerId(),
                saved.getEmail()
        );

        return CustomerResponse.from(saved);
    }

    public CustomerResponse getProfile(Long customerId) {
        return CustomerResponse.from(
                findCustomer(customerId)
        );
    }

    @Transactional
    public CustomerResponse updateProfile(
            Long customerId,
            ProfileUpdateRequest request
    ) {
        Customer customer = findCustomer(customerId);

        customer.setCustomerName(
                request.customerName()
        );

        customer.setTelephone(
                request.telephone()
        );

        customer.setCustomerBirthday(
                request.customerBirthday()
        );

        return CustomerResponse.from(
                customerRepository.save(customer)
        );
    }

    @Transactional
    public void changePassword(
            Long customerId,
            ChangePasswordRequest request
    ) {
        Customer customer = findCustomer(customerId);

        if (!passwordEncoder.matches(
                request.oldPassword(),
                customer.getPassword()
        )) {
            throw ApiException.badRequest(
                    "Old password is incorrect"
            );
        }

        if (request.oldPassword()
                .equals(request.newPassword())) {

            throw ApiException.badRequest(
                    "New password must be different from the old password"
            );
        }

        customer.setPassword(
                passwordEncoder.encode(
                        request.newPassword()
                )
        );

        customerRepository.save(customer);
    }

    // ===================== ADMIN =====================

    public List<CustomerResponse> search(
            String keyword
    ) {
        List<Customer> customers;

        if (keyword == null || keyword.isBlank()) {

            customers = customerRepository.findAll(
                    Sort.by("customerId")
            );

        } else {

            String value = keyword.trim();

            customers =
                    customerRepository
                            .findByCustomerNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByCustomerIdAsc(
                                    value,
                                    value
                            );
        }

        return customers.stream()
                .map(CustomerResponse::from)
                .toList();
    }

    public CustomerResponse getById(Long id) {
        return CustomerResponse.from(
                findCustomer(id)
        );
    }

    @Transactional
    public CustomerResponse create(
            AdminCustomerRequest request
    ) {

        if (request.password() == null
                || request.password().isBlank()) {

            throw ApiException.badRequest(
                    "password: Password is required when creating a customer"
            );
        }

        ensureEmailAvailable(
                request.email(),
                null
        );

        Customer customer = new Customer();

        applyAdminRequest(
                customer,
                request
        );

        customer.setPassword(
                passwordEncoder.encode(
                        request.password()
                )
        );

        return CustomerResponse.from(
                customerRepository.save(customer)
        );
    }

    @Transactional
    public CustomerResponse update(
            Long id,
            AdminCustomerRequest request
    ) {

        Customer customer =
                findCustomer(id);

        ensureEmailAvailable(
                request.email(),
                id
        );

        applyAdminRequest(
                customer,
                request
        );

        if (request.password() != null
                && !request.password().isBlank()) {

            customer.setPassword(
                    passwordEncoder.encode(
                            request.password()
                    )
            );
        }

        return CustomerResponse.from(
                customerRepository.save(customer)
        );
    }

    @Transactional
    public void delete(Long id) {

        Customer customer =
                findCustomer(id);

        customer.setCustomerStatus(
                CustomerStatus.INACTIVE
        );

        customerRepository.save(customer);
    }

    // ===================== HELPER =====================

    private Customer findCustomer(Long id) {
        return customerRepository
                .findById(id)
                .orElseThrow(() ->
                        ApiException.notFound(
                                "Customer not found with id: " + id
                        )
                );
    }

    private void ensureEmailAvailable(
            String email,
            Long excludeId
    ) {

        boolean exists;

        if (excludeId == null) {

            exists =
                    customerRepository
                            .existsByEmailIgnoreCase(
                                    email
                            );

        } else {

            exists =
                    customerRepository
                            .existsByEmailIgnoreCaseAndCustomerIdNot(
                                    email,
                                    excludeId
                            );
        }

        if (exists
                || adminEmail.equalsIgnoreCase(email)) {

            throw ApiException.conflict(
                    "Email is already in use: " + email
            );
        }
    }

    private void applyAdminRequest(
            Customer customer,
            AdminCustomerRequest request
    ) {

        customer.setCustomerName(
                request.customerName()
        );

        customer.setTelephone(
                request.telephone()
        );

        customer.setEmail(
                request.email()
        );

        customer.setCustomerBirthday(
                request.customerBirthday()
        );

        customer.setCustomerStatus(
                request.customerStatus()
        );
    }
}