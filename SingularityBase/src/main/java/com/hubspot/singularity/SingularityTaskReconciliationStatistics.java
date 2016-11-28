package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityTaskReconciliationStatistics {
    private final long taskReconciliationStartedAt;
    private final long taskReconciliationDurationMillis;
    private final int taskReconciliationIterations;
    private final long taskReconciliationResponseCount;
    private final long taskReconciliationResponseMax;
    private final double taskReconciliationResponseMean;
    private final long taskReconciliationResponseMin;
    private final double taskReconciliationResponseP50;
    private final double taskReconciliationResponseP75;
    private final double taskReconciliationResponseP95;
    private final double taskReconciliationResponseP98;
    private final double taskReconciliationResponseP99;
    private final double taskReconciliationResponseP999;
    private final double taskReconciliationResponseStddev;

    @JsonCreator
    public SingularityTaskReconciliationStatistics(@JsonProperty("taskReconciliationStartedAt") long taskReconciliationStartedAt,
        @JsonProperty("taskReconciliationDurationMillis") long taskReconciliationDurationMillis,
        @JsonProperty("taskReconciliationIterations") int taskReconciliationIterations,
        @JsonProperty("taskReconciliationResponseCount") long taskReconciliationResponseCount,
        @JsonProperty("taskReconciliationResponseMax") long taskReconciliationResponseMax,
        @JsonProperty("taskReconciliationResponseMean") double taskReconciliationResponseMean,
        @JsonProperty("taskReconciliationResponseMin") long taskReconciliationResponseMin,
        @JsonProperty("taskReconciliationResponseP50") double taskReconciliationResponseP50,
        @JsonProperty("taskReconciliationResponseP75") double taskReconciliationResponseP75,
        @JsonProperty("taskReconciliationResponseP95") double taskReconciliationResponseP95,
        @JsonProperty("taskReconciliationResponseP98") double taskReconciliationResponseP98,
        @JsonProperty("taskReconciliationResponseP99") double taskReconciliationResponseP99,
        @JsonProperty("taskReconciliationResponseP999") double taskReconciliationResponseP999,
        @JsonProperty("taskReconciliationResponseStddev") double taskReconciliationResponseStddev) {
        this.taskReconciliationStartedAt = taskReconciliationStartedAt;
        this.taskReconciliationDurationMillis = taskReconciliationDurationMillis;
        this.taskReconciliationIterations = taskReconciliationIterations;
        this.taskReconciliationResponseCount = taskReconciliationResponseCount;
        this.taskReconciliationResponseMax = taskReconciliationResponseMax;
        this.taskReconciliationResponseMean = taskReconciliationResponseMean;
        this.taskReconciliationResponseMin = taskReconciliationResponseMin;
        this.taskReconciliationResponseP50 = taskReconciliationResponseP50;
        this.taskReconciliationResponseP75 = taskReconciliationResponseP75;
        this.taskReconciliationResponseP95 = taskReconciliationResponseP95;
        this.taskReconciliationResponseP98 = taskReconciliationResponseP98;
        this.taskReconciliationResponseP99 = taskReconciliationResponseP99;
        this.taskReconciliationResponseP999 = taskReconciliationResponseP999;
        this.taskReconciliationResponseStddev = taskReconciliationResponseStddev;
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

    public long getTaskReconciliationResponseCount() {
        return taskReconciliationResponseCount;
    }

    public long getTaskReconciliationResponseMax() {
        return taskReconciliationResponseMax;
    }

    public double getTaskReconciliationResponseMean() {
        return taskReconciliationResponseMean;
    }

    public long getTaskReconciliationResponseMin() {
        return taskReconciliationResponseMin;
    }

    public double getTaskReconciliationResponseP50() {
        return taskReconciliationResponseP50;
    }

    public double getTaskReconciliationResponseP75() {
        return taskReconciliationResponseP75;
    }

    public double getTaskReconciliationResponseP95() {
        return taskReconciliationResponseP95;
    }

    public double getTaskReconciliationResponseP98() {
        return taskReconciliationResponseP98;
    }

    public double getTaskReconciliationResponseP99() {
        return taskReconciliationResponseP99;
    }

    public double getTaskReconciliationResponseP999() {
        return taskReconciliationResponseP999;
    }

    public double getTaskReconciliationResponseStddev() {
        return taskReconciliationResponseStddev;
    }

    @Override
    public String toString() {
        return "SingularityTaskReconciliationStatistics{" +
            "taskReconciliationStartedAt=" + taskReconciliationStartedAt +
            ", taskReconciliationDurationMillis=" + taskReconciliationDurationMillis +
            ", taskReconciliationIterations=" + taskReconciliationIterations +
            ", taskReconciliationResponseCount=" + taskReconciliationResponseCount +
            ", taskReconciliationResponseMax=" + taskReconciliationResponseMax +
            ", taskReconciliationResponseMean=" + taskReconciliationResponseMean +
            ", taskReconciliationResponseMin=" + taskReconciliationResponseMin +
            ", taskReconciliationResponseP50=" + taskReconciliationResponseP50 +
            ", taskReconciliationResponseP75=" + taskReconciliationResponseP75 +
            ", taskReconciliationResponseP95=" + taskReconciliationResponseP95 +
            ", taskReconciliationResponseP98=" + taskReconciliationResponseP98 +
            ", taskReconciliationResponseP99=" + taskReconciliationResponseP99 +
            ", taskReconciliationResponseP999=" + taskReconciliationResponseP999 +
            ", taskReconciliationResponseStddev=" + taskReconciliationResponseStddev +
            '}';
    }
}
