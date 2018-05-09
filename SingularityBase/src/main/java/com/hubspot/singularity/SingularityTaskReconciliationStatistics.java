package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes the last run of task reconciliation with the mesos master")
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

    @Schema(description = "Start time of the last reconciliation")
    public long getTaskReconciliationStartedAt() {
        return taskReconciliationStartedAt;
    }

    @Schema(description = "Duration in milliseconds of the last reconciliation")
    public long getTaskReconciliationDurationMillis() {
        return taskReconciliationDurationMillis;
    }

    @Schema(description = "Number of iterations required for the last reconciliation")
    public int getTaskReconciliationIterations() {
        return taskReconciliationIterations;
    }

    @Schema(description = "Responses counted for the last reconciliation")
    public long getTaskReconciliationResponseCount() {
        return taskReconciliationResponseCount;
    }

    @Schema(description = "Max time taken for a response during the last reconciliation")
    public long getTaskReconciliationResponseMax() {
        return taskReconciliationResponseMax;
    }

    @Schema(description = "Average time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseMean() {
        return taskReconciliationResponseMean;
    }

    @Schema(description = "Minimum time taken for a response during the last reconciliation")
    public long getTaskReconciliationResponseMin() {
        return taskReconciliationResponseMin;
    }

    @Schema(description = "50th percentile time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseP50() {
        return taskReconciliationResponseP50;
    }

    @Schema(description = "75th percentile time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseP75() {
        return taskReconciliationResponseP75;
    }

    @Schema(description = "95th percentile time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseP95() {
        return taskReconciliationResponseP95;
    }

    @Schema(description = "98th percentile time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseP98() {
        return taskReconciliationResponseP98;
    }

    @Schema(description = "99th percentile time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseP99() {
        return taskReconciliationResponseP99;
    }

    @Schema(description = "99.9th percentile time taken for a response during the last reconciliation")
    public double getTaskReconciliationResponseP999() {
        return taskReconciliationResponseP999;
    }

    @Schema(description = "Standard deviation in response time during the last reconciliation")
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
