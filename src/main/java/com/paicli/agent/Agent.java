package com.paicli.agent;

import com.paicli.llm.GLMClient;
import com.paicli.tool.ToolRegistry;

public class Agent {
    private final GLMClient glmClient;
    private final ToolRegistry tools;

    public Agent(GLMClient glmClient, ToolRegistry tools) {
        this.glmClient = glmClient;
        this.tools = tools;
    }

    public String run(String userInput) {
        if (userInput.startsWith("/tool ")) {
            String[] parts = userInput.substring("/tool ".length()).split(" ", 2);
            String name = parts[0];
            String input = parts.length > 1 ? parts[1] : "";
            return tools.execute(name, input);
        }
        return glmClient.chat("You are PaiCLI ReAct agent. Answer concisely and use tools only when requested.", userInput);
    }
}
