package com.paicli.agent.plan;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class PlanTask {
    private final String id;
    private final TaskType type;
    private final String title;
    private final String description;
    private final Set<String> dependsOn = new LinkedHashSet<>();
    private final Set<String> dependents = new LinkedHashSet<>();
    private TaskStatus status = TaskStatus.PENDING;

    public PlanTask(String id, TaskType type, String title, String description) {
        this.id = requireText(id, "id");
        this.type = Objects.requireNonNull(type, "type");
        this.title = requireText(title, "title");
        this.description = requireText(description, "description");
    }

    public String id() {
        return id;
    }

    public TaskType type() {
        return type;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public synchronized TaskStatus status() {
        return status;
    }

    public synchronized void transitionTo(TaskStatus next) {
        Objects.requireNonNull(next, "next");
        if (!canTransition(status, next)) {
            throw new InvalidTaskTransitionException(status, next);
        }
        status = next;
    }

    public Set<String> dependsOn() {
        return Collections.unmodifiableSet(dependsOn);
    }

    public Set<String> dependents() {
        return Collections.unmodifiableSet(dependents);
    }

    void addDependency(String dependencyId) {
        dependsOn.add(requireText(dependencyId, "dependencyId"));
    }

    void addDependent(String dependentId) {
        dependents.add(requireText(dependentId, "dependentId"));
    }

    private static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case PENDING -> to == TaskStatus.READY || to == TaskStatus.FAILED;
            case READY -> to == TaskStatus.RUNNING || to == TaskStatus.FAILED;
            case RUNNING -> to == TaskStatus.SUCCEEDED || to == TaskStatus.FAILED;
            case SUCCEEDED, FAILED -> false;
        };
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    @Override
    public String toString() {
        return id + "[" + type + "," + status() + "] " + title;
    }
}
