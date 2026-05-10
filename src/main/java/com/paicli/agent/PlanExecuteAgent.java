package com.paicli.agent;

import com.paicli.agent.plan.PlanExecutionResult;
import com.paicli.agent.plan.PlanExecutor;
import com.paicli.agent.plan.PlanTask;
import com.paicli.agent.plan.Planner;
import com.paicli.agent.plan.TaskGraph;

import java.util.Locale;
import java.util.stream.Collectors;

public class PlanExecuteAgent {
    private final Agent reactAgent;
    private final Planner planner;
    private final PlanExecutor executor;

    public PlanExecuteAgent(Agent reactAgent, Planner planner, PlanExecutor executor) {
        this.reactAgent = reactAgent;
        this.planner = planner;
        this.executor = executor;
    }

    public String run(String userInput) {
        if (!shouldUsePlanMode(userInput)) {
            return reactAgent.run(userInput);
        }
        TaskGraph graph = planner.plan(userInput);
        PlanExecutionResult result = executor.execute(graph);
        String plan = graph.tasks().stream()
                .map(task -> "- " + task.id() + " [" + task.type() + "/" + task.status() + "] " + task.title())
                .collect(Collectors.joining(System.lineSeparator()));
        String outputs = result.outputs().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
        return "Plan mode: " + (result.success() ? "SUCCEEDED" : "FAILED")
                + System.lineSeparator() + plan
                + System.lineSeparator() + outputs;
    }

    public boolean shouldUsePlanMode(String userInput) {
        String normalized = userInput.toLowerCase(Locale.ROOT);
        if (containsAny(normalized, "no planning", "quick", "directly execute")) {
            return false;
        }
        if (containsAny(normalized, "plan and execute", "break down", "multi-step", "dag", "topological")) {
            return true;
        }
        int score = 0;
        if (userInput.length() > 120) {
            score++;
        }
        if (containsAny(normalized, "implement", "refactor", "integrate", "test", "validate", "maven", "jline")) {
            score++;
        }
        if (containsAny(normalized, "multiple", "several", "dependency", "concurrent", "planner")) {
            score++;
        }
        return score >= 2;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
