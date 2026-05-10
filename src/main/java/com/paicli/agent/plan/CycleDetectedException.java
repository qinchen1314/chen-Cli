package com.paicli.agent.plan;

public class CycleDetectedException extends RuntimeException {
    public CycleDetectedException(String message) {
        super(message);
    }
}
