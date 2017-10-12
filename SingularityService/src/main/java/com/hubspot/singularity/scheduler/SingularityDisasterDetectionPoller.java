package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterDataPoint;
import com.hubspot.singularity.SingularityDisasterDataPoints;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.config.DisasterDetectionConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.hubspot.singularity.smtp.SingularityMailer;

public class SingularityDisasterDetectionPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDisasterDetectionPoller.class);

  private final SingularityConfiguration configuration;
  private final DisasterDetectionConfiguration disasterConfiguration;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final DisasterManager disasterManager;
  private final SingularityMailer mailer;
  private final Multiset<Reason> taskLostReasons;
  private final AtomicInteger activeSlavesLost;

  @Inject
  public SingularityDisasterDetectionPoller(SingularityConfiguration configuration,  TaskManager taskManager, SlaveManager slaveManager, DisasterManager disasterManager, SingularityMailer mailer,
                                            @Named(SingularityMesosModule.TASK_LOST_REASONS_COUNTER) Multiset<Reason> taskLostReasons, @Named(SingularityMesosModule.ACTIVE_SLAVES_LOST_COUNTER) AtomicInteger activeSlavesLost) {
    super(configuration.getDisasterDetection().getRunEveryMillis(), TimeUnit.MILLISECONDS);
    this.configuration = configuration;
    this.disasterConfiguration = configuration.getDisasterDetection();
    this.taskManager = taskManager;
    this.slaveManager = slaveManager;
    this.disasterManager = disasterManager;
    this.mailer = mailer;
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

    clearExpiredDisabledActions();

    List<SingularityDisasterType> previouslyActiveDisasters = disasterManager.getActiveDisasters();
    List<SingularityDisasterDataPoint> dataPoints = disasterManager.getDisasterStats().getDataPoints();
    SingularityDisasterDataPoint newStats = collectDisasterStats();
    dataPoints.add(0, newStats);

    if (dataPoints.size() > disasterConfiguration.getStatsHistorySize()) {
      dataPoints.remove(dataPoints.size() - 1);
    }

    LOG.debug("Collected new disaster detection dataPoints: {}", newStats);

    List<SingularityDisasterType> newActiveDisasters = checkDataPoints(dataPoints);
    if (!newActiveDisasters.isEmpty()) {
      LOG.warn("Detected new active disasters: {}", newActiveDisasters);
    }

    disasterManager.updateActiveDisasters(previouslyActiveDisasters, newActiveDisasters);
    disasterManager.saveDisasterStats(new SingularityDisasterDataPoints(dataPoints));

    if (!newActiveDisasters.isEmpty()) {
      if (!disasterManager.isAutomatedDisabledActionsDisabled()) {
        disasterManager.addDisabledActionsForDisasters(newActiveDisasters);
      }
      if (!previouslyActiveDisasters.containsAll(newActiveDisasters)) {
        queueDisasterEmail(dataPoints, newActiveDisasters);
      }
    } else {
      disasterManager.clearSystemGeneratedDisabledActions();
    }
  }

  private void clearExpiredDisabledActions() {
    for (SingularityDisabledAction disabledAction : disasterManager.getDisabledActions()) {
      if (disabledAction.getExpiresAt().isPresent() && System.currentTimeMillis() > disabledAction.getExpiresAt().get()) {
        disasterManager.enable(disabledAction.getType());
      }
    }
  }

  private SingularityDisasterDataPoint collectDisasterStats() {
    long now = System.currentTimeMillis();

    int numActiveTasks = taskManager.getNumActiveTasks();
    List<SingularityPendingTaskId> pendingTasks = taskManager.getPendingTaskIds();
    int numPendingTasks = pendingTasks.size();
    int numLateTasks = 0;
    long totalTaskLagMillis = 0;
    int numPastDueTasks = 0;

    for (SingularityPendingTaskId pendingTask : pendingTasks) {
      long taskLagMillis = now - pendingTask.getNextRunAt();
      if (taskLagMillis > 0) {
        numPastDueTasks++;
        totalTaskLagMillis += taskLagMillis;
        if (taskLagMillis > configuration.getDeltaAfterWhichTasksAreLateMillis()) {
          numLateTasks++;
        }
      }
    }

    long avgTaskLagMillis = totalTaskLagMillis / Math.max(numPastDueTasks, 1);

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

    return new SingularityDisasterDataPoint(now, numActiveTasks, numPendingTasks, numLateTasks, avgTaskLagMillis, numLostTasks, numRunningSlaves, numLostSlaves);
  }

  private List<SingularityDisasterType> checkDataPoints(List<SingularityDisasterDataPoint> dataPoints) {
    List<SingularityDisasterType> activeDisasters = new ArrayList<>();

    if (!dataPoints.isEmpty()) {
      long now = System.currentTimeMillis();

      if (disasterConfiguration.isCheckLateTasks() && tooMuchTaskLag(now, dataPoints)) {
        activeDisasters.add(SingularityDisasterType.EXCESSIVE_TASK_LAG);
      }
      if (disasterConfiguration.isCheckLostSlaves() && tooManyLostSlaves(now, dataPoints)) {
        activeDisasters.add(SingularityDisasterType.LOST_SLAVES);
      }
      if (disasterConfiguration.isCheckLostTasks() && tooManyLostTasks(now, dataPoints)) {
        activeDisasters.add(SingularityDisasterType.LOST_TASKS);
      }
    }

    return activeDisasters;
  }

  private boolean tooMuchTaskLag(long now, List<SingularityDisasterDataPoint> dataPoints) {
    Optional<Long> criticalAvgLagTriggeredSince = Optional.absent();
    Optional<Long> warningAvgLagTriggeredSince = Optional.absent();
    Optional<Long> criticalPortionTriggeredSince = Optional.absent();
    Optional<Long> warningPortionTriggeredSince = Optional.absent();

    for (SingularityDisasterDataPoint dataPoint : dataPoints) {
      double overdueTaskPortion = dataPoint.getNumLateTasks() / (double) Math.max((dataPoint.getNumActiveTasks() + dataPoint.getNumPendingTasks()), 1);
      boolean criticalOverdueTasksPortion = overdueTaskPortion > disasterConfiguration.getCriticalOverdueTaskPortion();
      boolean warningOverdueTasksPortion = overdueTaskPortion > disasterConfiguration.getWarningOverdueTaskPortion();
      boolean criticalAvgTaskLag = dataPoint.getAvgTaskLagMillis() > disasterConfiguration.getCriticalAvgTaskLagMillis() && warningOverdueTasksPortion;
      boolean warningAvgTaskLag = dataPoint.getAvgTaskLagMillis() > disasterConfiguration.getWarningAvgTaskLagMillis();

      if (criticalOverdueTasksPortion) {
        criticalPortionTriggeredSince = Optional.of(dataPoint.getTimestamp());
      }
      if (warningOverdueTasksPortion) {
        warningPortionTriggeredSince = Optional.of(dataPoint.getTimestamp());
      }
      if (criticalAvgTaskLag) {
        criticalAvgLagTriggeredSince = Optional.of(dataPoint.getTimestamp());
      }
      if (warningAvgTaskLag) {
        warningAvgLagTriggeredSince = Optional.of(dataPoint.getTimestamp());
      }
      if (!criticalOverdueTasksPortion && !warningOverdueTasksPortion && !criticalAvgTaskLag && !warningAvgTaskLag) {
        break;
      }
    }

    // 'true' if either critical condition is met
    if ((criticalAvgLagTriggeredSince.isPresent() && now - criticalAvgLagTriggeredSince.get() > disasterConfiguration.getTriggerAfterMillisOverTaskLagThreshold())
      || (criticalPortionTriggeredSince.isPresent() && now - criticalPortionTriggeredSince.get() > disasterConfiguration.getTriggerAfterMillisOverTaskLagThreshold())) {
      return true;
    }

    // 'true' if both warning conditions are met
    return warningAvgLagTriggeredSince.isPresent() && now - warningAvgLagTriggeredSince.get() > disasterConfiguration.getTriggerAfterMillisOverTaskLagThreshold()
      && warningPortionTriggeredSince.isPresent() && now - warningPortionTriggeredSince.get() > disasterConfiguration.getTriggerAfterMillisOverTaskLagThreshold();

  }

  private boolean tooManyLostSlaves(long now, List<SingularityDisasterDataPoint> dataPoints) {
    int totalLostSlaves = 0;
    for (SingularityDisasterDataPoint dataPoint : dataPoints) {
      if (now - dataPoint.getTimestamp() < disasterConfiguration.getIncludeLostSlavesInLastMillis()) {
        totalLostSlaves += dataPoint.getNumLostSlaves();
      }
    }
    double lostSlavesPortion = totalLostSlaves / (double) (Math.max(dataPoints.get(0).getNumActiveSlaves() + dataPoints.get(0).getNumLostSlaves(), 1));
    return lostSlavesPortion > disasterConfiguration.getCriticalLostSlavePortion();
  }

  private boolean tooManyLostTasks(long now, List<SingularityDisasterDataPoint> dataPoints) {
    int totalLostTasks = 0;
    for (SingularityDisasterDataPoint dataPoint : dataPoints) {
      if (now - dataPoint.getTimestamp() < disasterConfiguration.getIncludeLostTasksInLastMillis()) {
        totalLostTasks += dataPoint.getNumLostTasks();
      }
    }

    double lostTasksPortion = totalLostTasks / (double) Math.max(dataPoints.get(0).getNumActiveTasks(), 1);
    return lostTasksPortion > disasterConfiguration.getCriticalLostTaskPortion();
  }

  private void queueDisasterEmail(List<SingularityDisasterDataPoint> dataPoints, List<SingularityDisasterType> disasters) {
    SingularityDisastersData data = new SingularityDisastersData(
      dataPoints,
      disasterManager.getAllDisasterStates(disasters),
      disasterManager.isAutomatedDisabledActionsDisabled()
    );

    mailer.sendDisasterMail(data);
  }

}
