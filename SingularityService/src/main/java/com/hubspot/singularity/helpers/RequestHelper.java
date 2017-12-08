package com.hubspot.singularity.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskHistoryQuery;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskIdsByStatus;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UserManager;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class RequestHelper {
  private static final Logger LOG = LoggerFactory.getLogger(RequestHelper.class);

  private final RequestManager requestManager;
  private final SingularityMailer mailer;
  private final DeployManager deployManager;
  private final SingularityValidator validator;
  private final UserManager userManager;
  private final TaskManager taskManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final TaskHistoryHelper taskHistoryHelper;

  @Inject
  public RequestHelper(RequestManager requestManager,
                       SingularityMailer mailer,
                       DeployManager deployManager,
                       SingularityValidator validator,
                       UserManager userManager,
                       TaskManager taskManager,
                       SingularityDeployHealthHelper deployHealthHelper,
                       TaskHistoryHelper taskHistoryHelper) {
    this.requestManager = requestManager;
    this.mailer = mailer;
    this.deployManager = deployManager;
    this.validator = validator;
    this.userManager = userManager;
    this.taskManager = taskManager;
    this.deployHealthHelper = deployHealthHelper;
    this.taskHistoryHelper = taskHistoryHelper;
  }

  public long unpause(SingularityRequest request, Optional<String> user, Optional<String> message, Optional<Boolean> skipHealthchecks) {
    mailer.sendRequestUnpausedMail(request, user, message);

    Optional<String> maybeDeployId = deployManager.getInUseDeployId(request.getId());

    final long now = System.currentTimeMillis();

    requestManager.unpause(request, now, user, message);

    if (maybeDeployId.isPresent() && !request.isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), maybeDeployId.get(), now, user, PendingType.UNPAUSED,
          skipHealthchecks, message));
    }

    return now;
  }

  private SingularityRequestDeployHolder getDeployHolder(String requestId) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestId);

    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();

    if (requestDeployState.isPresent()) {
      if (requestDeployState.get().getActiveDeploy().isPresent()) {
        activeDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getActiveDeploy().get().getDeployId());
      }
      if (requestDeployState.get().getPendingDeploy().isPresent()) {
        pendingDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getPendingDeploy().get().getDeployId());
      }
    }

    return new SingularityRequestDeployHolder(activeDeploy, pendingDeploy);
  }

  private boolean shouldReschedule(SingularityRequest newRequest, SingularityRequest oldRequest) {
    if (newRequest.getInstancesSafe() != oldRequest.getInstancesSafe()) {
      return true;
    }
    if (newRequest.isScheduled() && oldRequest.isScheduled()) {
      if (!newRequest.getQuartzScheduleSafe().equals(oldRequest.getQuartzScheduleSafe())) {
        return true;
      }
    }

    return false;
  }

  private void checkReschedule(SingularityRequest newRequest, Optional<SingularityRequest> maybeOldRequest, Optional<String> user, long timestamp, Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<SingularityBounceRequest> maybeBounceRequest) {
    if (!maybeOldRequest.isPresent()) {
      return;
    }

    if (shouldReschedule(newRequest, maybeOldRequest.get())) {
      Optional<String> maybeDeployId = deployManager.getInUseDeployId(newRequest.getId());

      if (maybeDeployId.isPresent()) {
        if (maybeBounceRequest.isPresent()) {
          Optional<String> actionId = maybeBounceRequest.get().getActionId().or(Optional.of(UUID.randomUUID().toString()));
          Optional<Boolean> removeFromLoadBalancer = Optional.absent();
          SingularityCreateResult createResult = requestManager.createCleanupRequest(
            new SingularityRequestCleanup(user, maybeBounceRequest.get().getIncremental().or(true) ? RequestCleanupType.INCREMENTAL_BOUNCE : RequestCleanupType.BOUNCE,
              System.currentTimeMillis(), Optional.<Boolean> absent(), removeFromLoadBalancer, newRequest.getId(), Optional.of(maybeDeployId.get()), skipHealthchecks, message, actionId, maybeBounceRequest.get().getRunShellCommandBeforeKill()));

          if (createResult != SingularityCreateResult.EXISTED) {
            requestManager.bounce(newRequest, System.currentTimeMillis(), user, Optional.of("Bouncing due to bounce after scale"));
            final SingularityBounceRequest validatedBounceRequest = validator.checkBounceRequest(maybeBounceRequest.get());
            requestManager.saveExpiringObject(new SingularityExpiringBounce(newRequest.getId(), maybeDeployId.get(), user, System.currentTimeMillis(), validatedBounceRequest, actionId.get()));
          } else {
            requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), timestamp, user, PendingType.UPDATED_REQUEST,
              skipHealthchecks, message));
          }
        } else {
          requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), timestamp, user, PendingType.UPDATED_REQUEST,
            skipHealthchecks, message));
        }
      }
    }
  }

  public void updateRequest(SingularityRequest request, Optional<SingularityRequest> maybeOldRequest, RequestState requestState, Optional<RequestHistoryType> historyType,
      Optional<String> user, Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<SingularityBounceRequest> maybeBounceRequest) {
    SingularityRequestDeployHolder deployHolder = getDeployHolder(request.getId());

    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy());

    final long now = System.currentTimeMillis();

    if (requestState == RequestState.FINISHED && maybeOldRequest.isPresent() && shouldReschedule(newRequest, maybeOldRequest.get())) {
      requestState = RequestState.ACTIVE;
    }

    RequestHistoryType historyTypeToSet = null;

    if (historyType.isPresent()) {
      historyTypeToSet = historyType.get();
    } else if (maybeOldRequest.isPresent()) {
      historyTypeToSet = RequestHistoryType.UPDATED;
    } else {
      historyTypeToSet = RequestHistoryType.CREATED;
    }

    requestManager.save(newRequest, requestState, historyTypeToSet, now, user, message);

    checkReschedule(newRequest, maybeOldRequest, user, now, skipHealthchecks, message, maybeBounceRequest);
  }

  public List<SingularityRequestParent> fillDataForRequestsAndFilter(List<SingularityRequestWithState> requests,
                                                                     SingularityUser user,
                                                                     boolean filterRelevantForUser,
                                                                     boolean includeFullRequestData,
                                                                     Optional<Integer> limit,
                                                                     List<RequestType> requestTypeFilters) {
    final Map<String, Optional<SingularityTaskIdHistory>> mostRecentTasks = new ConcurrentHashMap<>();
    final Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(requests.stream().map((r) -> r.getRequest().getId()).collect(Collectors.toList()));
    final Map<String, Optional<SingularityRequestHistory>> requestIdToLastHistory;

    if (includeFullRequestData) {
      requestIdToLastHistory = requests.parallelStream()
          .collect(Collectors.toMap(
              (r) -> r.getRequest().getId(),
              (r) -> getMostRecentHistoryFromZk(r.getRequest().getId())
          ));
    } else {
      requestIdToLastHistory = Collections.emptyMap();
    }

    Optional<SingularityUserSettings> maybeUserSettings = userManager.getUserSettings(user.getId());

    return requests.parallelStream()
        .filter((request) -> {
          if (!requestTypeFilters.isEmpty() && !requestTypeFilters.contains(request.getRequest().getRequestType())) {
            return false;
          }
          if (!filterRelevantForUser || user.equals(SingularityUser.DEFAULT_USER)) {
            return true;
          }
          String requestId = request.getRequest().getId();
          if (maybeUserSettings.isPresent() && maybeUserSettings.get().getStarredRequestIds().contains(requestId)) {
            // This is a starred request for the user
            return true;
          }
          if (request.getRequest().getGroup().isPresent() && user.getGroups().contains(request.getRequest().getGroup().get())) {
            // The user is in the group for this request
            return true;
          }
          if (includeFullRequestData) {
            if (userModifiedRequestLast(requestIdToLastHistory.getOrDefault(requestId, Optional.absent()), user)) {
              return true;
            }
          }
          return userAssociatedWithDeploy(Optional.fromNullable(deployStates.get(requestId)), user);
        })
        .map((request) -> {
          Long lastActionTime = null;
          if (includeFullRequestData) {
            lastActionTime = getLastActionTimeForRequest(
                request.getRequest(),
                requestIdToLastHistory.getOrDefault(request.getRequest().getId(), Optional.absent()),
                Optional.fromNullable(deployStates.get(request.getRequest().getId())),
                mostRecentTasks.computeIfAbsent(request.getRequest().getId(), (id) -> getMostRecentTask(request.getRequest()))
            );
          } else {
            // To save on zk calls, if not returning all data, use the most recent deploy timestamps
            Optional<SingularityRequestDeployState> deployState = Optional.fromNullable(deployStates.get(request.getRequest().getId()));
            if (deployState.isPresent()) {
              if (deployState.get().getPendingDeploy().isPresent()) {
                lastActionTime = deployState.get().getPendingDeploy().get().getTimestamp();
              }
              if (deployState.get().getActiveDeploy().isPresent()) {
                lastActionTime = deployState.get().getActiveDeploy().get().getTimestamp();
              }
            }
            if (lastActionTime == null) {
              lastActionTime = 0L;
            }
          }

          return new RequestParentWithLastActionTime(request, lastActionTime, maybeUserSettings.isPresent() && maybeUserSettings.get().getStarredRequestIds().contains(request.getRequest().getId()));
        })
        .sorted() // Sorted by last action time descending, with starred requests coming first
        .limit(limit.or(requests.size()))
        .map((parentWithActionTime) -> {
          SingularityRequestWithState requestWithState = parentWithActionTime.getRequestWithState();
          if (includeFullRequestData) {
            CompletableFuture<Optional<SingularityTaskIdsByStatus>> maybeTaskIdsByStatus = CompletableFuture.supplyAsync(() -> getTaskIdsByStatusForRequest(requestWithState)).exceptionally((throwable) -> Optional.absent());
            CompletableFuture<Optional<SingularityExpiringBounce>> maybeExpiringBounce = CompletableFuture.supplyAsync(() -> requestManager.getExpiringBounce(requestWithState.getRequest().getId())).exceptionally((throwable) -> Optional.absent());
            CompletableFuture<Optional<SingularityExpiringPause>> maybeExpiringPause = CompletableFuture.supplyAsync(() -> requestManager.getExpiringPause(requestWithState.getRequest().getId())).exceptionally((throwable) -> Optional.absent());
            CompletableFuture<Optional<SingularityExpiringScale>> maybeExpiringScale = CompletableFuture.supplyAsync(() -> requestManager.getExpiringScale(requestWithState.getRequest().getId())).exceptionally((throwable) -> Optional.absent());
            CompletableFuture<Optional<SingularityExpiringSkipHealthchecks>> maybeExpiringSkipHealthchecks = CompletableFuture.supplyAsync(() -> requestManager.getExpiringSkipHealthchecks(requestWithState.getRequest().getId())).exceptionally((throwable) -> Optional.absent());
            return new SingularityRequestParent(
                requestWithState.getRequest(), requestWithState.getState(),
                Optional.fromNullable(deployStates.get(requestWithState.getRequest().getId())),
                Optional.absent(), Optional.absent(), Optional.absent(), // full deploy data not provided
                maybeExpiringBounce.join(), maybeExpiringPause.join(), maybeExpiringScale.join(), maybeExpiringSkipHealthchecks.join(),
                maybeTaskIdsByStatus.join(),
                requestIdToLastHistory.getOrDefault(requestWithState.getRequest().getId(), Optional.absent()),
                mostRecentTasks.computeIfAbsent(requestWithState.getRequest().getId(), (id) -> getMostRecentTask(requestWithState.getRequest())));
          } else {
            return new SingularityRequestParent(
                requestWithState.getRequest(), requestWithState.getState(), Optional.fromNullable(deployStates.get(requestWithState.getRequest().getId())),
                Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
          }
        })
        .collect(Collectors.toList());
  }

  public Optional<SingularityRequestHistory> getMostRecentHistoryFromZk(String requestId) {
    // Most recent history is stored in zk, don't need to check mysql
    List<SingularityRequestHistory> requestHistory = requestManager.getRequestHistory(requestId);
    return JavaUtils.getFirst(requestHistory);
  }

  public Optional<SingularityTaskIdsByStatus> getTaskIdsByStatusForRequest(String requestId) {
    Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(requestId);
    if (!requestWithState.isPresent()) {
      return Optional.absent();
    }

    return getTaskIdsByStatusForRequest(requestWithState.get());
  }

  public Optional<SingularityTaskIdHistory> getMostRecentTask(SingularityRequest request) {
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(request.getId());
    if (!activeTaskIds.isEmpty()) {
      SingularityTaskId lastTaskId = activeTaskIds.get(0);
      List<SingularityTaskHistoryUpdate> historyUpdates = taskManager.getTaskHistoryUpdates(lastTaskId);
      if (!historyUpdates.isEmpty()) {
        SingularityTaskHistoryUpdate lastUpdate = historyUpdates.get(historyUpdates.size() - 1);
        return Optional.of(new SingularityTaskIdHistory(
            lastTaskId,
            lastUpdate.getTimestamp(),
            Optional.of(lastUpdate.getTaskState()),
            Optional.absent() // runId not currently provided here, grabbing the full task data for this is a more expensive call
        ));
      }
    }
    List<SingularityTaskIdHistory> maybeRecentTasks = taskHistoryHelper.getBlendedHistory(new SingularityTaskHistoryQuery(request.getId()), 0 , 1);
    if (!maybeRecentTasks.isEmpty()) {
      return Optional.of(maybeRecentTasks.get(0));
    }
    return Optional.absent();
  }

  private Optional<SingularityTaskIdsByStatus> getTaskIdsByStatusForRequest(SingularityRequestWithState requestWithState) {
    String requestId = requestWithState.getRequest().getId();
    Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(requestId);

    List<SingularityTaskId> cleaningTaskIds = taskManager.getCleanupTaskIds().stream().filter((t) -> t.getRequestId().equals(requestId)).collect(Collectors.toList());
    List<SingularityPendingTaskId> pendingTaskIds = taskManager.getPendingTaskIdsForRequest(requestId);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(requestId);
    activeTaskIds.removeAll(cleaningTaskIds);

    List<SingularityTaskId> healthyTaskIds = new ArrayList<>();
    List<SingularityTaskId> notYetHealthyTaskIds = new ArrayList<>();
    Map<String, List<SingularityTaskId>> taskIdsByDeployId = activeTaskIds.stream().collect(Collectors.groupingBy(SingularityTaskId::getDeployId));
    for (Map.Entry<String, List<SingularityTaskId>> entry : taskIdsByDeployId.entrySet()) {
      Optional<SingularityDeploy> deploy = deployManager.getDeploy(requestId, entry.getKey());
      List<SingularityTaskId> healthyTasksIdsForDeploy = deployHealthHelper.getHealthyTasks(
          requestWithState.getRequest(),
          deploy,
          entry.getValue(),
          pendingDeploy.isPresent() && pendingDeploy.get().getDeployMarker().getDeployId().equals(entry.getKey()));
      for (SingularityTaskId taskId : entry.getValue()) {
        if (healthyTasksIdsForDeploy.contains(taskId)) {
          healthyTaskIds.add(taskId);
        } else {
          notYetHealthyTaskIds.add(taskId);
        }
      }
    }

    return Optional.of(new SingularityTaskIdsByStatus(healthyTaskIds, notYetHealthyTaskIds, pendingTaskIds, cleaningTaskIds));
  }

  private boolean userAssociatedWithDeploy(Optional<SingularityRequestDeployState> deployState, SingularityUser user) {
    return deployState.isPresent() &&
        (deployState.get().getPendingDeploy().isPresent() && userMatches(deployState.get().getPendingDeploy().get().getUser(), user) ||
            deployState.get().getActiveDeploy().isPresent() && userMatches(deployState.get().getActiveDeploy().get().getUser(), user));
  }

  private boolean userMatches(Optional<String> input, SingularityUser user) {
    return input.isPresent() &&
        (user.getEmail().equals(input) || user.getId().equals(input.get()) || user.getName().equals(input.get()));
  }

  private boolean userModifiedRequestLast(Optional<SingularityRequestHistory> lastHistory, SingularityUser user) {
    return lastHistory.isPresent() && userMatches(lastHistory.get().getUser(), user);
  }

  private long getLastActionTimeForRequest(SingularityRequest request, Optional<SingularityRequestHistory> lastHistory, Optional<SingularityRequestDeployState> deployState, Optional<SingularityTaskIdHistory> mostRecentTask) {
    long lastUpdate = 0;
    if (lastHistory.isPresent()) {
      lastUpdate = lastHistory.get().getCreatedAt();
    }
    if (deployState.isPresent()) {
      if (deployState.get().getActiveDeploy().isPresent()) {
        lastUpdate = Math.max(lastUpdate, deployState.get().getActiveDeploy().get().getTimestamp());
      }
      if (deployState.get().getPendingDeploy().isPresent()) {
        lastUpdate = Math.max(lastUpdate, deployState.get().getPendingDeploy().get().getTimestamp());
      }
    }
    // Only consider most recent task time for non-long-running
    if (mostRecentTask.isPresent() && !request.getRequestType().isLongRunning()) {
      lastUpdate = Math.max(lastUpdate, mostRecentTask.get().getUpdatedAt());
    }
    return lastUpdate;
  }
}
