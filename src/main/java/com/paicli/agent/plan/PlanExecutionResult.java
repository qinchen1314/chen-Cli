package com.paicli.agent.plan;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlanExecutionResult {
    private final boolean success;
    private final Map<String, String> outputs;

    public PlanExecutionResult(boolean success, Map<String, String> outputs) {
        this.success = success;
        this.outputs = Collections.unmodifiableMap(new LinkedHashMap<>(outputs));
    }

    public boolean success() {
        return success;
    }

    public Map<String, String> outputs() {
        return outputs;
    }
}
