package com.hubspot.singularity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskReconciliationStatistics {
    private final long taskReconciliationStartedAt;
    private final long taskReconciliationDurationMillis;
    private final int taskReconciliationIterations;
    private final List<Integer> taskReconciliationCounts;

    @JsonCreator
    public SingularityTaskReconciliationStatistics(@JsonProperty("taskReconciliationStartedAt") long taskReconciliationStartedAt,
        @JsonProperty("taskReconciliationDurationMillis") long taskReconciliationDurationMillis,
        @JsonProperty("taskReconciliationIterations") int taskReconciliationIterations,
        @JsonProperty("taskReconciliationCounts") List<Integer> taskReconciliationCounts) {
        this.taskReconciliationStartedAt = taskReconciliationStartedAt;
        this.taskReconciliationDurationMillis = taskReconciliationDurationMillis;
        this.taskReconciliationIterations = taskReconciliationIterations;
        this.taskReconciliationCounts = taskReconciliationCounts;
    }

    public long getTaskReconciliationStartedAt() {
        return taskReconciliationStartedAt;
    }

    public long getTaskReconciliationDurationMillis() {
        return taskReconciliationDurationMillis;
    }

    public int getTaskReconciliationIterations() {
        return taskReconciliationIterations;
    }

    public List<Integer> getTaskReconciliationCounts() {
        return taskReconciliationCounts;
    }

    @Override
    public String toString() {
        return "SingularityTaskReconciliationStatistics{" +
            "taskReconciliationStartedAt=" + taskReconciliationStartedAt +
            ", taskReconciliationDurationMillis=" + taskReconciliationDurationMillis +
            ", taskReconciliationIterations=" + taskReconciliationIterations +
            ", taskReconciliationCounts=" + taskReconciliationCounts +
            '}';
    }
}
