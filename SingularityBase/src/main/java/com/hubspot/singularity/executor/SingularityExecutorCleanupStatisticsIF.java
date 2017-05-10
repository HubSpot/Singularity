package com.hubspot.singularity.executor;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

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
