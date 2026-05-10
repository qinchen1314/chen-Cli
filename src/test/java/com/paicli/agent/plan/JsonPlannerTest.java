package com.paicli.agent.plan;

import com.paicli.llm.GLMClient;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonPlannerTest {
    private final JsonPlanner planner = new JsonPlanner(new GLMClient(Map.of()), 1, 10);

    @Test
    void parsesForwardReferences() {
        String json = """
                {
                  "tasks": [
                    {
                      "id": "T1",
                      "type": "RESEARCH",
                      "title": "Research",
                      "description": "Inspect existing code",
                      "dependsOn": ["T2"]
                    },
                    {
                      "id": "T2",
                      "type": "REASONING",
                      "title": "Reason",
                      "description": "Decide approach",
                      "dependsOn": []
                    }
                  ]
                }
                """;

        TaskGraph graph = planner.parse(json);

        assertEquals(2, graph.size());
        assertEquals(1, graph.get("T1").dependsOn().size());
        assertEquals(1, graph.get("T2").dependents().size());
    }

    @Test
    void rejectsDuplicateIds() {
        String json = """
                {"tasks": [
                  {"id": "T1", "type": "RESEARCH", "title": "A", "description": "A", "dependsOn": []},
                  {"id": "T1", "type": "CODING", "title": "B", "description": "B", "dependsOn": []}
                ]}
                """;

        assertThrows(PlanValidationException.class, () -> planner.parse(json));
    }

    @Test
    void rejectsCyclesFromJson() {
        String json = """
                {"tasks": [
                  {"id": "T1", "type": "RESEARCH", "title": "A", "description": "A", "dependsOn": ["T2"]},
                  {"id": "T2", "type": "CODING", "title": "B", "description": "B", "dependsOn": ["T1"]}
                ]}
                """;

        assertThrows(CycleDetectedException.class, () -> planner.parse(json));
    }
}
