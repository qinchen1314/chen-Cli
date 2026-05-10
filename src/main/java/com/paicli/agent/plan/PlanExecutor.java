package com.paicli.agent.plan;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PlanExecutor {
    private final int maxParallelism;
    private final TaskRunner runner;

    public PlanExecutor(int maxParallelism, TaskRunner runner) {
        if (maxParallelism < 1) {
            throw new IllegalArgumentException("maxParallelism must be >= 1");
        }
        this.maxParallelism = maxParallelism;
        this.runner = runner;
    }

    public PlanExecutionResult execute(TaskGraph graph) {
        graph.validateAcyclic();
        int workers = Math.min(maxParallelism, Math.max(1, graph.size()));
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        CompletionService<TaskOutcome> completionService = new ExecutorCompletionService<>(executor);
        Map<String, Integer> remainingDependencies = new HashMap<>();
        Queue<PlanTask> ready = new ArrayDeque<>();
        Map<String, String> outputs = new LinkedHashMap<>();

        for (PlanTask task : graph.tasks()) {
            int dependencyCount = task.dependsOn().size();
            remainingDependencies.put(task.id(), dependencyCount);
            if (dependencyCount == 0) {
                task.transitionTo(TaskStatus.READY);
                ready.add(task);
            }
        }

        int completed = 0;
        int running = 0;
        boolean success = true;
        try {
            while (completed < graph.size()) {
                while (!ready.isEmpty()) {
                    PlanTask task = ready.remove();
                    task.transitionTo(TaskStatus.RUNNING);
                    completionService.submit(() -> runTask(task));
                    running++;
                }

                if (running == 0) {
                    break;
                }

                Future<TaskOutcome> future = completionService.take();
                TaskOutcome outcome = future.get();
                running--;
                completed++;
                outputs.put(outcome.taskId(), outcome.output());

                PlanTask task = graph.get(outcome.taskId());
                if (outcome.success()) {
                    task.transitionTo(TaskStatus.SUCCEEDED);
                    for (String dependentId : task.dependents()) {
                        PlanTask dependent = graph.get(dependentId);
                        int remaining = remainingDependencies.merge(dependentId, -1, Integer::sum);
                        if (remaining == 0 && dependent.status() == TaskStatus.PENDING) {
                            dependent.transitionTo(TaskStatus.READY);
                            ready.add(dependent);
                        }
                    }
                } else {
                    success = false;
                    task.transitionTo(TaskStatus.FAILED);
                    completed += failDownstream(graph, task);
                }
            }
            return new PlanExecutionResult(success && completed == graph.size(), outputs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PlanValidationException("Plan execution interrupted", e);
        } catch (Exception e) {
            throw new PlanValidationException("Plan execution failed", e);
        } finally {
            executor.shutdownNow();
        }
    }

    private TaskOutcome runTask(PlanTask task) {
        try {
            return new TaskOutcome(task.id(), true, runner.run(task));
        } catch (Exception e) {
            return new TaskOutcome(task.id(), false, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private int failDownstream(TaskGraph graph, PlanTask failedTask) {
        int marked = 0;
        for (String dependentId : failedTask.dependents()) {
            PlanTask dependent = graph.get(dependentId);
            if (!dependent.status().isTerminal()) {
                dependent.transitionTo(TaskStatus.FAILED);
                marked++;
                marked += failDownstream(graph, dependent);
            }
        }
        return marked;
    }

    private record TaskOutcome(String taskId, boolean success, String output) {
    }
}
