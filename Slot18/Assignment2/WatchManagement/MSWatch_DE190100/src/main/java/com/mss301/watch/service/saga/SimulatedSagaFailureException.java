package com.mss301.watch.service.saga;

// [TODO-6.12]
public class SimulatedSagaFailureException
        extends RuntimeException {

    public SimulatedSagaFailureException(
            String message
    ) {
        super(message);
    }
}