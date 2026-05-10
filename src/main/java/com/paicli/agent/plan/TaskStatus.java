package com.paicli.agent.plan;

public enum TaskStatus {
    PENDING,
    READY,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED;
    }
}
