package com.paicli.agent.plan;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskGraphTest {
    @Test
    void supportsSixTypesAndValidStatusTransitions() {
        assertEquals(6, TaskType.values().length);
        PlanTask task = new PlanTask("T1", TaskType.RESEARCH, "Research", "Inspect project");

        task.transitionTo(TaskStatus.READY);
        task.transitionTo(TaskStatus.RUNNING);
        task.transitionTo(TaskStatus.SUCCEEDED);

        assertEquals(TaskStatus.SUCCEEDED, task.status());
        assertThrows(InvalidTaskTransitionException.class, () -> task.transitionTo(TaskStatus.FAILED));
    }

    @Test
    void tracksDependenciesInBothDirections() {
        TaskGraph graph = new TaskGraph();
        graph.addTask(new PlanTask("T1", TaskType.RESEARCH, "Research", "Inspect"));
        graph.addTask(new PlanTask("T2", TaskType.CODING, "Code", "Implement"));

        graph.addDependency("T2", "T1");

        assertEquals(Set.of("T1"), graph.get("T2").dependsOn());
        assertEquals(Set.of("T2"), graph.get("T1").dependents());
    }

    @Test
    void topologicalSortPlacesDependenciesFirst() {
        TaskGraph graph = new TaskGraph();
        graph.addTask(new PlanTask("T1", TaskType.RESEARCH, "Research", "Inspect"));
        graph.addTask(new PlanTask("T2", TaskType.CODING, "Code", "Implement"));
        graph.addTask(new PlanTask("T3", TaskType.VALIDATION, "Test", "Validate"));
        graph.addDependency("T3", "T2");
        graph.addDependency("T2", "T1");

        List<String> order = new TopologicalSorter().sort(graph).stream()
                .map(PlanTask::id)
                .collect(Collectors.toList());

        assertTrue(order.indexOf("T1") < order.indexOf("T2"));
        assertTrue(order.indexOf("T2") < order.indexOf("T3"));
    }

    @Test
    void detectsCycles() {
        TaskGraph graph = new TaskGraph();
        graph.addTask(new PlanTask("T1", TaskType.RESEARCH, "Research", "Inspect"));
        graph.addTask(new PlanTask("T2", TaskType.CODING, "Code", "Implement"));
        graph.addDependency("T1", "T2");
        graph.addDependency("T2", "T1");

        CycleDetectedException error = assertThrows(CycleDetectedException.class, graph::validateAcyclic);
        assertTrue(error.getMessage().contains("Cycle detected"));
        assertTrue(error.getMessage().contains("T1"));
        assertTrue(error.getMessage().contains("T2"));
    }
}
