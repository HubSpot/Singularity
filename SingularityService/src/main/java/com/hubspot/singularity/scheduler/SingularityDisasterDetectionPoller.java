package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisabledActionType;
import com.hubspot.singularity.SingularityDisasterStats;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.DisasterDetectionConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityDisasterDetectionPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDisasterDetectionPoller.class);

  private final DisasterDetectionConfiguration configuration;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final DisasterManager disasterManager;

  @Inject
  public SingularityDisasterDetectionPoller(SingularityConfiguration configuration,  TaskManager taskManager, SlaveManager slaveManager, DisasterManager disasterManager) {
    super(configuration.getDisasterDetection().getRunEveryMillis(), TimeUnit.MILLISECONDS);
    this.configuration = configuration.getDisasterDetection();
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.disasterManager = disasterManager;
  }

  @Override
  protected boolean isEnabled() {
    return configuration.isEnabled();
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

  @Override
  public void runActionOnPoll() {
    List<SingularityDisasterType> previouslyActiveDisasters = disasterManager.getActiveDisasters();
    Optional<SingularityDisasterStats> lastStats = disasterManager.getDisasterStats();
    SingularityDisasterStats newStats = collectNewStats(lastStats);
    updateActiveDisastersAndDisabledActions(previouslyActiveDisasters, checkStats(lastStats, newStats));
    disasterManager.saveDisasterStats(newStats);
  }

  private SingularityDisasterStats collectNewStats(Optional<SingularityDisasterStats> lastStats) {
    long now = System.currentTimeMillis();

    // Pending and active task data
    int numActiveTasks = taskManager.getNumActiveTasks();
    List<SingularityPendingTask> pendingTasks = taskManager.getPendingTasks();
    int numPendingTasks = pendingTasks.size();
    int numOverdueTasks = 0;
    long totalTaskLagMillis = 0;
    int numPastDueTasks = 0;

    for (SingularityPendingTask pendingTask : pendingTasks) {
      long taskLagMillis = now - pendingTask.getPendingTaskId().getNextRunAt();
      if (taskLagMillis > configuration.getConsiderOverdueAfterMillis()) {
        numOverdueTasks++;
      }
      if (taskLagMillis > 0) {
        numPastDueTasks++;
        totalTaskLagMillis += taskLagMillis;
      }
    }

    long avgTaskLagMillis = totalTaskLagMillis / numPastDueTasks;

    // Active/Inactive slave data
    List<SingularitySlave> slaves = slaveManager.getObjects();
    int numRunningSlaves = 0;
    int numLostSlaves = 0;
    for (SingularitySlave slave : slaves) {
      if (slave.getCurrentState().getState() != MachineState.DEAD && slave.getCurrentState().getState() != MachineState.MISSING_ON_STARTUP) {
        numRunningSlaves++;
      } else {
        if (now - slave.getCurrentState().getTimestamp() < now - configuration.getCheckLostSlavesInLastMillis()) {
          numLostSlaves ++;
        }
      }
    }


    return new SingularityDisasterStats(now, numActiveTasks, numPendingTasks, numOverdueTasks, avgTaskLagMillis, numRunningSlaves, numLostSlaves);
  }

  private void updateActiveDisastersAndDisabledActions(List<SingularityDisasterType> previouslyActiveDisasters, List<SingularityDisasterType> newActiveDisasters) {
    for (SingularityDisasterType disaster : previouslyActiveDisasters) {
      if (!newActiveDisasters.contains(disaster)) {
        disasterManager.removeDisaster(disaster);
      }
    }

    for (SingularityDisasterType disaster : newActiveDisasters) {
      disasterManager.addDisaster(disaster);
    }

    if (!newActiveDisasters.isEmpty()) {
      addDisabledActions(newActiveDisasters);
    } else {
      clearSystemGeneratedDisabledActions();
    }
  }

  private void addDisabledActions(List<SingularityDisasterType> newActiveDisasters) {
    String message = String.format("Active disasters detected: (%s)", newActiveDisasters);
    for (SingularityDisabledActionType action : configuration.getDisableActionsOnDisaster()) {
      disasterManager.disable(action, Optional.of(message), Optional.<SingularityUser>absent(), true);
    }
  }

  private void clearSystemGeneratedDisabledActions() {
    for (SingularityDisabledAction disabledAction : disasterManager.getDisabledActions()) {
      if (disabledAction.isSystemGenerated()) {
        disasterManager.enable(disabledAction.getType());
      }
    }
  }

  private List<SingularityDisasterType> checkStats(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    List<SingularityDisasterType> activeDisasters = new ArrayList<>();

    if (configuration.isCheckOverdueTasks() && tooMuchTaskLag(lastStats, newStats)) {
      activeDisasters.add(SingularityDisasterType.EXCESSIVE_TASK_LAG);
    }
    if (configuration.isCheckLostSlaves() && tooManyLostSlaves(lastStats, newStats)) {
      activeDisasters.add(SingularityDisasterType.LOST_SLAVES);
    }

    return activeDisasters;
  }

  private boolean tooMuchTaskLag(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    double overdueTaskPortion = newStats.getNumOverdueTasks() / (newStats.getNumActiveTasks() + newStats.getNumPendingTasks());
    boolean criticalOverdueTasksPortion = overdueTaskPortion > configuration.getCriticalOverdueTaskPortion();
    boolean criticalAvgTaskLag = newStats.getAvgTaskLagMillis() > configuration.getCriticalAvgTaskLagMillis();

    if (configuration.isRequireAllConditionsForOverdueTaskDisaster()) {
      return criticalAvgTaskLag && criticalOverdueTasksPortion;
    } else {
      return criticalAvgTaskLag || criticalOverdueTasksPortion;
    }
  }

  private boolean tooManyLostSlaves(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    double lostSlavesPortion = newStats.getNumLostSlaves() / (newStats.getNumActiveSlaves() + newStats.getNumLostSlaves());
    return lostSlavesPortion > configuration.getCriticalLostSlavePortion();
  }

}
