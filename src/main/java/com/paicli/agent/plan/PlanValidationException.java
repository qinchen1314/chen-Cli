package com.paicli.agent.plan;

public class PlanValidationException extends RuntimeException {
    public PlanValidationException(String message) {
        super(message);
    }

    public PlanValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
