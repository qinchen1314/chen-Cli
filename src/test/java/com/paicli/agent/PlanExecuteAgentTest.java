package com.paicli.agent;

import com.paicli.agent.plan.PlanExecutor;
import com.paicli.agent.plan.PlanTask;
import com.paicli.agent.plan.TaskGraph;
import com.paicli.agent.plan.TaskType;
import com.paicli.llm.GLMClient;
import com.paicli.tool.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanExecuteAgentTest {
    @Test
    void switchesBetweenReactAndPlanMode() {
        Agent react = new Agent(new FakeGLMClient(), new ToolRegistry());
        PlanExecuteAgent agent = new PlanExecuteAgent(react, goal -> {
            TaskGraph graph = new TaskGraph();
            graph.addTask(new PlanTask("T1", TaskType.RESPONSE, "Answer", goal));
            return graph;
        }, new PlanExecutor(1, task -> "done"));

        assertFalse(agent.shouldUsePlanMode("quick say hello"));
        assertTrue(agent.shouldUsePlanMode("plan and execute the implementation with tests"));
        assertTrue(agent.run("plan and execute the implementation with tests").contains("Plan mode: SUCCEEDED"));
    }

    private static class FakeGLMClient extends GLMClient {
        FakeGLMClient() {
            super(Map.of());
        }

        @Override
        public String chat(String systemPrompt, String userPrompt) {
            return "react";
        }
    }
}
