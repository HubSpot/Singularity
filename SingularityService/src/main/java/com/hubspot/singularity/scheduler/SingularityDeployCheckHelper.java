package com.hubspot.singularity.scheduler;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployFailure;
import com.hubspot.singularity.SingularityDeployFailureReason;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityDeployCheckHelper {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityDeployCheckHelper.class
  );

  private final SingularityConfiguration configuration;
  private final SingularityDeployHealthHelper deployHealthHelper;

  public SingularityDeployCheckHelper(
    SingularityConfiguration configuration,
    SingularityDeployHealthHelper deployHealthHelper
  ) {
    this.configuration = configuration;
    this.deployHealthHelper = deployHealthHelper;
  }

  public boolean isNotInDeployableState(
    Optional<SingularityRequestWithState> maybeRequestWithState
  ) {
    return (
      !(
        maybeRequestWithState.isPresent() &&
        maybeRequestWithState.get().getState() == RequestState.FINISHED
      ) &&
      !(
        configuration.isAllowDeployOfPausedRequests() &&
        maybeRequestWithState.isPresent() &&
        maybeRequestWithState.get().getState() == RequestState.PAUSED
      ) &&
      !SingularityRequestWithState.isActive(maybeRequestWithState)
    );
  }

  public static boolean shouldCancelLoadBalancer(SingularityPendingDeploy pendingDeploy) {
    return (
      pendingDeploy.getLastLoadBalancerUpdate().isPresent() &&
      !pendingDeploy.getCurrentDeployState().isDeployFinished()
    );
  }

  public static Optional<SingularityDeployMarker> findCancel(
    List<SingularityDeployMarker> cancelDeploys,
    SingularityDeployMarker activeDeploy
  ) {
    for (SingularityDeployMarker cancelDeploy : cancelDeploys) {
      if (
        cancelDeploy.getRequestId().equals(activeDeploy.getRequestId()) &&
        cancelDeploy.getDeployId().equals(activeDeploy.getDeployId())
      ) {
        return Optional.of(cancelDeploy);
      }
    }

    return Optional.empty();
  }

  public static Optional<SingularityUpdatePendingDeployRequest> findUpdateRequest(
    List<SingularityUpdatePendingDeployRequest> updateRequests,
    SingularityPendingDeploy pendingDeploy
  ) {
    for (SingularityUpdatePendingDeployRequest updateRequest : updateRequests) {
      if (
        updateRequest
          .getRequestId()
          .equals(pendingDeploy.getDeployMarker().getRequestId()) &&
        updateRequest.getDeployId().equals(pendingDeploy.getDeployMarker().getDeployId())
      ) {
        return Optional.of(updateRequest);
      }
    }
    return Optional.empty();
  }

  public static TaskCleanupType getCleanupType(
    SingularityPendingDeploy pendingDeploy,
    SingularityRequest request,
    SingularityDeployResult deployResult
  ) {
    if (
      pendingDeploy.getDeployProgress().isPresent() &&
      pendingDeploy.getDeployProgress().get().getDeployInstanceCountPerStep() !=
      request.getInstancesSafe()
    ) {
      // For incremental deploys, return a special cleanup type
      if (deployResult.getDeployState() == DeployState.FAILED) {
        return TaskCleanupType.INCREMENTAL_DEPLOY_FAILED;
      } else if (deployResult.getDeployState() == DeployState.CANCELED) {
        return TaskCleanupType.INCREMENTAL_DEPLOY_CANCELLED;
      }
    }
    return deployResult.getDeployState().getCleanupType();
  }

  public boolean isDeployOverdue(
    SingularityPendingDeploy pendingDeploy,
    Optional<SingularityDeploy> deploy
  ) {
    if (!deploy.isPresent()) {
      if (
        System.currentTimeMillis() -
        pendingDeploy.getDeployMarker().getTimestamp() >
        TimeUnit.SECONDS.toMillis(configuration.getDeployHealthyBySeconds())
      ) {
        LOG.warn(
          "Can't determine if deploy {} is overdue because it was missing, but pending time is > {}s, marking as overdue",
          pendingDeploy,
          configuration.getDeployHealthyBySeconds()
        );
        return true;
      } else {
        LOG.warn(
          "Can't determine if deploy {} is overdue because it was missing",
          pendingDeploy
        );
        return false;
      }
    }

    if (
      pendingDeploy.getDeployProgress().isPresent() &&
      pendingDeploy.getDeployProgress().get().isStepComplete()
    ) {
      return false;
    }

    final long startTime = getStartTime(pendingDeploy);

    final long deployDuration = System.currentTimeMillis() - startTime;

    final long allowedTime = getAllowedMillis(deploy.get());

    if (deployDuration > allowedTime) {
      LOG.warn(
        "Deploy {} is overdue (duration: {}), allowed: {}",
        pendingDeploy,
        DurationFormatUtils.formatDurationHMS(deployDuration),
        DurationFormatUtils.formatDurationHMS(allowedTime)
      );

      return true;
    } else {
      LOG.trace(
        "Deploy {} is not yet overdue (duration: {}), allowed: {}",
        pendingDeploy,
        DurationFormatUtils.formatDurationHMS(deployDuration),
        DurationFormatUtils.formatDurationHMS(allowedTime)
      );

      return false;
    }
  }

  private long getStartTime(SingularityPendingDeploy pendingDeploy) {
    if (pendingDeploy.getDeployProgress().isPresent()) {
      return pendingDeploy.getDeployProgress().get().getTimestamp();
    } else {
      return pendingDeploy.getDeployMarker().getTimestamp();
    }
  }

  public long getAllowedMillis(SingularityDeploy deploy) {
    long seconds = deploy
      .getDeployHealthTimeoutSeconds()
      .orElse(configuration.getDeployHealthyBySeconds());

    if (
      deploy.getHealthcheck().isPresent() &&
      !deploy.getSkipHealthchecksOnDeploy().orElse(false)
    ) {
      seconds +=
        deployHealthHelper.getMaxHealthcheckTimeoutSeconds(deploy.getHealthcheck().get());
    } else {
      seconds +=
        deploy
          .getConsiderHealthyAfterRunningForSeconds()
          .orElse(configuration.getConsiderTaskHealthyAfterRunningForSeconds());
    }

    return TimeUnit.SECONDS.toMillis(seconds);
  }

  public static DeployState interpretLoadBalancerState(
    SingularityLoadBalancerUpdate lbUpdate,
    DeployState unknownState
  ) {
    switch (lbUpdate.getLoadBalancerState()) {
      case CANCELED:
        return DeployState.CANCELED;
      case SUCCESS:
        return DeployState.SUCCEEDED;
      case FAILED:
      case INVALID_REQUEST_NOOP:
        return DeployState.FAILED;
      case CANCELING:
        return DeployState.CANCELING;
      case UNKNOWN:
        return unknownState;
      case WAITING:
    }

    return DeployState.WAITING;
  }

  public static PendingType canceledOr(DeployState deployState, PendingType pendingType) {
    if (deployState == DeployState.CANCELED) {
      return PendingType.DEPLOY_CANCELLED;
    } else {
      return pendingType;
    }
  }

  public static boolean shouldCheckLbState(final SingularityPendingDeploy pendingDeploy) {
    return (
      pendingDeploy.getLastLoadBalancerUpdate().isPresent() &&
      getLoadBalancerRequestId(pendingDeploy)
        .getId()
        .equals(
          pendingDeploy
            .getLastLoadBalancerUpdate()
            .get()
            .getLoadBalancerRequestId()
            .getId()
        ) &&
      (
        pendingDeploy.getLastLoadBalancerUpdate().get().getLoadBalancerState() !=
        BaragonRequestState.UNKNOWN
      )
    );
  }

  public static LoadBalancerRequestId getLoadBalancerRequestId(
    SingularityPendingDeploy pendingDeploy
  ) {
    return new LoadBalancerRequestId(
      String.format(
        "%s-%s-%s",
        pendingDeploy.getDeployMarker().getRequestId(),
        pendingDeploy.getDeployMarker().getDeployId(),
        pendingDeploy.getDeployProgress().get().getTargetActiveInstances()
      ),
      LoadBalancerRequestType.DEPLOY,
      Optional.<Integer>empty()
    );
  }

  public static PendingType computePendingType(
    SingularityRequest request,
    List<SingularityTaskId> activeTasks,
    List<SingularityPendingTaskId> pendingTasks
  ) {
    PendingType pendingType = null;
    if (request.isScheduled()) {
      if (activeTasks.isEmpty()) {
        pendingType = PendingType.IMMEDIATE;
      } else {
        // Don't run scheduled task over a running task. Will be picked up on the next run.
        pendingType = PendingType.NEW_DEPLOY;
      }
    } else if (!request.isLongRunning()) {
      if (
        request.getInstances().isPresent() &&
        (activeTasks.size() + pendingTasks.size() < request.getInstances().get())
      ) {
        pendingType = PendingType.ONEOFF;
      } else {
        // Don't run one-off / on-demand task when already at instance count cap
        pendingType = PendingType.NEW_DEPLOY;
      }
    }
    return pendingType;
  }

  public static SingularityPendingRequestBuilder buildBasePendingRequest(
    SingularityRequest request,
    String deployId,
    SingularityDeployResult deployResult,
    SingularityPendingDeploy pendingDeploy,
    SingularityRunNowRequest runNowRequest
  ) {
    return new SingularityPendingRequestBuilder()
      .setRequestId(request.getId())
      .setDeployId(deployId)
      .setTimestamp(deployResult.getTimestamp())
      .setUser(pendingDeploy.getDeployMarker().getUser())
      .setCmdLineArgsList(runNowRequest.getCommandLineArgs())
      .setRunId(
        Optional.of(runNowRequest.getRunId().orElse(UUID.randomUUID().toString()))
      )
      .setSkipHealthchecks(
        runNowRequest.getSkipHealthchecks().isPresent()
          ? runNowRequest.getSkipHealthchecks()
          : request.getSkipHealthchecks()
      )
      .setMessage(
        runNowRequest.getMessage().isPresent()
          ? runNowRequest.getMessage()
          : pendingDeploy.getDeployMarker().getMessage()
      )
      .setResources(runNowRequest.getResources())
      .setRunAsUserOverride(runNowRequest.getRunAsUserOverride())
      .setEnvOverrides(runNowRequest.getEnvOverrides())
      .setExtraArtifacts(runNowRequest.getExtraArtifacts())
      .setRunAt(runNowRequest.getRunAt());
  }

  public static SingularityPendingRequest buildPendingRequest(
    SingularityPendingTask pendingTask,
    SingularityPendingDeploy pendingDeploy
  ) {
    return new SingularityPendingRequest(
      pendingTask.getPendingTaskId().getRequestId(),
      pendingDeploy.getDeployMarker().getDeployId(),
      System.currentTimeMillis(),
      pendingTask.getUser(),
      pendingTask.getPendingTaskId().getPendingType(),
      pendingTask.getCmdLineArgsList(),
      pendingTask.getRunId(),
      pendingTask.getSkipHealthchecks(),
      pendingTask.getMessage(),
      pendingTask.getActionId(),
      pendingTask.getResources(),
      pendingTask.getS3UploaderAdditionalFiles(),
      pendingTask.getRunAsUserOverride(),
      pendingTask.getEnvOverrides(),
      pendingTask.getRequiredAgentAttributeOverrides(),
      pendingTask.getAllowedAgentAttributeOverrides(),
      pendingTask.getExtraArtifacts(),
      Optional.of(pendingTask.getPendingTaskId().getNextRunAt())
    );
  }

  public static List<SingularityTask> getTasks(
    Collection<SingularityTaskId> taskIds,
    Map<SingularityTaskId, SingularityTask> taskIdToTask
  ) {
    final List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      tasks.add(taskIdToTask.get(taskId));
    }

    return tasks;
  }

  public SingularityDeployResult getDeployResultWithFailures(
    SingularityRequest request,
    Optional<SingularityDeploy> deploy,
    SingularityPendingDeploy pendingDeploy,
    DeployState state,
    String message,
    Collection<SingularityTaskId> matchingTasks
  ) {
    List<SingularityDeployFailure> deployFailures = getDeployFailures(
      request,
      deploy,
      pendingDeploy,
      state,
      matchingTasks
    );
    if (deployFailures.size() == 1 && !deployFailures.get(0).getTaskId().isPresent()) { // Single non-task-specific failure should become the deploy result message (e.g. not enough resources to launch all tasks)
      return new SingularityDeployResult(
        state,
        deployFailures.get(0).getMessage(),
        pendingDeploy.getLastLoadBalancerUpdate(),
        Collections.emptyList(),
        System.currentTimeMillis()
      );
    } else {
      return new SingularityDeployResult(
        state,
        Optional.of(message),
        pendingDeploy.getLastLoadBalancerUpdate(),
        deployFailures,
        System.currentTimeMillis()
      );
    }
  }

  public List<SingularityDeployFailure> getDeployFailures(
    SingularityRequest request,
    Optional<SingularityDeploy> deploy,
    SingularityPendingDeploy pendingDeploy,
    DeployState state,
    Collection<SingularityTaskId> matchingTasks
  ) {
    List<SingularityDeployFailure> failures = new ArrayList<>();
    failures.addAll(deployHealthHelper.getTaskFailures(deploy, matchingTasks));

    if (state == DeployState.OVERDUE) {
      int targetInstances = pendingDeploy.getDeployProgress().isPresent()
        ? pendingDeploy.getDeployProgress().get().getTargetActiveInstances()
        : request.getInstancesSafe();
      if (failures.isEmpty() && matchingTasks.size() < targetInstances) {
        failures.add(
          new SingularityDeployFailure(
            SingularityDeployFailureReason.TASK_COULD_NOT_BE_SCHEDULED,
            Optional.<SingularityTaskId>empty(),
            Optional.of(
              String.format(
                "Only %s of %s tasks could be launched for deploy, there may not be enough resources to launch the remaining tasks",
                matchingTasks.size(),
                targetInstances
              )
            )
          )
        );
      }
    }

    return failures;
  }

  public static Set<SingularityTaskId> getNewInactiveDeployTasks(
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> inactiveDeployMatchingTasks
  ) {
    Set<SingularityTaskId> newInactiveDeployTasks = new HashSet<>(
      inactiveDeployMatchingTasks
    );
    if (pendingDeploy.getDeployProgress().isPresent()) {
      newInactiveDeployTasks.removeAll(
        pendingDeploy.getDeployProgress().get().getFailedDeployTasks()
      );
    }

    return newInactiveDeployTasks;
  }
}
