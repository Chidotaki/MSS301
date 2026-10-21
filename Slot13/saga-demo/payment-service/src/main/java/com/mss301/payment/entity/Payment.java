package com.mss301.payment.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    private Long orderId;

    private String customerId;

    private double amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
}