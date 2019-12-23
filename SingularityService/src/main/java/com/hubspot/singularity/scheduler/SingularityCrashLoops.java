package com.hubspot.singularity.scheduler;

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
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.TaskFailureEvent;
import com.hubspot.singularity.TaskFailureType;
import com.hubspot.singularity.config.CrashLoopConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;

@Singleton
public class SingularityCrashLoops {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCrashLoops.class);

  private final CrashLoopConfiguration configuration;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;

  @Inject
  public SingularityCrashLoops(SingularityConfiguration configuration,
                               TaskManager taskManager,
                               DeployManager deployManager,
                               RequestManager requestManager) {
    this.configuration = configuration.getCrashLoopConfiguration();
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
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
    long threshold = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(configuration.getEvaluateCooldownOverMinutes());
    List<Long> failureTimestamps = deployStatistics.getTaskFailureEvents().stream()
        .map(TaskFailureEvent::getTimestamp)
        .collect(Collectors.toList());
    ;
    recentFailureTimestamp.ifPresent(failureTimestamps::add);
    List<Long> pastThreshold = failureTimestamps.stream()
        .filter((t) -> t > threshold)
        .collect(Collectors.toList());
    if (pastThreshold.size() >= configuration.getCooldownFailureThreshold()) {
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

    Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(deployStatistics.getRequestId());
    if (!maybeRequest.isPresent()) {
      return active;
    }

    long now = System.currentTimeMillis();

    // Check fast failures
    Optional<Long> maybeCooldownStart = cooldownStart(deployStatistics, Optional.empty());
    if (maybeCooldownStart.isPresent()) {
      active.add(new CrashLoopInfo(
          deployStatistics.getRequestId(),
          deployStatistics.getDeployId(),
          maybeCooldownStart.get(),
          Optional.empty(),
          CrashLoopType.FAST_FAILURE_LOOP
      ));
    }

    /*
     * Startup failure loop
     * a) small count of failures but instance num matches one that is in cleaning state waiting for a replacement
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
            && taskCleanStartTimes.containsKey(e.getInstance()))
        .collect(Collectors.groupingBy(
            TaskFailureEvent::getInstance,
            Collectors.mapping(TaskFailureEvent::getTimestamp, Collectors.toList()))
        );

    boolean hasStartupFailure = false;
    for (Map.Entry<Integer, List<Long>> entry : recentStartupFailures.entrySet()) {
      if (taskCleanStartTimes.containsKey(entry.getKey())) {
        if (entry.getValue().stream().filter((t) -> t > taskCleanStartTimes.get(entry.getKey())).count() > 2) {
          active.add(new CrashLoopInfo(
              deployStatistics.getRequestId(),
              deployStatistics.getDeployId(),
              entry.getValue().stream().min(Comparator.comparingLong(Long::longValue)).get(),
              Optional.empty(),
              CrashLoopType.STARTUP_FAILURE_LOOP)
          );
          hasStartupFailure = true;
          break;
        }
      }
    }

    /*
     * Startup failure loop
     * b) multiple instances failing healthchecks too many times in X minutes
     */
    if (!hasStartupFailure) {
      long startupFailThreshold = now - TimeUnit.MINUTES.toMillis(configuration.getEvaluateStartupLoopOverMinutes());
      List<Long> recentStartupFailTimestamps = recentStartupFailures.values()
          .stream()
          .flatMap(List::stream)
          .filter((t) -> t > startupFailThreshold)
          .collect(Collectors.toList());
      if (recentStartupFailTimestamps.size() > configuration.getStartupFailureThreshold()) {
        active.add(new CrashLoopInfo(
            deployStatistics.getRequestId(),
            deployStatistics.getDeployId(),
            recentStartupFailTimestamps.stream().min(Comparator.comparingLong(Long::longValue)).get(),
            Optional.empty(),
            CrashLoopType.STARTUP_FAILURE_LOOP)
        );
      }
    }

    /*
     * OOM Danger. > X OOMs in Y minutes across all instances
     */
    long thresholdOomTime = now - TimeUnit.MINUTES.toMillis(configuration.getEvaluateOomsOverMinutes());
    List<Long> oomFailures = deployStatistics.getTaskFailureEvents()
        .stream()
        .filter((e) -> e.getType() == TaskFailureType.OOM && e.getTimestamp() > thresholdOomTime)
        .map(TaskFailureEvent::getTimestamp)
        .collect(Collectors.toList());

    if (oomFailures.size() >= configuration.getOomFailureThreshold()) {
      active.add(new CrashLoopInfo(
          deployStatistics.getRequestId(),
          deployStatistics.getDeployId(),
          oomFailures.stream().min(Comparator.comparingLong(Long::longValue)).get(),
          Optional.empty(),
          CrashLoopType.OOM)
      );
    }

    /*
     * Single instance failure. > X failures with same instance no in X minutes, bucketed to avoid counting fast failure as one of these
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
      Optional<Long> maybeCrashStart = getStartForFailuresInBuckets(
          now,
          entry.getValue(),
          TimeUnit.MINUTES.toMillis(configuration.getSingleInstanceFailureBucketSizeMinutes()),
          configuration.getSingleInstanceFailureBuckets(),
          configuration.getSingleInstanceFailureThreshold()
      );
      if (maybeCrashStart.isPresent()) {
        active.add(new CrashLoopInfo(
            deployStatistics.getRequestId(),
            deployStatistics.getDeployId(),
            maybeCrashStart.get(),
            Optional.empty(),
            CrashLoopType.SINGLE_INSTANCE_FAILURE_LOOP)
        );
        break;
      }
    }

    Optional<Long> maybeMultiCrashStart = getStartForFailuresInBuckets(
        now,
        recentFailuresByInstance.values().stream().flatMap(List::stream).collect(Collectors.toList()),
        TimeUnit.MINUTES.toMillis(configuration.getMultiInstanceFailureBucketSizeMinutes()),
        configuration.getMultiInstanceFailureBuckets(),
        configuration.getMultiInstanceFailureThreshold()
    );
    if (maybeMultiCrashStart.isPresent()) {
      active.add(new CrashLoopInfo(
          deployStatistics.getRequestId(),
          deployStatistics.getDeployId(),
          maybeMultiCrashStart.get(),
          Optional.empty(),
          CrashLoopType.MULTI_INSTANCE_FAILURE)
      );
    }

    if (maybeRequest.get().getRequest().isLongRunning()) {
      /*
       * Slow failures. Occasional failures, count on order of hours, looking for consistency in non-zero count each hour
       */
      getStartForFailuresInBuckets(
          now,
          recentFailuresByInstance,
          TimeUnit.MINUTES.toMillis(configuration.getSlowFailureBucketSizeMinutes()),
          configuration.getSlowFailureBuckets(),
          configuration.getSlowFailureThreshold()
      ).ifPresent((start) ->
          active.add(new CrashLoopInfo(
              deployStatistics.getRequestId(),
              deployStatistics.getDeployId(),
              start,
              Optional.empty(),
              CrashLoopType.SLOW_FAILURES)
          ));

      getUnexpectedExitLoop(now, deployStatistics)
          .ifPresent(active::add);
    }

    return active;
  }

  /*
   * Unexpected Exits. Too many task finished from a long-running type in X minutes
   */
  private Optional<CrashLoopInfo> getUnexpectedExitLoop(long now, SingularityDeployStatistics deployStatistics) {
    long thresholdUnexpectedExitTime = now - TimeUnit.MINUTES.toMillis(30); // TODO - configurable?
    List<Long> recentUnexpectedExits = deployStatistics.getTaskFailureEvents()
        .stream()
        .filter((e) -> e.getType() == TaskFailureType.UNEXPECTED_EXIT && e.getTimestamp() > thresholdUnexpectedExitTime)
        .map(TaskFailureEvent::getTimestamp)
        .collect(Collectors.toList());
    if (recentUnexpectedExits.size() > 4) { // TODO - configurable?
      return Optional.of(new CrashLoopInfo(
          deployStatistics.getRequestId(),
          deployStatistics.getDeployId(),
          recentUnexpectedExits.stream().min(Comparator.comparingLong(Long::longValue)).get(),
          Optional.empty(),
          CrashLoopType.UNEXPECTED_EXITS));
    }
    return Optional.empty();
  }

  private Optional<Long> getStartForFailuresInBuckets(long now, Map<Integer, List<Long>> recentFailuresByInstance, long bucketSizeMillis, int numBuckets, double percentThreshold) {
    return getStartForFailuresInBuckets(
        now,
        recentFailuresByInstance.values().stream().flatMap(List::stream).collect(Collectors.toList()),
        bucketSizeMillis,
        numBuckets,
        percentThreshold
    );
  }

  private Optional<Long> getStartForFailuresInBuckets(long now, List<Long> recentFailures, long bucketSizeMillis, int numBuckets, double percentThreshold) {
    long thresholdFailTimeMillis = now - (bucketSizeMillis * numBuckets);
    Map<Long, List<Long>> bucketedFailures = recentFailures.stream()
        .filter((t) -> t > thresholdFailTimeMillis)
        .collect(Collectors.groupingBy(
            (e) -> e / bucketSizeMillis
        ));
    long bucketsWithFailure = bucketedFailures.entrySet()
        .stream()
        .filter((e) -> !e.getValue().isEmpty())
        .count();
    if ((double) bucketsWithFailure / numBuckets > percentThreshold) {
      return recentFailures.stream().min(Comparator.comparingLong(Long::longValue));
    }
    return Optional.empty();
  }
}
