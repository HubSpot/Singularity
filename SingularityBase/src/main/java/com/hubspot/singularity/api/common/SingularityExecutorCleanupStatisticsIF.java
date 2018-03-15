package com.hubspot.singularity.api.common;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;


@Immutable
@SingularityStyle
public interface SingularityExecutorCleanupStatisticsIF {
  int getTotalTaskFiles();

  int getMesosRunningTasks();

  int getWaitingTasks();

  int getRunningTasksIgnored();

  int getSuccessfullyCleanedTasks();

  int getIoErrorTasks();

  int getErrorTasks();

  int getInvalidTasks();

  Optional<String> getErrorMessage();
}
