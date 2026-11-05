package com.mss301.watch.service.saga;

/**
 * Một bước của Saga gồm:
 * - action: local transaction chính
 * - compensation: transaction bù trừ nếu Saga thất bại
 *
 * Pivot là bước không có compensation.
 */
// [TODO-6.8: bước Saga = action + compensation; pivot]
public record SagaStep(
        String name,
        Runnable action,
        String compensationName,
        Runnable compensation
) {

    public static SagaStep of(
            String name,
            Runnable action,
            String compensationName,
            Runnable compensation
    ) {

        return new SagaStep(
                name,
                action,
                compensationName,
                compensation
        );
    }

    public static SagaStep pivot(
            String name,
            Runnable action
    ) {

        return new SagaStep(
                name,
                action,
                null,
                null
        );
    }

    public boolean isPivot() {
        return compensation == null;
    }
}