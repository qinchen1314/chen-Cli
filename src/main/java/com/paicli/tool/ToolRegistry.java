package com.paicli.tool;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class ToolRegistry {
    private final Map<String, Function<String, String>> tools = new LinkedHashMap<>();

    public ToolRegistry() {
        register("echo", input -> input);
        register("time", input -> Instant.now().toString());
    }

    public void register(String name, Function<String, String> tool) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must not be blank");
        }
        tools.put(name.trim(), tool);
    }

    public String execute(String name, String input) {
        Function<String, String> tool = tools.get(name);
        if (tool == null) {
            throw new IllegalArgumentException("Unknown tool: " + name);
        }
        return tool.apply(input == null ? "" : input);
    }

    public Set<String> names() {
        return Collections.unmodifiableSet(tools.keySet());
    }
}
