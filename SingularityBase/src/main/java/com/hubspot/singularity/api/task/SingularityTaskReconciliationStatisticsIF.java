package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Describes the last run of task reconciliation with the mesos master")
public interface SingularityTaskReconciliationStatisticsIF {
  @Schema(description = "Start time of the last reconciliation")
  long getTaskReconciliationStartedAt();

  @Schema(description = "Duration in milliseconds of the last reconciliation")
  long getTaskReconciliationDurationMillis();

  @Schema(description = "Number of iterations required for the last reconciliation")
  int getTaskReconciliationIterations();

  @Schema(description = "Responses counted for the last reconciliation")
  long getTaskReconciliationResponseCount();

  @Schema(description = "Max time taken for a response during the last reconciliation")
  long getTaskReconciliationResponseMax();

  @Schema(description = "Average time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseMean();

  @Schema(description = "Minimum time taken for a response during the last reconciliation")
  long getTaskReconciliationResponseMin();

  @Schema(description = "50th percentile time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseP50();

  @Schema(description = "75th percentile time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseP75();

  @Schema(description = "95th percentile time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseP95();

  @Schema(description = "98th percentile time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseP98();

  @Schema(description = "99th percentile time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseP99();

  @Schema(description = "99.9th percentile time taken for a response during the last reconciliation")
  double getTaskReconciliationResponseP999();

  @Schema(description = "Standard deviation in response time during the last reconciliation")
  double getTaskReconciliationResponseStddev();
}
