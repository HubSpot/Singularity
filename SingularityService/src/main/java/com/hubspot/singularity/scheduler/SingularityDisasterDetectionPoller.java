package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mesos.Protos.TaskStatus.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityDisasterStats;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityScheduledTasksInfo;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.DisasterDetectionConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosModule;

public class SingularityDisasterDetectionPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDisasterDetectionPoller.class);

  private final SingularityConfiguration configuration;
  private final DisasterDetectionConfiguration disasterConfiguration;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final DisasterManager disasterManager;
  private final Multiset<Reason> taskLostReasons;
  private final AtomicInteger activeSlavesLost;

  @Inject
  public SingularityDisasterDetectionPoller(SingularityConfiguration configuration,  TaskManager taskManager, SlaveManager slaveManager, DisasterManager disasterManager,
                                            @Named(SingularityMesosModule.TASK_LOST_REASONS_COUNTER) Multiset<Reason> taskLostReasons, @Named(SingularityMesosModule.ACTIVE_SLAVES_LOST_COUNTER) AtomicInteger activeSlavesLost) {
    super(configuration.getDisasterDetection().getRunEveryMillis(), TimeUnit.MILLISECONDS);
    this.configuration = configuration;
    this.disasterConfiguration = configuration.getDisasterDetection();
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.disasterManager = disasterManager;
    this.taskLostReasons = taskLostReasons;
    this.activeSlavesLost = activeSlavesLost;
  }

  @Override
  protected boolean isEnabled() {
    return disasterConfiguration.isEnabled();
  }

  @Override
  protected boolean abortsOnError() {
    return false;
  }

  @Override
  public void runActionOnPoll() {
    LOG.trace("Starting disaster detection");
    List<SingularityDisasterType> previouslyActiveDisasters = disasterManager.getActiveDisasters();
    Optional<SingularityDisasterStats> lastStats = disasterManager.getDisasterStats();
    SingularityDisasterStats newStats = collectNewStats();

    LOG.debug("Collected new disaster detection stats: {}", newStats);

    List<SingularityDisasterType> newActiveDisasters = checkStats(lastStats, newStats);
    if (!newActiveDisasters.isEmpty()) {
      LOG.warn("Detected new active disasters: {}", newActiveDisasters);
    }

    disasterManager.updateActiveDisasters(previouslyActiveDisasters, newActiveDisasters);
    disasterManager.saveDisasterStats(newStats);

    if (!newActiveDisasters.isEmpty() && !disasterManager.isAutomatedDisabledActionsDisabled()) {
      disasterManager.addDisabledActionsForDisasters(newActiveDisasters);
    } else {
      disasterManager.clearSystemGeneratedDisabledActions();
    }
  }

  private SingularityDisasterStats collectNewStats() {
    long now = System.currentTimeMillis();

    int numActiveTasks = taskManager.getNumActiveTasks();
    List<SingularityPendingTask> pendingTasks = taskManager.getPendingTasks();
    SingularityScheduledTasksInfo scheduledTasksInfo = SingularityScheduledTasksInfo.getInfo(pendingTasks, configuration.getDeltaAfterWhichTasksAreLateMillis());
    int numPendingTasks = pendingTasks.size();
    int numLateTasks = scheduledTasksInfo.getNumLateTasks();
    long totalTaskLagMillis = 0;
    int numPastDueTasks = 0;

    for (SingularityPendingTask pendingTask : pendingTasks) {
      long taskLagMillis = now - pendingTask.getPendingTaskId().getNextRunAt();
      if (taskLagMillis > disasterConfiguration.getConsiderOverdueAfterMillis()) {
        numLateTasks++;
      }
      if (taskLagMillis > 0) {
        numPastDueTasks++;
        totalTaskLagMillis += taskLagMillis;
      }
    }

    long avgTaskLagMillis = totalTaskLagMillis / numPastDueTasks;

    List<SingularitySlave> slaves = slaveManager.getObjects();
    int numRunningSlaves = 0;
    for (SingularitySlave slave : slaves) {
      if (slave.getCurrentState().getState() != MachineState.DEAD && slave.getCurrentState().getState() != MachineState.MISSING_ON_STARTUP) {
        numRunningSlaves++;
      }
    }

    int numLostSlaves = activeSlavesLost.getAndSet(0);

    int numLostTasks = 0;
    for (Reason lostTaskReason : disasterConfiguration.getLostTaskReasons()) {
      numLostTasks += taskLostReasons.count(lostTaskReason);
    }
    taskLostReasons.clear();

    return new SingularityDisasterStats(now, numActiveTasks, numPendingTasks, numLateTasks, avgTaskLagMillis, numLostTasks, numRunningSlaves, numLostSlaves);
  }

  private List<SingularityDisasterType> checkStats(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    List<SingularityDisasterType> activeDisasters = new ArrayList<>();

    if (disasterConfiguration.isCheckLateTasks() && tooMuchTaskLag(lastStats, newStats)) {
      activeDisasters.add(SingularityDisasterType.EXCESSIVE_TASK_LAG);
    }
    if (disasterConfiguration.isCheckLostSlaves() && tooManyLostSlaves(lastStats, newStats)) {
      activeDisasters.add(SingularityDisasterType.LOST_SLAVES);
    }
    if (disasterConfiguration.isCheckLostTasks() && tooManyLostTasks(lastStats, newStats)) {
      activeDisasters.add(SingularityDisasterType.LOST_TASKS);
    }

    return activeDisasters;
  }

  private boolean tooMuchTaskLag(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    double overdueTaskPortion = newStats.getNumLateTasks() / (double) Math.max((newStats.getNumActiveTasks() + newStats.getNumPendingTasks()), 1);
    boolean criticalOverdueTasksPortion = overdueTaskPortion > disasterConfiguration.getCriticalOverdueTaskPortion();
    boolean criticalAvgTaskLag = newStats.getAvgTaskLagMillis() > disasterConfiguration.getCriticalAvgTaskLagMillis();

    return criticalAvgTaskLag && criticalOverdueTasksPortion;
  }

  private boolean tooManyLostSlaves(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    int effectiveLostSlaves;
    if (disasterConfiguration.isIncludePreviousLostSlavesCount()) {
      effectiveLostSlaves = lastStats.isPresent() ? lastStats.get().getNumLostSlaves() + newStats.getNumLostSlaves() : newStats.getNumLostSlaves();
    } else {
      effectiveLostSlaves = newStats.getNumLostSlaves();
    }
    double lostSlavesPortion = effectiveLostSlaves / (double) (Math.max(newStats.getNumActiveSlaves() + newStats.getNumLostSlaves(), 1));
    return lostSlavesPortion > disasterConfiguration.getCriticalLostSlavePortion();
  }

  private boolean tooManyLostTasks(Optional<SingularityDisasterStats> lastStats, SingularityDisasterStats newStats) {
    int effectiveLostTasks;
    if (disasterConfiguration.isIncludePreviousLostTaskCount()) {
      effectiveLostTasks = lastStats.isPresent() ? lastStats.get().getNumLostTasks() + newStats.getNumLostTasks() : newStats.getNumLostTasks();
    } else {
      effectiveLostTasks = newStats.getNumLostTasks();
    }
    double lostTasksPortion = effectiveLostTasks / (double) Math.max(newStats.getNumActiveTasks(), 1);
    return lostTasksPortion > disasterConfiguration.getCriticalLostTaskPortion();
  }

}
