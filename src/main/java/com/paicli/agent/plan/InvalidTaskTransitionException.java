package com.paicli.agent.plan;

public class InvalidTaskTransitionException extends RuntimeException {
    public InvalidTaskTransitionException(TaskStatus from, TaskStatus to) {
        super("Invalid task status transition: " + from + " -> " + to);
    }
}
