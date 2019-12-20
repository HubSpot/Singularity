package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.CrashLoopType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskFailureEvent;
import com.hubspot.singularity.TaskFailureType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityCrashLoops {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCrashLoops.class);

  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final DeployManager deployManager;

  @Inject
  public SingularityCrashLoops(SingularityConfiguration configuration,
                               TaskManager taskManager,
                               DeployManager deployManager) {
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.deployManager = deployManager;
  }

  boolean shouldEnterCooldown(SingularityRequest request, RequestState requestState, SingularityDeployStatistics deployStatistics, long failureTimestamp) {
    if (requestState != RequestState.ACTIVE || !request.isAlwaysRunning()) {
      return false;
    }

    return shouldBeInCooldown(deployStatistics, Optional.of(failureTimestamp));
  }

  private boolean shouldBeInCooldown(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return cooldownStart(deployStatistics, recentFailureTimestamp).isPresent();
  }

  private Optional<Long> cooldownStart(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    long threshold = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);
    List<Long> failureTimestamps = deployStatistics.getTaskFailureEvents().stream()
        .map(TaskFailureEvent::getTimestamp)
        .collect(Collectors.toList());;
    recentFailureTimestamp.ifPresent(failureTimestamps::add);
    List<Long> pastThreshold = failureTimestamps.stream()
        .filter((t) -> t > threshold)
        .collect(Collectors.toList());
    if (pastThreshold.size() > 5) {
      return pastThreshold.stream().max(Comparator.comparingLong(Long::longValue));
    }
    return Optional.empty();
  }

  boolean hasCooldownExpired(SingularityDeployStatistics deployStatistics, Optional<Long> recentFailureTimestamp) {
    return !shouldBeInCooldown(deployStatistics, recentFailureTimestamp);
  }

  Set<CrashLoopInfo> getActiveCrashLoops(SingularityDeployStatistics deployStatistics) {
    Set<CrashLoopInfo> active = new HashSet<>();

    if (deployStatistics.getTaskFailureEvents().isEmpty()) {
      return active;
    }

    Optional<SingularityPendingDeploy> maybePending = deployManager.getPendingDeploy(deployStatistics.getRequestId());
    if (maybePending.isPresent() && maybePending.get().getDeployMarker().getDeployId().equals(deployStatistics.getDeployId())) {
      LOG.debug("Not checking cooldown for pending deploy {} - {}", deployStatistics.getRequestId(), deployStatistics.getDeployId());
      return active;
    }

    long now = System.currentTimeMillis();

    // Check fast failures
    cooldownStart(deployStatistics, Optional.empty())
        .ifPresent((start) ->
            active.add(new CrashLoopInfo(
                deployStatistics.getRequestId(),
                deployStatistics.getDeployId(),
                start,
                Optional.empty(),
                CrashLoopType.FAST_FAILURE_LOOP
            )));

    /*
     * Startup failure loop
     * a) multiple instances failing healthchecks, or single instance failing healthchecks too many times in X minutes
     * b) small count of failures (3?) but instance no matches one that is in cleaning state waiting for a replacement
     */
    Map<Integer, Long> taskCleanStartTimes = taskManager.getCleanupTasks().stream()
        .filter((t) -> t.getTaskId().getRequestId().equals(deployStatistics.getRequestId()) && t.getTaskId().getDeployId().equals(deployStatistics.getDeployId()))
        .collect(Collectors.toMap(
            (t) -> t.getTaskId().getInstanceNo(),
            SingularityTaskCleanup::getTimestamp,
            Math::max
        ));
    Map<Integer, List<Long>> recentStartupFailures = deployStatistics.getTaskFailureEvents()
        .stream()
        .filter((e) -> e.getType() == TaskFailureType.STARTUP_FAILURE
            && taskCleanStartTimes.containsKey(e.getInstance())
            && e.getTimestamp() > taskCleanStartTimes.get(e.getInstance()))
        .collect(Collectors.groupingBy(
            TaskFailureEvent::getInstance,
            Collectors.mapping(TaskFailureEvent::getTimestamp, Collectors.toList()))
        );

    int totalFailures = 0;
    for (Map.Entry<Integer, List<Long>> entry : recentStartupFailures.entrySet()) {
      if (entry.getValue().size() > 2) { // TODO - only 2 in a row per instance? Maybe 3?
        active.add(new CrashLoopInfo(
            deployStatistics.getRequestId(),
            deployStatistics.getDeployId(),
            entry.getValue().stream().min(Comparator.comparingLong(Long::longValue)).get(),
            Optional.empty(),
            CrashLoopType.STARTUP_FAILURE_LOOP)
        );
        break;
      }
      totalFailures += entry.getValue().size();
      if (totalFailures > 5) { // TODO - configurable? Percentage of instance count?
        active.add(new CrashLoopInfo(
            deployStatistics.getRequestId(),
            deployStatistics.getDeployId(),
            entry.getValue().stream().min(Comparator.comparingLong(Long::longValue)).get(),
            Optional.empty(),
            CrashLoopType.STARTUP_FAILURE_LOOP)
        );
        break;
      }
    }

    /*
     * OOM Danger. > X OOMs in Y minutes across all instances
     */
    long thresholdOomTime = now - TimeUnit.MINUTES.toMillis(30);
    List<Long> oomFailures = deployStatistics.getTaskFailureEvents()
        .stream()
        .filter((e) -> e.getType() == TaskFailureType.OOM && e.getTimestamp() > thresholdOomTime)
        .map(TaskFailureEvent::getTimestamp)
        .collect(Collectors.toList());

    if (oomFailures.size() > 3) { // TODO - configurable?
      active.add(new CrashLoopInfo(
          deployStatistics.getRequestId(),
          deployStatistics.getDeployId(),
          oomFailures.stream().min(Comparator.comparingLong(Long::longValue)).get(),
          Optional.empty(),
          CrashLoopType.STARTUP_FAILURE_LOOP)
      );
    }

    /*
     * Single instance failure. > X failures with same instance no in X minutes
     * Multi instance failure. > X% of instances failing within Y minutes
     */
    Map<Integer, List<Long>> recentFailuresByInstance = deployStatistics.getTaskFailureEvents()
        .stream()
        .filter((e) -> e.getType() == TaskFailureType.OOM || e.getType() == TaskFailureType.BAD_EXIT_CODE || e.getType() == TaskFailureType.OUT_OF_DISK_SPACE)
        .collect(Collectors.groupingBy(
            TaskFailureEvent::getInstance,
            Collectors.mapping(TaskFailureEvent::getTimestamp, Collectors.toList()))
        );

    for (Map.Entry<Integer, List<Long>> entry : recentFailuresByInstance.entrySet()) {

    }

    /*
     * Unexpected Exits. Too many task finished from a long-running type in X minutes
     */

    /*
     * Slow failures. Occasional failures, count on order of hours, looking for consistency in non-zero count each hour
     */


    return active;
  }
}
