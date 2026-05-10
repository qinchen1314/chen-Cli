package com.paicli.agent.plan;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanExecutorTest {
    @Test
    void runsIndependentTasksConcurrently() {
        TaskGraph graph = new TaskGraph();
        graph.addTask(new PlanTask("T1", TaskType.RESEARCH, "A", "A"));
        graph.addTask(new PlanTask("T2", TaskType.REASONING, "B", "B"));
        PlanExecutor executor = new PlanExecutor(2, task -> {
            Thread.sleep(300);
            return task.id();
        });

        long started = System.currentTimeMillis();
        PlanExecutionResult result = executor.execute(graph);
        long elapsed = System.currentTimeMillis() - started;

        assertTrue(result.success());
        assertTrue(elapsed < 550, "expected concurrent execution, elapsed=" + elapsed);
    }

    @Test
    void respectsDependencies() {
        TaskGraph graph = new TaskGraph();
        graph.addTask(new PlanTask("T1", TaskType.RESEARCH, "A", "A"));
        graph.addTask(new PlanTask("T2", TaskType.CODING, "B", "B"));
        graph.addDependency("T2", "T1");
        List<String> order = new ArrayList<>();
        PlanExecutor executor = new PlanExecutor(2, task -> {
            order.add(task.id());
            return task.id();
        });

        PlanExecutionResult result = executor.execute(graph);

        assertTrue(result.success());
        assertEquals(List.of("T1", "T2"), order);
    }

    @Test
    void failedDependencyBlocksDownstreamTasks() {
        TaskGraph graph = new TaskGraph();
        graph.addTask(new PlanTask("T1", TaskType.RESEARCH, "A", "A"));
        graph.addTask(new PlanTask("T2", TaskType.CODING, "B", "B"));
        graph.addDependency("T2", "T1");
        PlanExecutor executor = new PlanExecutor(2, task -> {
            throw new IllegalStateException("boom");
        });

        PlanExecutionResult result = executor.execute(graph);

        assertFalse(result.success());
        assertEquals(TaskStatus.FAILED, graph.get("T1").status());
        assertEquals(TaskStatus.FAILED, graph.get("T2").status());
    }
}
