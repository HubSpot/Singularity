package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDriverManager;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestLbCleanup;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper.DeployHealth;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityCleaner {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCleaner.class);

  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final SingularityDriverManager driverManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final LoadBalancerClient lbClient;
  private final SingularityExceptionNotifier exceptionNotifier;
  private final RequestHistoryHelper requestHistoryHelper;

  private final SingularityConfiguration configuration;
  private final long killNonLongRunningTasksInCleanupAfterMillis;

  @Inject
  public SingularityCleaner(TaskManager taskManager, SingularityDeployHealthHelper deployHealthHelper, DeployManager deployManager, RequestManager requestManager,
      SingularityDriverManager driverManager, SingularityConfiguration configuration, LoadBalancerClient lbClient, SingularityExceptionNotifier exceptionNotifier,
      RequestHistoryHelper requestHistoryHelper) {
    this.taskManager = taskManager;
    this.lbClient = lbClient;
    this.deployHealthHelper = deployHealthHelper;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.driverManager = driverManager;
    this.exceptionNotifier = exceptionNotifier;
    this.requestHistoryHelper = requestHistoryHelper;

    this.configuration = configuration;

    this.killNonLongRunningTasksInCleanupAfterMillis = TimeUnit.SECONDS.toMillis(configuration.getKillNonLongRunningTasksInCleanupAfterSeconds());
  }

  private boolean shouldKillTask(SingularityTaskCleanup taskCleanup, List<SingularityTaskId> activeTaskIds, List<SingularityTaskId> cleaningTasks, Multiset<SingularityDeployKey> incrementalBounceCleaningTasks) {
    final Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(taskCleanup.getTaskId().getRequestId());

    if (!requestWithState.isPresent()) {
      LOG.debug("Killing a task {} immediately because the request was missing", taskCleanup);
      return true;
    }

    final SingularityRequest request = requestWithState.get().getRequest();

    if (taskCleanup.getCleanupType().shouldKillTaskInstantly(request)) {
      LOG.debug("Killing a task {} immediately because of its cleanup type", taskCleanup);
      return true;
    }

    if (requestWithState.get().getState() == RequestState.PAUSED) {
      LOG.debug("Killing a task {} immediately because the request was paused", taskCleanup);
      return true;
    }

    if (!request.isLongRunning()) {
      final long timeSinceCleanup = System.currentTimeMillis() - taskCleanup.getTimestamp();
      final long maxWaitTime = request.getKillOldNonLongRunningTasksAfterMillis().or(killNonLongRunningTasksInCleanupAfterMillis);
      final boolean tooOld = (maxWaitTime < 1) || (timeSinceCleanup > maxWaitTime);

      if (!tooOld) {
        LOG.trace("Not killing a non-longRunning task {}, running time since cleanup {} (max wait time is {})", taskCleanup, timeSinceCleanup, maxWaitTime);
      } else {
        LOG.debug("Killing a non-longRunning task {} - running time since cleanup {} exceeded max wait time {}", taskCleanup, timeSinceCleanup, maxWaitTime);
      }

      return tooOld;
    }

    final String requestId = request.getId();

    final Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestId);

    if (taskCleanup.getCleanupType() == TaskCleanupType.DECOMISSIONING && deployState.get().getPendingDeploy().isPresent()
        && deployState.get().getPendingDeploy().get().getDeployId().equals(taskCleanup.getTaskId().getDeployId())) {
      final long timeSinceCleanup = System.currentTimeMillis() - taskCleanup.getTimestamp();
      final long maxWaitTime = configuration.getPendingDeployHoldTaskDuringDecommissionMillis();
      final boolean tooOld = (maxWaitTime < 1) || (timeSinceCleanup > maxWaitTime);

      if (!tooOld) {
        LOG.trace("Not killing {} - part of pending deploy - running time since cleanup {} (max wait time is {})", taskCleanup, timeSinceCleanup, maxWaitTime);
        return false;
      } else {
        LOG.debug("Killing {} - part of pending deploy but running time since cleanup {} exceeded max wait time {}", taskCleanup, timeSinceCleanup, maxWaitTime);
        return true;
      }
    }

    if (!deployState.isPresent() || !deployState.get().getActiveDeploy().isPresent()) {
      LOG.debug("Killing a task {} immediately because there is no active deploy state {}", taskCleanup, deployState);
      return true;
    }

    final String activeDeployId = deployState.get().getActiveDeploy().get().getDeployId();

    // check to see if there are enough active tasks out there that have been active for long enough that we can safely shut this task down.
    final List<SingularityTaskId> matchingTasks = SingularityTaskId.matchingAndNotIn(activeTaskIds, taskCleanup.getTaskId().getRequestId(), activeDeployId, cleaningTasks);

    // For an incremental bounce, shut down old tasks as new ones are started
    final SingularityDeployKey key = SingularityDeployKey.fromTaskId(taskCleanup.getTaskId());
    if (taskCleanup.getCleanupType() == TaskCleanupType.INCREMENTAL_BOUNCE) {
      int healthyReplacementTasks = getNumHealthyTasks(request, activeDeployId, matchingTasks);
      if (healthyReplacementTasks + incrementalBounceCleaningTasks.count(key) <= request.getInstancesSafe()) {
        LOG.trace("Not killing a task {} yet, only {} matching out of a required {}", taskCleanup, matchingTasks.size(), request.getInstancesSafe() - incrementalBounceCleaningTasks.count(key));
        return false;
      } else {
        LOG.debug("Killing a task {}, {} replacement tasks are healthy", taskCleanup, healthyReplacementTasks);
        incrementalBounceCleaningTasks.remove(key);
        return true;
      }
    } else {
      if (matchingTasks.size() < request.getInstancesSafe()) {
        LOG.trace("Not killing a task {} yet, only {} matching out of a required {}", taskCleanup, matchingTasks.size(), request.getInstancesSafe());
        return false;
      }
    }

    final Optional<SingularityDeploy> deploy = deployManager.getDeploy(requestId, activeDeployId);

    final DeployHealth deployHealth = deployHealthHelper.getDeployHealth(requestWithState.get().getRequest(), deploy, matchingTasks, false);

    switch (deployHealth) {
      case HEALTHY:
        for (SingularityTaskId taskId : matchingTasks) {
          DeployHealth lbHealth = getLbHealth(request, taskId);

          if (lbHealth != DeployHealth.HEALTHY) {
            LOG.trace("Not killing a task {}, waiting for new replacement tasks to be added to LB (current state: {})", taskCleanup, lbHealth);
            return false;
          }
        }

        LOG.debug("Killing a task {}, all replacement tasks are healthy", taskCleanup);
        return true;
      case WAITING:
      case UNHEALTHY:
      default:
        LOG.trace("Not killing a task {}, waiting for new replacement tasks to be healthy (current state: {})", taskCleanup, deployHealth);
        return false;
    }
  }

  private int getNumHealthyTasks(SingularityRequest request, String activeDeployId, List<SingularityTaskId> matchingTasks) {
    Optional<SingularityDeploy> deploy = deployManager.getDeploy(request.getId(), activeDeployId);

    List<SingularityTaskId> healthyTasks = deployHealthHelper.getHealthyTasks(request, deploy, matchingTasks, false);

    int numHealthyTasks = 0;

    for (SingularityTaskId taskId : healthyTasks) {
      DeployHealth lbHealth = getLbHealth(request, taskId);

      if (lbHealth == DeployHealth.HEALTHY) {
        numHealthyTasks++;
      }
    }

    return numHealthyTasks;
  }

  private DeployHealth getLbHealth(SingularityRequest request, SingularityTaskId taskId) {
    if (!request.isLoadBalanced()) {
      return DeployHealth.HEALTHY;
    }

    Optional<SingularityLoadBalancerUpdate> update = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.ADD);

    if (!update.isPresent()) {
      return DeployHealth.WAITING;
    }

    switch (update.get().getLoadBalancerState()) {
      case SUCCESS:
        return DeployHealth.HEALTHY;
      case CANCELED:
      case CANCELING:
      case UNKNOWN:
      case INVALID_REQUEST_NOOP:
      case FAILED:
        return DeployHealth.UNHEALTHY;
      case WAITING:
        return DeployHealth.WAITING;
    }

    return DeployHealth.WAITING;
  }

  private boolean isObsolete(long start, long cleanupRequest) {
    final long delta = start - cleanupRequest;

    return delta > getObsoleteExpirationTime();
  }

  private long getObsoleteExpirationTime() {
    return TimeUnit.SECONDS.toMillis(configuration.getCleanupEverySeconds()) * 3;
  }

  private void drainRequestCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityRequestCleanup> cleanupRequests = requestManager.getCleanupRequests();

    if (cleanupRequests.isEmpty()) {
      LOG.trace("Request cleanup queue is empty");
      return;
    }

    LOG.info("Cleaning up {} requests", cleanupRequests.size());

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();
    final List<SingularityPendingTask> pendingTasks = taskManager.getPendingTasks();

    int numTasksKilled = 0;
    int numScheduledTasksRemoved = 0;

    for (SingularityRequestCleanup requestCleanup : cleanupRequests) {
      final String requestId = requestCleanup.getRequestId();
      final Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(requestId);

      boolean killActiveTasks = requestCleanup.getKillTasks().or(configuration.isDefaultValueForKillTasksOfPausedRequests());
      boolean killScheduledTasks = true;

      final Iterable<SingularityTaskId> matchingActiveTaskIds = Iterables.filter(activeTaskIds, SingularityTaskId.matchingRequest(requestId));

      switch (requestCleanup.getCleanupType()) {
        case PAUSING:
          if (SingularityRequestWithState.isActive(requestWithState)) {
            if (isObsolete(start, requestCleanup.getTimestamp())) {
              killScheduledTasks = false;
              killActiveTasks = false;
              LOG.info("Ignoring {}, because {} is {}", requestCleanup, requestCleanup.getRequestId(), requestWithState.get().getState());
            } else {
              LOG.debug("Waiting on {} (it will expire after {}), because {} is {}", requestCleanup, JavaUtils.durationFromMillis(getObsoleteExpirationTime()), requestCleanup.getRequestId(), requestWithState.get().getState());
              continue;
            }
          }
          break;
        case DELETING:
          if (requestWithState.isPresent()) {
            killActiveTasks = false;
            killScheduledTasks = false;
            LOG.info("Ignoring {}, because {} still existed", requestCleanup, requestCleanup.getRequestId());
            if (requestWithState.get().getRequest().isLoadBalanced() && configuration.isDeleteRemovedRequestsFromLoadBalancer()) {
              createLbCleanupRequest(requestId, matchingActiveTaskIds);
            }
          } else {
            Optional<SingularityRequestHistory> maybeHistory = requestHistoryHelper.getLastHistory(requestId);
            if (maybeHistory.isPresent() && maybeHistory.get().getRequest().isLoadBalanced() && configuration.isDeleteRemovedRequestsFromLoadBalancer()) {
              createLbCleanupRequest(requestId, matchingActiveTaskIds);
            }
            cleanupDeployState(requestCleanup);
          }
          break;
        case BOUNCE:
        case INCREMENTAL_BOUNCE:
          killActiveTasks = false;
          killScheduledTasks = false;

          bounce(requestCleanup, activeTaskIds);
          break;
      }

      if (killActiveTasks) {
        for (SingularityTaskId matchingTaskId : matchingActiveTaskIds) {
          LOG.debug("Killing task {} due to {}", matchingTaskId, requestCleanup);
          driverManager.killAndRecord(matchingTaskId, requestCleanup.getCleanupType());
          numTasksKilled++;
        }
      } else {
        LOG.info("Active tasks for {} not killed", requestCleanup);
      }

      if (killScheduledTasks) {
        for (SingularityPendingTask matchingTask : Iterables.filter(pendingTasks, SingularityPendingTask.matchingRequest(requestId))) {
          LOG.debug("Deleting scheduled task {} due to {}", matchingTask, requestCleanup);
          taskManager.deletePendingTask(matchingTask.getPendingTaskId());
          numScheduledTasksRemoved++;
        }
      }

      requestManager.deleteCleanRequest(requestId, requestCleanup.getCleanupType());
    }

    LOG.info("Killed {} tasks (removed {} scheduled) in {}", numTasksKilled, numScheduledTasksRemoved, JavaUtils.duration(start));
  }

  private void createLbCleanupRequest(String requestId, Iterable<SingularityTaskId> matchingActiveTaskIds) {
    Optional<String> maybeCurrentDeployId = deployManager.getInUseDeployId(requestId);
    Optional<SingularityDeploy> maybeDeploy = Optional.absent();
    if (maybeCurrentDeployId.isPresent()) {
      maybeDeploy = deployManager.getDeploy(requestId, maybeCurrentDeployId.get());
      if (maybeDeploy.isPresent()) {
        List<String> taskIds = new ArrayList<>();
        for (SingularityTaskId taskId : matchingActiveTaskIds) {
          taskIds.add(taskId.getId());
        }
        requestManager.saveLbCleanupRequest(new SingularityRequestLbCleanup(requestId, maybeDeploy.get().getLoadBalancerGroups().get(), maybeDeploy.get().getServiceBasePath().get(), taskIds, Optional.<SingularityLoadBalancerUpdate>absent()));
        return;
      }
    }
    exceptionNotifier.notify("Insufficient data to create LB request cleanup", ImmutableMap.of("requestId", requestId, "deployId", maybeCurrentDeployId.toString(), "deploy", maybeDeploy.toString()));
  }

  private void bounce(SingularityRequestCleanup requestCleanup, final List<SingularityTaskId> activeTaskIds) {
    final long now = System.currentTimeMillis();

    final List<SingularityTaskId> matchingTaskIds = SingularityTaskId.matchingAndNotIn(activeTaskIds, requestCleanup.getRequestId(), requestCleanup.getDeployId().get(), Collections.<SingularityTaskId> emptyList());

    for (SingularityTaskId matchingTaskId : matchingTaskIds) {
      LOG.debug("Adding task {} to cleanup (bounce)", matchingTaskId.getId());

      taskManager.createTaskCleanup(new SingularityTaskCleanup(requestCleanup.getUser(), requestCleanup.getCleanupType().getTaskCleanupType().get(), now, matchingTaskId, requestCleanup.getMessage(), requestCleanup.getActionId()));
    }

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestCleanup.getRequestId(), requestCleanup.getDeployId().get(), requestCleanup.getTimestamp(),
        requestCleanup.getUser(), PendingType.BOUNCE, Optional.<List<String>> absent(), Optional.<String> absent(), requestCleanup.getSkipHealthchecks(), requestCleanup.getMessage(), requestCleanup.getActionId()));

    LOG.info("Added {} tasks for request {} to cleanup bounce queue in {}", matchingTaskIds.size(), requestCleanup.getRequestId(), JavaUtils.duration(now));
  }

  private void cleanupDeployState(SingularityRequestCleanup requestCleanup) {
    SingularityDeleteResult deletePendingDeployResult = deployManager.deletePendingDeploy(requestCleanup.getRequestId());
    SingularityDeleteResult deleteRequestDeployStateResult = deployManager.deleteRequestDeployState(requestCleanup.getRequestId());
    LOG.trace("Deleted pendingDeploy ({}) and requestDeployState ({}) due to {}", deletePendingDeployResult, deleteRequestDeployStateResult, requestCleanup);
  }

  public void drainCleanupQueue() {
    drainRequestCleanupQueue();
    drainTaskCleanupQueue();

    final List<SingularityTaskId> lbCleanupTasks = taskManager.getLBCleanupTasks();
    drainLBTaskCleanupQueue(lbCleanupTasks);
    drainLBRequestCleanupQueue(lbCleanupTasks);

    checkKilledTaskIdRecords();
  }

  private boolean isValidTask(SingularityTaskCleanup cleanupTask) {
    return taskManager.isActiveTask(cleanupTask.getTaskId().getId());
  }

  private void checkKilledTaskIdRecords() {
    final long start = System.currentTimeMillis();
    final List<SingularityKilledTaskIdRecord> killedTaskIdRecords = taskManager.getKilledTaskIdRecords();

    if (killedTaskIdRecords.isEmpty()) {
      LOG.trace("No killed taskId records");
      return;
    }

    int obsolete = 0;
    int waiting = 0;
    int rekilled = 0;

    for (SingularityKilledTaskIdRecord killedTaskIdRecord : killedTaskIdRecords) {
      if (!taskManager.isActiveTask(killedTaskIdRecord.getTaskId().getId())) {
        SingularityDeleteResult deleteResult = taskManager.deleteKilledRecord(killedTaskIdRecord.getTaskId());

        LOG.debug("Deleting obsolete {} - {}", killedTaskIdRecord, deleteResult);

        obsolete++;

        continue;
      }

      long duration = start - killedTaskIdRecord.getTimestamp();

      if (duration > configuration.getAskDriverToKillTasksAgainAfterMillis()) {
        LOG.info("{} is still active, and time since last kill {} is greater than configured (askDriverToKillTasksAgainAfterMillis) {} - asking driver to kill again",
            killedTaskIdRecord, JavaUtils.durationFromMillis(duration), JavaUtils.durationFromMillis(configuration.getAskDriverToKillTasksAgainAfterMillis()));

        driverManager.killAndRecord(killedTaskIdRecord.getTaskId(), killedTaskIdRecord.getRequestCleanupType(),
            killedTaskIdRecord.getTaskCleanupType(), Optional.of(killedTaskIdRecord.getOriginalTimestamp()), Optional.of(killedTaskIdRecord.getRetries()));

        rekilled++;
      } else {
        LOG.trace("Ignoring {}, because duration {} is less than configured (askDriverToKillTasksAgainAfterMillis) {}", killedTaskIdRecord, JavaUtils.durationFromMillis(duration),
            JavaUtils.durationFromMillis(configuration.getAskDriverToKillTasksAgainAfterMillis()));

        waiting++;
      }
    }

    LOG.info("{} obsolete, {} waiting, {} rekilled tasks based on {} killedTaskIdRecords", obsolete, waiting, rekilled, killedTaskIdRecords.size());
  }

  private void drainTaskCleanupQueue() {
    final long start = System.currentTimeMillis();

    final List<SingularityTaskCleanup> cleanupTasks = taskManager.getCleanupTasks();

    if (cleanupTasks.isEmpty()) {
      LOG.trace("Task cleanup queue is empty");
      return;
    }

    final Multiset<SingularityDeployKey> incrementalBounceCleaningTasks = HashMultiset.create(cleanupTasks.size());
    final Set<SingularityDeployKey> isBouncing = new HashSet<>(cleanupTasks.size());

    final List<SingularityTaskId> cleaningTasks = Lists.newArrayListWithCapacity(cleanupTasks.size());
    for (SingularityTaskCleanup cleanupTask : cleanupTasks) {
      cleaningTasks.add(cleanupTask.getTaskId());
      if (cleanupTask.getCleanupType() == TaskCleanupType.INCREMENTAL_BOUNCE) {
        incrementalBounceCleaningTasks.add(SingularityDeployKey.fromTaskId(cleanupTask.getTaskId()));
      }
      if (cleanupTask.getCleanupType() == TaskCleanupType.BOUNCING || cleanupTask.getCleanupType() == TaskCleanupType.INCREMENTAL_BOUNCE) {
        isBouncing.add(SingularityDeployKey.fromTaskId(cleanupTask.getTaskId()));
      }
    }

    LOG.info("Cleaning up {} tasks", cleanupTasks.size());

    final List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    int killedTasks = 0;

    for (SingularityTaskCleanup cleanupTask : cleanupTasks) {
      if (!isValidTask(cleanupTask)) {
        LOG.info("Couldn't find a matching active task for cleanup task {}, deleting..", cleanupTask);
        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());
      } else if (shouldKillTask(cleanupTask, activeTaskIds, cleaningTasks, incrementalBounceCleaningTasks) && checkLBStateAndShouldKillTask(cleanupTask)) {
        driverManager.killAndRecord(cleanupTask.getTaskId(), cleanupTask.getCleanupType());

        taskManager.deleteCleanupTask(cleanupTask.getTaskId().getId());

        killedTasks++;
      } else if (cleanupTask.getCleanupType() == TaskCleanupType.BOUNCING || cleanupTask.getCleanupType() == TaskCleanupType.INCREMENTAL_BOUNCE) {
        isBouncing.remove(SingularityDeployKey.fromTaskId(cleanupTask.getTaskId()));
      }
    }

    for (SingularityDeployKey bounceSucceeded : isBouncing) {
      Optional<SingularityExpiringBounce> expiringBounce = requestManager.getExpiringBounce(bounceSucceeded.getRequestId());

      if (expiringBounce.isPresent() && expiringBounce.get().getDeployId().equals(bounceSucceeded.getDeployId())) {
        LOG.info("Bounce succeeded for {}, deleting expiring bounce {}", bounceSucceeded.getRequestId(), expiringBounce.get());

        requestManager.deleteExpiringObject(SingularityExpiringBounce.class, bounceSucceeded.getRequestId());
      }
    }

    LOG.info("Killed {} tasks in {}", killedTasks, JavaUtils.duration(start));
  }

  private boolean checkLBStateAndShouldKillTask(SingularityTaskCleanup cleanupTask) {
    final long start = System.currentTimeMillis();

    CheckLBState checkLbState = checkLbState(cleanupTask.getTaskId());

    LOG.debug("TaskCleanup {} had LB state {} after {}", cleanupTask, checkLbState, JavaUtils.duration(start));

    switch (checkLbState) {
      case DONE:
      case NOT_LOAD_BALANCED:
      case MISSING_TASK:
      case LOAD_BALANCE_FAILED:
        return true;
      case RETRY:
      case WAITING:
    }

    return false;
  }

  private enum CheckLBState {
    NOT_LOAD_BALANCED, LOAD_BALANCE_FAILED, MISSING_TASK, WAITING, DONE, RETRY;
  }

  private boolean shouldRemoveLbState(SingularityTaskId taskId, SingularityLoadBalancerUpdate loadBalancerUpdate) {
    switch (loadBalancerUpdate.getLoadBalancerState()) {
      case UNKNOWN:
      case WAITING:
      case SUCCESS:
        return true;
      case INVALID_REQUEST_NOOP:
        return false;  // don't need to remove because Baragon doesnt know about it
      default:
        LOG.trace("Task {} had abnormal LB state {}", taskId, loadBalancerUpdate);
        return false;
    }
  }

  private LoadBalancerRequestId getLoadBalancerRequestId(SingularityTaskId taskId, Optional<SingularityLoadBalancerUpdate> lbRemoveUpdate) {
    if (!lbRemoveUpdate.isPresent()) {
      return new LoadBalancerRequestId(taskId.getId(), LoadBalancerRequestType.REMOVE, Optional.<Integer> absent());
    }

    switch (lbRemoveUpdate.get().getLoadBalancerState()) {
      case FAILED:
      case CANCELED:
        return new LoadBalancerRequestId(taskId.getId(), LoadBalancerRequestType.REMOVE, Optional.of(lbRemoveUpdate.get().getLoadBalancerRequestId().getAttemptNumber() + 1));
      default:
        return lbRemoveUpdate.get().getLoadBalancerRequestId();
    }
  }

  private boolean shouldEnqueueLbRequest(Optional<SingularityLoadBalancerUpdate> maybeLbUpdate) {
    if (!maybeLbUpdate.isPresent()) {
      return true;
    }

    switch (maybeLbUpdate.get().getLoadBalancerState()) {
      case UNKNOWN:
      case FAILED:
      case CANCELED:
        return true;
      case CANCELING:
      case SUCCESS:
      case WAITING:
      case INVALID_REQUEST_NOOP:
    }

    return false;
  }

  private CheckLBState checkLbState(SingularityTaskId taskId) {
    Optional<SingularityLoadBalancerUpdate> lbAddUpdate = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.ADD);

    if (!lbAddUpdate.isPresent()) {
      return CheckLBState.NOT_LOAD_BALANCED;
    }

    if (!shouldRemoveLbState(taskId, lbAddUpdate.get())) {
      return CheckLBState.LOAD_BALANCE_FAILED;
    }

    Optional<SingularityLoadBalancerUpdate> maybeLbRemoveUpdate = taskManager.getLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE);
    SingularityLoadBalancerUpdate lbRemoveUpdate = null;

    final LoadBalancerRequestId loadBalancerRequestId = getLoadBalancerRequestId(taskId, maybeLbRemoveUpdate);

    if (shouldEnqueueLbRequest(maybeLbRemoveUpdate)) {
      final Optional<SingularityTask> task = taskManager.getTask(taskId);

      if (!task.isPresent()) {
        LOG.error("Missing task {}", taskId);
        return CheckLBState.MISSING_TASK;
      }

      lbRemoveUpdate = lbClient.enqueue(loadBalancerRequestId, task.get().getTaskRequest().getRequest(), task.get().getTaskRequest().getDeploy(), Collections.<SingularityTask>emptyList(), Collections.singletonList(task.get()));

      taskManager.saveLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE, lbRemoveUpdate);
    } else if (maybeLbRemoveUpdate.get().getLoadBalancerState() == BaragonRequestState.WAITING || maybeLbRemoveUpdate.get().getLoadBalancerState() == BaragonRequestState.CANCELING) {
      lbRemoveUpdate = lbClient.getState(loadBalancerRequestId);

      taskManager.saveLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE, lbRemoveUpdate);
    } else {
      lbRemoveUpdate = maybeLbRemoveUpdate.get();
    }

    switch (lbRemoveUpdate.getLoadBalancerState()) {
      case SUCCESS:
        if (configuration.getLoadBalancerRemovalGracePeriodMillis() > 0) {
          final long duration = System.currentTimeMillis() - lbRemoveUpdate.getTimestamp();

          if (duration < configuration.getLoadBalancerRemovalGracePeriodMillis()) {
            LOG.trace("LB removal for {} succeeded - waiting at least {} to kill task (current duration {})", taskId,
                JavaUtils.durationFromMillis(configuration.getLoadBalancerRemovalGracePeriodMillis()), JavaUtils.durationFromMillis(duration));
            return CheckLBState.WAITING;
          }
        }

        return CheckLBState.DONE;
      case FAILED:
      case CANCELED:
        LOG.error("LB removal request {} ({}) got unexpected response {}", lbRemoveUpdate, loadBalancerRequestId, lbRemoveUpdate.getLoadBalancerState());
        exceptionNotifier.notify("LB removal failed", ImmutableMap.of("state", lbRemoveUpdate.getLoadBalancerState().name(), "loadBalancerRequestId", loadBalancerRequestId.toString(), "addUpdate", lbRemoveUpdate.toString()));
        return CheckLBState.RETRY;
      case UNKNOWN:
      case CANCELING:
      case WAITING:
        LOG.trace("Waiting on LB cleanup request {} in state {}", loadBalancerRequestId, lbRemoveUpdate.getLoadBalancerState());
        break;
      case INVALID_REQUEST_NOOP:
        exceptionNotifier.notify("LB removal failed", ImmutableMap.of("state", lbRemoveUpdate.getLoadBalancerState().name(), "loadBalancerRequestId", loadBalancerRequestId.toString(), "addUpdate", lbRemoveUpdate.toString()));
        return CheckLBState.LOAD_BALANCE_FAILED;
    }

    return CheckLBState.WAITING;
  }

  private void drainLBTaskCleanupQueue(List<SingularityTaskId> lbCleanupTasks) {
    final long start = System.currentTimeMillis();

    if (lbCleanupTasks.isEmpty()) {
      LOG.trace("LB task cleanup queue is empty");
      return;
    }

    LOG.info("LB task cleanup queue had {} tasks", lbCleanupTasks.size());

    int cleanedTasks = 0;
    int ignoredTasks = 0;

    for (SingularityTaskId taskId : lbCleanupTasks) {
      final long checkStart = System.currentTimeMillis();

      final CheckLBState checkLbState = checkLbState(taskId);

      LOG.debug("LB cleanup for task {} had state {} after {}", taskId, checkLbState, JavaUtils.duration(checkStart));

      switch (checkLbState) {
        case WAITING:
        case RETRY:
          continue;
        case DONE:
        case MISSING_TASK:
          cleanedTasks++;
          break;
        case NOT_LOAD_BALANCED:
        case LOAD_BALANCE_FAILED:
          ignoredTasks++;
      }

      taskManager.deleteLBCleanupTask(taskId);
    }

    LOG.info("LB cleaned {} tasks ({} left, {} obsolete) in {}", cleanedTasks, lbCleanupTasks.size() - (ignoredTasks + cleanedTasks), ignoredTasks, JavaUtils.duration(start));
  }

  private void drainLBRequestCleanupQueue(List<SingularityTaskId> lbCleanupTasks) {
    final long start = System.currentTimeMillis();

    final List<SingularityRequestLbCleanup> lbCleanupRequests = requestManager.getLbCleanupRequests();

    if (lbCleanupRequests.isEmpty()) {
      LOG.trace("LB request cleanup queue is empty");
      return;
    }

    LOG.info("LB request cleanup queue had {} requests", lbCleanupRequests.size());

    int cleanedRequests = 0;
    int ignoredRequests = 0;

    for (SingularityRequestLbCleanup cleanup : lbCleanupRequests) {
      final long checkStart = System.currentTimeMillis();

      final CheckLBState checkLbState = checkRequestLbState(cleanup, lbCleanupTasks);

      LOG.debug("LB cleanup for request {} had state {} after {}", cleanup.getRequestId(), checkLbState, JavaUtils.duration(checkStart));

      switch (checkLbState) {
        case WAITING:
        case RETRY:
          continue;
        case DONE:
        case MISSING_TASK:
          cleanedRequests++;
          break;
        case NOT_LOAD_BALANCED:
        case LOAD_BALANCE_FAILED:
          ignoredRequests++;
      }

      requestManager.deleteLbCleanupRequest(cleanup.getRequestId());
    }
    LOG.info("LB cleaned {} requests ({} left, {} obsolete) in {}", cleanedRequests, lbCleanupRequests.size() - (ignoredRequests + cleanedRequests), ignoredRequests, JavaUtils.duration(start));

  }

  private boolean  canRunRequestLbCleanup(SingularityRequestLbCleanup cleanup, List<SingularityTaskId> lbCleanupTasks) {
    Optional<SingularityRequestWithState> maybeRequestWithState = requestManager.getRequest(cleanup.getRequestId());
    if (maybeRequestWithState.isPresent() && SingularityRequestWithState.isActive(maybeRequestWithState)) {
      LOG.trace("Request is still active, will wait for request lb cleanup");
      return false;
    }
    for (String taskId : cleanup.getActiveTaskIds()) {
      if (taskManager.isActiveTask(taskId)) {
        LOG.trace("Request still has active tasks, will wait for lb request cleanup");
        return false;
      }
    }
    for (SingularityTaskId taskId : lbCleanupTasks) {
      if (taskId.getRequestId().equals(cleanup.getRequestId())) {
        LOG.trace("Waiting for task lb cleanup to finish before trying request lb cleanup for request {}", cleanup.getRequestId());
        return false;
      }
    }
    return true;
  }

  private CheckLBState checkRequestLbState(SingularityRequestLbCleanup cleanup, List<SingularityTaskId> lbCleanupTasks) {
    if (!canRunRequestLbCleanup(cleanup , lbCleanupTasks)) {
      return CheckLBState.RETRY;
    }

    Optional<SingularityLoadBalancerUpdate> maybeDeleteUpdate = cleanup.getLoadBalancerUpdate();
    final LoadBalancerRequestId loadBalancerRequestId = getLoadBalancerRequestId(cleanup.getRequestId(), maybeDeleteUpdate);
    SingularityLoadBalancerUpdate lbDeleteUpdate;
    if (shouldEnqueueLbRequest(maybeDeleteUpdate)) {
      lbDeleteUpdate = lbClient.delete(loadBalancerRequestId, cleanup.getRequestId(), cleanup.getLoadBalancerGroups(), cleanup.getServiceBasePath());
      cleanup.setLoadBalancerUpdate(Optional.of(lbDeleteUpdate));
      requestManager.saveLbCleanupRequest(cleanup);
    } else if (maybeDeleteUpdate.get().getLoadBalancerState() == BaragonRequestState.WAITING || maybeDeleteUpdate.get().getLoadBalancerState() == BaragonRequestState.CANCELING) {
      lbDeleteUpdate = lbClient.getState(loadBalancerRequestId);
      cleanup.setLoadBalancerUpdate(Optional.of(lbDeleteUpdate));
      requestManager.saveLbCleanupRequest(cleanup);
    } else {
      lbDeleteUpdate = maybeDeleteUpdate.get();
    }

    switch (lbDeleteUpdate.getLoadBalancerState()) {
      case SUCCESS:
        return CheckLBState.DONE;
      case FAILED:
      case CANCELED:
        LOG.error("LB delete request {} ({}) got unexpected response {}", lbDeleteUpdate, loadBalancerRequestId, lbDeleteUpdate.getLoadBalancerState());
        exceptionNotifier.notify(String.format("LB delete failed for %s", lbDeleteUpdate.getLoadBalancerRequestId().toString()),
            ImmutableMap.of("state", lbDeleteUpdate.getLoadBalancerState().name(), "loadBalancerRequestId", loadBalancerRequestId.toString(), "addUpdate", lbDeleteUpdate.toString()));
        return CheckLBState.RETRY;
      case UNKNOWN:
      case CANCELING:
      case WAITING:
        LOG.trace("Waiting on LB delete request {} in state {}", loadBalancerRequestId, lbDeleteUpdate.getLoadBalancerState());
        break;
      case INVALID_REQUEST_NOOP:
        exceptionNotifier.notify(String.format("LB delete failed for %s", lbDeleteUpdate.getLoadBalancerRequestId().toString()),
            ImmutableMap.of("state", lbDeleteUpdate.getLoadBalancerState().name(), "loadBalancerRequestId", loadBalancerRequestId.toString(), "addUpdate", lbDeleteUpdate.toString()));
        return CheckLBState.LOAD_BALANCE_FAILED;
    }

    return CheckLBState.WAITING;
  }

  private LoadBalancerRequestId getLoadBalancerRequestId(String requestId, Optional<SingularityLoadBalancerUpdate> lbDeleteUpdate) {
    if (!lbDeleteUpdate.isPresent()) {
      return new LoadBalancerRequestId(String.format("%s-%s", requestId, System.currentTimeMillis()), LoadBalancerRequestType.DELETE, Optional.<Integer>absent());
    }

    switch (lbDeleteUpdate.get().getLoadBalancerState()) {
      case FAILED:
      case CANCELED:
        return new LoadBalancerRequestId(String.format("%s-%s", requestId, System.currentTimeMillis()), LoadBalancerRequestType.DELETE, Optional.of(lbDeleteUpdate.get().getLoadBalancerRequestId().getAttemptNumber() + 1));
      default:
        return lbDeleteUpdate.get().getLoadBalancerRequestId();
    }
  }

}
