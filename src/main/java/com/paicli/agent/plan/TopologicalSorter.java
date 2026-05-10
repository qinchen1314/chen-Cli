package com.paicli.agent.plan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopologicalSorter {
    private enum Mark {
        UNVISITED,
        VISITING,
        VISITED
    }

    public List<PlanTask> sort(TaskGraph graph) {
        Map<String, Mark> marks = new HashMap<>();
        List<PlanTask> ordered = new ArrayList<>();
        Deque<String> stack = new ArrayDeque<>();
        for (PlanTask task : graph.tasks()) {
            marks.put(task.id(), Mark.UNVISITED);
        }
        for (PlanTask task : graph.tasks()) {
            visit(graph, task, marks, stack, ordered);
        }
        return ordered;
    }

    private void visit(
            TaskGraph graph,
            PlanTask task,
            Map<String, Mark> marks,
            Deque<String> stack,
            List<PlanTask> ordered
    ) {
        Mark mark = marks.get(task.id());
        if (mark == Mark.VISITING) {
            throw new CycleDetectedException("Cycle detected: " + renderCycle(stack, task.id()));
        }
        if (mark == Mark.VISITED) {
            return;
        }

        marks.put(task.id(), Mark.VISITING);
        stack.push(task.id());
        for (String dependencyId : task.dependsOn()) {
            visit(graph, graph.get(dependencyId), marks, stack, ordered);
        }
        stack.pop();
        marks.put(task.id(), Mark.VISITED);
        ordered.add(task);
    }

    private String renderCycle(Deque<String> stack, String repeatedId) {
        List<String> path = new ArrayList<>(stack);
        int start = path.indexOf(repeatedId);
        if (start < 0) {
            return repeatedId + " -> " + repeatedId;
        }
        List<String> cycle = new ArrayList<>();
        for (int i = start; i >= 0; i--) {
            cycle.add(path.get(i));
        }
        cycle.add(repeatedId);
        return String.join(" -> ", cycle);
    }
}
