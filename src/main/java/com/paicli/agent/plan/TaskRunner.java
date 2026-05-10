package com.paicli.agent.plan;

@FunctionalInterface
public interface TaskRunner {
    String run(PlanTask task) throws Exception;
}
