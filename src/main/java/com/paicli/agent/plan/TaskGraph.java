package com.paicli.agent.plan;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class TaskGraph {
    private final Map<String, PlanTask> tasks = new LinkedHashMap<>();

    public void addTask(PlanTask task) {
        Objects.requireNonNull(task, "task");
        if (tasks.containsKey(task.id())) {
            throw new PlanValidationException("Duplicate task id: " + task.id());
        }
        tasks.put(task.id(), task);
    }

    public void addDependency(String taskId, String dependencyId) {
        PlanTask task = requireTask(taskId);
        PlanTask dependency = requireTask(dependencyId);
        if (task.id().equals(dependency.id())) {
            throw new PlanValidationException("Task cannot depend on itself: " + task.id());
        }
        task.addDependency(dependency.id());
        dependency.addDependent(task.id());
    }

    public PlanTask get(String id) {
        return requireTask(id);
    }

    public Collection<PlanTask> tasks() {
        return Collections.unmodifiableCollection(tasks.values());
    }

    public int size() {
        return tasks.size();
    }

    public void validateAcyclic() {
        new TopologicalSorter().sort(this);
    }

    private PlanTask requireTask(String id) {
        PlanTask task = tasks.get(id);
        if (task == null) {
            throw new PlanValidationException("Unknown task id: " + id);
        }
        return task;
    }
}
