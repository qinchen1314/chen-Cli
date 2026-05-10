package com.paicli.agent.plan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paicli.llm.GLMClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class JsonPlanner implements Planner {
    private static final int DEFAULT_MIN_TASKS = 5;
    private static final int DEFAULT_MAX_TASKS = 10;

    private final GLMClient glmClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int minTasks;
    private final int maxTasks;

    public JsonPlanner(GLMClient glmClient) {
        this(glmClient, DEFAULT_MIN_TASKS, DEFAULT_MAX_TASKS);
    }

    public JsonPlanner(GLMClient glmClient, int minTasks, int maxTasks) {
        this.glmClient = glmClient;
        this.minTasks = minTasks;
        this.maxTasks = maxTasks;
    }

    @Override
    public TaskGraph plan(String goal) {
        String response = glmClient.chat(systemPrompt(), userPrompt(goal));
        return parse(response);
    }

    public TaskGraph parse(String jsonText) {
        try {
            JsonNode root = objectMapper.readTree(stripMarkdownFence(jsonText));
            JsonNode tasksNode = root.path("tasks");
            if (!tasksNode.isArray()) {
                throw new PlanValidationException("Planner JSON must contain a tasks array");
            }
            int count = tasksNode.size();
            if (count < minTasks || count > maxTasks) {
                throw new PlanValidationException("Planner must return " + minTasks + "-" + maxTasks + " tasks, got " + count);
            }

            TaskGraph graph = new TaskGraph();
            List<PendingDependencies> pendingDependencies = new ArrayList<>();
            for (JsonNode node : tasksNode) {
                String id = requireText(node, "id");
                TaskType type = parseType(requireText(node, "type"));
                String title = requireText(node, "title");
                String description = requireText(node, "description");
                graph.addTask(new PlanTask(id, type, title, description));

                List<String> dependencies = new ArrayList<>();
                JsonNode depsNode = node.path("dependsOn");
                if (depsNode.isMissingNode() || depsNode.isNull()) {
                    pendingDependencies.add(new PendingDependencies(id, dependencies));
                } else if (!depsNode.isArray()) {
                    throw new PlanValidationException("dependsOn must be an array for task " + id);
                } else {
                    for (JsonNode dep : depsNode) {
                        String dependencyId = dep.asText();
                        if (dependencyId == null || dependencyId.isBlank()) {
                            throw new PlanValidationException("Blank dependency id for task " + id);
                        }
                        dependencies.add(dependencyId.trim());
                    }
                    pendingDependencies.add(new PendingDependencies(id, dependencies));
                }
            }

            for (PendingDependencies pending : pendingDependencies) {
                for (String dependencyId : pending.dependencies()) {
                    graph.addDependency(pending.taskId(), dependencyId);
                }
            }
            graph.validateAcyclic();
            return graph;
        } catch (PlanValidationException | CycleDetectedException e) {
            throw e;
        } catch (Exception e) {
            throw new PlanValidationException("Invalid planner JSON", e);
        }
    }

    private String systemPrompt() {
        return """
                You are PaiCLI Planner. Return strict JSON only, no markdown.
                Break complex goals into 5-10 executable DAG tasks.
                Each task must have id, type, title, description, dependsOn.
                Allowed types: RESEARCH, REASONING, CODING, TOOL_CALL, VALIDATION, RESPONSE.
                Dependencies may reference tasks declared later, but the final graph must be acyclic.
                """;
    }

    private String userPrompt(String goal) {
        return "Create an executable plan for this goal:\n" + goal;
    }

    private String stripMarkdownFence(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String requireText(JsonNode node, String field) {
        String value = node.path(field).asText();
        if (value == null || value.isBlank()) {
            throw new PlanValidationException("Task field is required: " + field);
        }
        return value.trim();
    }

    private TaskType parseType(String value) {
        try {
            return TaskType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new PlanValidationException("Unknown task type: " + value, e);
        }
    }

    private record PendingDependencies(String taskId, List<String> dependencies) {
    }
}
