package com.mss301.order.entity;

public enum SagaStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED,
    COMPENSATING,
    COMPENSATED
}