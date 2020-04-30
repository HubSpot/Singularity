package com.hubspot.singularity.scheduler;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.ScheduleType;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployStatisticsBuilder;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequestId;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.TaskFailureEvent;
import com.hubspot.singularity.TaskFailureType;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.helpers.RFC5545Schedule;
import com.hubspot.singularity.helpers.RebalancingHelper;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerClient;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.hubspot.singularity.smtp.SingularityMailer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.apache.mesos.v1.scheduler.Protos.Call.Reconcile.Task;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.quartz.CronExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityScheduler.class);

  private final SingularityConfiguration configuration;
  private final SingularityCrashLoops crashLoops;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final TaskRequestManager taskRequestManager;
  private final DeployManager deployManager;
  private final SlaveManager slaveManager;
  private final RebalancingHelper rebalancingHelper;
  private final RackManager rackManager;
  private final SingularityMailer mailer;
  private final SingularityLeaderCache leaderCache;
  private final SingularitySchedulerLock lock;
  private final ExecutorService schedulerExecutorService;
  private final SingularityMesosSchedulerClient mesosSchedulerClient;

  private final Map<SingularityTaskId, Long> requestedReconciles;

  @Inject
  public SingularityScheduler(
    TaskRequestManager taskRequestManager,
    SingularityConfiguration configuration,
    SingularityCrashLoops crashLoops,
    DeployManager deployManager,
    TaskManager taskManager,
    RequestManager requestManager,
    SlaveManager slaveManager,
    RebalancingHelper rebalancingHelper,
    RackManager rackManager,
    SingularityMailer mailer,
    SingularityLeaderCache leaderCache,
    SingularitySchedulerLock lock,
    SingularityManagedThreadPoolFactory threadPoolFactory,
    SingularityMesosSchedulerClient mesosSchedulerClient
  ) {
    this.taskRequestManager = taskRequestManager;
    this.configuration = configuration;
    this.deployManager = deployManager;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
    this.rebalancingHelper = rebalancingHelper;
    this.rackManager = rackManager;
    this.mailer = mailer;
    this.crashLoops = crashLoops;
    this.leaderCache = leaderCache;
    this.lock = lock;
    this.schedulerExecutorService =
      threadPoolFactory.get("scheduler", configuration.getCoreThreadpoolSize());
    this.mesosSchedulerClient = mesosSchedulerClient;
    this.requestedReconciles = new HashMap<>();
  }

  private void cleanupTaskDueToDecomission(
    final Map<String, Optional<String>> requestIdsToUserToReschedule,
    final Set<SingularityTaskId> matchingTaskIds,
    SingularityTask task,
    SingularityMachineAbstraction<?> decommissioningObject
  ) {
    requestIdsToUserToReschedule.put(
      task.getTaskRequest().getRequest().getId(),
      decommissioningObject.getCurrentState().getUser()
    );

    matchingTaskIds.add(task.getTaskId());

    LOG.trace(
      "Scheduling a cleanup task for {} due to decomissioning {}",
      task.getTaskId(),
      decommissioningObject
    );

    taskManager.createTaskCleanup(
      new SingularityTaskCleanup(
        decommissioningObject.getCurrentState().getUser(),
        TaskCleanupType.DECOMISSIONING,
        System.currentTimeMillis(),
        task.getTaskId(),
        Optional.of(
          String.format(
            "%s %s is decomissioning",
            decommissioningObject.getTypeName(),
            decommissioningObject.getName()
          )
        ),
        Optional.<String>empty(),
        Optional.<SingularityTaskShellCommandRequestId>empty()
      )
    );
  }

  private <T extends SingularityMachineAbstraction<T>> Map<T, MachineState> getDefaultMap(
    List<T> objects
  ) {
    Map<T, MachineState> map = Maps.newHashMapWithExpectedSize(objects.size());
    for (T object : objects) {
      map.put(object, MachineState.DECOMMISSIONING);
    }
    return map;
  }

  @Timed
  public void checkForDecomissions() {
    final long start = System.currentTimeMillis();

    final Map<String, Optional<String>> requestIdsToUserToReschedule = Maps.newHashMap();
    final Set<SingularityTaskId> matchingTaskIds = Sets.newHashSet();

    final Collection<SingularityTaskId> activeTaskIds = leaderCache.getActiveTaskIds();

    final Map<SingularitySlave, MachineState> slaves = getDefaultMap(
      slaveManager.getObjectsFiltered(MachineState.STARTING_DECOMMISSION)
    );

    for (SingularitySlave slave : slaves.keySet()) {
      boolean foundTask = false;

      for (SingularityTask activeTask : taskManager.getTasksOnSlave(
        activeTaskIds,
        slave
      )) {
        cleanupTaskDueToDecomission(
          requestIdsToUserToReschedule,
          matchingTaskIds,
          activeTask,
          slave
        );
        foundTask = true;
      }

      if (!foundTask) {
        slaves.put(slave, MachineState.DECOMMISSIONED);
      }
    }

    final Map<SingularityRack, MachineState> racks = getDefaultMap(
      rackManager.getObjectsFiltered(MachineState.STARTING_DECOMMISSION)
    );

    for (SingularityRack rack : racks.keySet()) {
      final String sanitizedRackId = JavaUtils.getReplaceHyphensWithUnderscores(
        rack.getId()
      );

      boolean foundTask = false;

      for (SingularityTaskId activeTaskId : activeTaskIds) {
        if (sanitizedRackId.equals(activeTaskId.getSanitizedRackId())) {
          foundTask = true;
        }

        if (matchingTaskIds.contains(activeTaskId)) {
          continue;
        }

        if (sanitizedRackId.equals(activeTaskId.getSanitizedRackId())) {
          Optional<SingularityTask> maybeTask = taskManager.getTask(activeTaskId);
          cleanupTaskDueToDecomission(
            requestIdsToUserToReschedule,
            matchingTaskIds,
            maybeTask.get(),
            rack
          );
        }
      }

      if (!foundTask) {
        racks.put(rack, MachineState.DECOMMISSIONED);
      }
    }

    for (Entry<String, Optional<String>> requestIdAndUser : requestIdsToUserToReschedule.entrySet()) {
      final String requestId = requestIdAndUser.getKey();

      LOG.trace("Rescheduling request {} due to decomissions", requestId);

      Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

      if (maybeDeployId.isPresent()) {
        requestManager.addToPendingQueue(
          new SingularityPendingRequest(
            requestId,
            maybeDeployId.get(),
            start,
            requestIdAndUser.getValue(),
            PendingType.DECOMISSIONED_SLAVE_OR_RACK,
            Optional.<Boolean>empty(),
            Optional.<String>empty()
          )
        );
      } else {
        LOG.warn(
          "Not rescheduling a request ({}) because of no active deploy",
          requestId
        );
      }
    }

    changeState(slaves, slaveManager);
    changeState(racks, rackManager);

    if (
      slaves.isEmpty() &&
      racks.isEmpty() &&
      requestIdsToUserToReschedule.isEmpty() &&
      matchingTaskIds.isEmpty()
    ) {
      LOG.trace("Decomission check found nothing");
    } else {
      LOG.info(
        "Found {} decomissioning slaves, {} decomissioning racks, rescheduling {} requests and scheduling {} tasks for cleanup in {}",
        slaves.size(),
        racks.size(),
        requestIdsToUserToReschedule.size(),
        matchingTaskIds.size(),
        JavaUtils.duration(start)
      );
    }
  }

  private <T extends SingularityMachineAbstraction<T>> void changeState(
    Map<T, MachineState> map,
    AbstractMachineManager<T> manager
  ) {
    for (Entry<T, MachineState> entry : map.entrySet()) {
      manager.changeState(
        entry.getKey().getId(),
        entry.getValue(),
        entry.getKey().getCurrentState().getMessage(),
        entry.getKey().getCurrentState().getUser()
      );
    }
  }

  @Timed
  public void drainPendingQueue() {
    final long start = System.currentTimeMillis();
    final ImmutableList<SingularityPendingRequest> pendingRequests = ImmutableList.copyOf(
      requestManager.getPendingRequests()
    );

    if (pendingRequests.isEmpty()) {
      LOG.trace("Pending queue was empty");
      return;
    }

    LOG.info("Pending queue had {} requests", pendingRequests.size());

    Map<SingularityDeployKey, List<SingularityPendingRequest>> deployKeyToPendingRequests = pendingRequests
      .stream()
      .collect(
        Collectors.groupingBy(
          request ->
            new SingularityDeployKey(request.getRequestId(), request.getDeployId())
        )
      );

    AtomicInteger totalNewScheduledTasks = new AtomicInteger(0);
    AtomicInteger heldForScheduledActiveTask = new AtomicInteger(0);
    AtomicInteger obsoleteRequests = new AtomicInteger(0);

    List<CompletableFuture<Void>> checkFutures = deployKeyToPendingRequests
      .entrySet()
      .stream()
      .map(
        e ->
          CompletableFuture.runAsync(
            () ->
              lock.runWithRequestLock(
                () ->
                  handlePendingRequestsForDeployKey(
                    obsoleteRequests,
                    heldForScheduledActiveTask,
                    totalNewScheduledTasks,
                    e.getKey(),
                    e.getValue()
                  ),
                e.getKey().getRequestId(),
                String.format("%s#%s", getClass().getSimpleName(), "drainPendingQueue")
              ),
            schedulerExecutorService
          )
      )
      .collect(Collectors.toList());
    CompletableFutures.allOf(checkFutures).join();

    LOG.info(
      "Scheduled {} new tasks ({} obsolete requests, {} held) in {}",
      totalNewScheduledTasks.get(),
      obsoleteRequests.get(),
      heldForScheduledActiveTask.get(),
      JavaUtils.duration(start)
    );
  }

  private void handlePendingRequestsForDeployKey(
    AtomicInteger obsoleteRequests,
    AtomicInteger heldForScheduledActiveTask,
    AtomicInteger totalNewScheduledTasks,
    SingularityDeployKey deployKey,
    List<SingularityPendingRequest> pendingRequestsForDeploy
  ) {
    final String requestId = deployKey.getRequestId();
    final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(
      requestId
    );
    final SingularityDeployStatistics deployStatistics = getDeployStatistics(
      deployKey.getRequestId(),
      deployKey.getDeployId()
    );

    if (!isRequestActive(maybeRequest)) {
      LOG.debug(
        "Pending request {} was obsolete (request {})",
        requestId,
        SingularityRequestWithState.getRequestState(maybeRequest)
      );
      obsoleteRequests.getAndIncrement();
      for (SingularityPendingRequest pendingRequest : pendingRequestsForDeploy) {
        requestManager.deletePendingRequest(pendingRequest);
      }
      return;
    }

    SingularityRequestWithState request = maybeRequest.get();

    Optional<SingularityRequestDeployState> maybeRequestDeployState = deployManager.getRequestDeployState(
      requestId
    );
    Optional<SingularityPendingDeploy> maybePendingDeploy = deployManager.getPendingDeploy(
      requestId
    );
    List<SingularityTaskId> matchingTaskIds = getMatchingTaskIds(
      request.getRequest(),
      deployKey
    );

    List<SingularityPendingRequest> effectivePendingRequests = new ArrayList<>();

    // Things that are closest to now (ie smaller timestamps) should come first in the queue
    pendingRequestsForDeploy.sort(
      Comparator.comparingLong(SingularityPendingRequest::getTimestamp)
    );
    int scheduledTasks = 0;
    for (SingularityPendingRequest pendingRequest : pendingRequestsForDeploy) {
      final SingularityRequest updatedRequest = updatedRequest(
        maybePendingDeploy,
        pendingRequest,
        request
      );

      if (
        !shouldScheduleTasks(
          updatedRequest,
          pendingRequest,
          maybePendingDeploy,
          maybeRequestDeployState
        )
      ) {
        LOG.debug(
          "Pending request {} was obsolete (request {})",
          pendingRequest,
          SingularityRequestWithState.getRequestState(maybeRequest)
        );
        obsoleteRequests.getAndIncrement();
        requestManager.deletePendingRequest(pendingRequest);
        continue;
      }

      int missingInstances = getNumMissingInstances(
        matchingTaskIds,
        updatedRequest,
        pendingRequest,
        maybePendingDeploy
      );
      if (
        missingInstances == 0 &&
        !matchingTaskIds.isEmpty() &&
        updatedRequest.isScheduled() &&
        pendingRequest.getPendingType() == PendingType.NEW_DEPLOY
      ) {
        LOG.trace(
          "Holding pending request {} because it is scheduled and has an active task",
          pendingRequest
        );
        heldForScheduledActiveTask.getAndIncrement();
        continue;
      }

      if (effectivePendingRequests.isEmpty()) {
        effectivePendingRequests.add(pendingRequest);
        RequestState requestState = checkCooldown(
          request.getState(),
          request.getRequest(),
          deployStatistics
        );
        scheduledTasks +=
          scheduleTasks(
            request.getRequest(),
            requestState,
            pendingRequest,
            matchingTaskIds,
            maybePendingDeploy
          );
        requestManager.deletePendingRequest(pendingRequest);
      } else if (pendingRequest.getPendingType() == PendingType.IMMEDIATE) {
        effectivePendingRequests.add(pendingRequest);
        RequestState requestState = checkCooldown(
          request.getState(),
          request.getRequest(),
          deployStatistics
        );
        scheduledTasks +=
          scheduleTasks(
            request.getRequest(),
            requestState,
            pendingRequest,
            matchingTaskIds,
            maybePendingDeploy
          );
        requestManager.deletePendingRequest(pendingRequest);
      } else if (pendingRequest.getPendingType() == PendingType.ONEOFF) {
        effectivePendingRequests.add(pendingRequest);
        RequestState requestState = checkCooldown(
          request.getState(),
          request.getRequest(),
          deployStatistics
        );
        scheduledTasks +=
          scheduleTasks(
            request.getRequest(),
            requestState,
            pendingRequest,
            matchingTaskIds,
            maybePendingDeploy
          );
        requestManager.deletePendingRequest(pendingRequest);
      } else if (
        updatedRequest.isScheduled() &&
        (
          pendingRequest.getPendingType() == PendingType.NEW_DEPLOY ||
          pendingRequest.getPendingType() == PendingType.TASK_DONE
        )
      ) {
        // If we are here, there is already an immediate of run of the scheduled task launched. Drop anything that would
        // leave a second instance of the request in the pending queue.
        requestManager.deletePendingRequest(pendingRequest);
      }
      // Any other subsequent requests are not honored until after the pending queue is cleared.
    }

    totalNewScheduledTasks.getAndAdd(scheduledTasks);
  }

  private RequestState checkCooldown(
    RequestState requestState,
    SingularityRequest request,
    SingularityDeployStatistics deployStatistics
  ) {
    if (requestState != RequestState.SYSTEM_COOLDOWN) {
      return requestState;
    }

    if (crashLoops.hasCooldownExpired(deployStatistics, Optional.empty())) {
      requestManager.exitCooldown(
        request,
        System.currentTimeMillis(),
        Optional.empty(),
        Optional.empty()
      );
      return RequestState.ACTIVE;
    }

    return requestState;
  }

  private boolean shouldScheduleTasks(
    SingularityRequest request,
    SingularityPendingRequest pendingRequest,
    Optional<SingularityPendingDeploy> maybePendingDeploy,
    Optional<SingularityRequestDeployState> maybeRequestDeployState
  ) {
    if (
      request.isDeployable() &&
      pendingRequest.getPendingType() == PendingType.NEW_DEPLOY &&
      !maybePendingDeploy.isPresent()
    ) {
      return false;
    }
    if (
      request.getRequestType() == RequestType.RUN_ONCE &&
      pendingRequest.getPendingType() == PendingType.NEW_DEPLOY
    ) {
      return true;
    }

    return isDeployInUse(maybeRequestDeployState, pendingRequest.getDeployId(), false);
  }

  @Timed
  public List<SingularityTaskRequest> getDueTasks() {
    final List<SingularityPendingTask> tasks = taskManager.getPendingTasks();

    final long now = System.currentTimeMillis();

    final List<SingularityPendingTask> dueTasks = Lists.newArrayListWithCapacity(
      tasks.size()
    );

    for (SingularityPendingTask task : tasks) {
      if (task.getPendingTaskId().getNextRunAt() <= now) {
        dueTasks.add(task);
      }
    }

    final List<SingularityTaskRequest> dueTaskRequests = taskRequestManager.getTaskRequests(
      dueTasks
    );

    return checkForStaleScheduledTasks(dueTasks, dueTaskRequests);
  }

  private List<SingularityTaskRequest> checkForStaleScheduledTasks(
    List<SingularityPendingTask> pendingTasks,
    List<SingularityTaskRequest> taskRequests
  ) {
    final Set<String> foundPendingTaskId = Sets.newHashSetWithExpectedSize(
      taskRequests.size()
    );
    final Set<String> requestIds = Sets.newHashSetWithExpectedSize(taskRequests.size());

    for (SingularityTaskRequest taskRequest : taskRequests) {
      foundPendingTaskId.add(taskRequest.getPendingTask().getPendingTaskId().getId());
      requestIds.add(taskRequest.getRequest().getId());
    }

    for (SingularityPendingTask pendingTask : pendingTasks) {
      if (!foundPendingTaskId.contains(pendingTask.getPendingTaskId().getId())) {
        LOG.info("Removing stale pending task {}", pendingTask.getPendingTaskId());
        taskManager.deletePendingTask(pendingTask.getPendingTaskId());
      }
    }

    // TODO this check isn't necessary if we keep track better during deploys
    final Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(
      requestIds
    );
    final List<SingularityTaskRequest> taskRequestsWithValidDeploys = Lists.newArrayListWithCapacity(
      taskRequests.size()
    );

    for (SingularityTaskRequest taskRequest : taskRequests) {
      SingularityRequestDeployState requestDeployState = deployStates.get(
        taskRequest.getRequest().getId()
      );

      if (
        !matchesDeploy(requestDeployState, taskRequest) &&
        !(taskRequest.getRequest().getRequestType() == RequestType.RUN_ONCE)
      ) {
        LOG.info(
          "Removing stale pending task {} because the deployId did not match active/pending deploys {}",
          taskRequest.getPendingTask().getPendingTaskId(),
          requestDeployState
        );
        taskManager.deletePendingTask(taskRequest.getPendingTask().getPendingTaskId());
      } else {
        taskRequestsWithValidDeploys.add(taskRequest);
      }
    }

    return taskRequestsWithValidDeploys;
  }

  private boolean matchesDeploy(
    SingularityRequestDeployState requestDeployState,
    SingularityTaskRequest taskRequest
  ) {
    if (requestDeployState == null) {
      return false;
    }
    return (
      matchesDeployMarker(
        requestDeployState.getActiveDeploy(),
        taskRequest.getDeploy().getId()
      ) ||
      matchesDeployMarker(
        requestDeployState.getPendingDeploy(),
        taskRequest.getDeploy().getId()
      )
    );
  }

  private boolean matchesDeployMarker(
    Optional<SingularityDeployMarker> deployMarker,
    String deployId
  ) {
    return deployMarker.isPresent() && deployMarker.get().getDeployId().equals(deployId);
  }

  private void deleteScheduledTasks(
    final Collection<SingularityPendingTask> scheduledTasks,
    SingularityPendingRequest pendingRequest
  ) {
    List<SingularityPendingTask> tasksForDeploy = scheduledTasks
      .stream()
      .filter(
        task ->
          pendingRequest.getRequestId().equals(task.getPendingTaskId().getRequestId())
      )
      .filter(
        task -> pendingRequest.getDeployId().equals(task.getPendingTaskId().getDeployId())
      )
      .collect(Collectors.toList());

    for (SingularityPendingTask task : tasksForDeploy) {
      LOG.debug(
        "Deleting pending task {} in order to reschedule {}",
        task.getPendingTaskId().getId(),
        pendingRequest
      );
      taskManager.deletePendingTask(task.getPendingTaskId());
    }
  }

  private List<SingularityTaskId> getMatchingTaskIds(
    SingularityRequest request,
    SingularityDeployKey deployKey
  ) {
    List<SingularityTaskId> activeTaskIdsForRequest = leaderCache.getActiveTaskIdsForRequest(
      deployKey.getRequestId()
    );
    if (request.isLongRunning()) {
      Set<SingularityTaskId> killedTaskIds = leaderCache
        .getKilledTasks()
        .stream()
        .map(SingularityKilledTaskIdRecord::getTaskId)
        .collect(Collectors.toSet());

      List<SingularityTaskId> matchingTaskIds = new ArrayList<>();
      for (SingularityTaskId taskId : activeTaskIdsForRequest) {
        if (!taskId.getDeployId().equals(deployKey.getDeployId())) {
          continue;
        }
        if (leaderCache.getCleanupTaskIds().contains(taskId)) {
          continue;
        }
        if (killedTaskIds.contains(taskId)) {
          continue;
        }
        matchingTaskIds.add(taskId);
      }
      return matchingTaskIds;
    } else {
      return new ArrayList<>(activeTaskIdsForRequest);
    }
  }

  private int scheduleTasks(
    SingularityRequest request,
    RequestState state,
    SingularityPendingRequest pendingRequest,
    List<SingularityTaskId> matchingTaskIds,
    Optional<SingularityPendingDeploy> maybePendingDeploy
  ) {
    if (request.getRequestType() != RequestType.ON_DEMAND) {
      deleteScheduledTasks(leaderCache.getPendingTasks(), pendingRequest);
    }

    final int numMissingInstances = getNumMissingInstances(
      matchingTaskIds,
      request,
      pendingRequest,
      maybePendingDeploy
    );

    LOG.debug(
      "Missing {} instances of request {} (matching tasks: {}), pending request: {}, pending deploy: {}",
      numMissingInstances,
      request.getId(),
      matchingTaskIds.size() < 20 ? matchingTaskIds : matchingTaskIds.size(),
      pendingRequest,
      maybePendingDeploy
    );

    if (numMissingInstances > 0) {
      schedule(numMissingInstances, matchingTaskIds, request, state, pendingRequest);
    } else if (numMissingInstances < 0) {
      final long now = System.currentTimeMillis();

      if (
        maybePendingDeploy.isPresent() &&
        maybePendingDeploy.get().getDeployProgress().isPresent()
      ) {
        Collections.sort(matchingTaskIds, SingularityTaskId.INSTANCE_NO_COMPARATOR); // For deploy steps we replace lowest instances first, so clean those
      } else {
        Collections.sort(
          matchingTaskIds,
          Collections.reverseOrder(SingularityTaskId.INSTANCE_NO_COMPARATOR)
        ); // clean the highest numbers
      }

      List<SingularityTaskId> remainingActiveTasks = new ArrayList<>(matchingTaskIds);
      final int expectedInstances = numMissingInstances + matchingTaskIds.size();
      LOG.info("expected {} active {}", expectedInstances, matchingTaskIds);

      List<Integer> usedIds = new ArrayList<>();
      for (SingularityTaskId taskId : matchingTaskIds) {
        if (
          usedIds.contains(taskId.getInstanceNo()) ||
          taskId.getInstanceNo() > expectedInstances
        ) {
          remainingActiveTasks.remove(taskId);
          LOG.info(
            "Cleaning up task {} due to new request {} - scaling down to {} instances",
            taskId.getId(),
            request.getId(),
            request.getInstancesSafe()
          );
          taskManager.createTaskCleanup(
            new SingularityTaskCleanup(
              pendingRequest.getUser(),
              TaskCleanupType.SCALING_DOWN,
              now,
              taskId,
              Optional.empty(),
              Optional.empty(),
              Optional.empty()
            )
          );
        }
        usedIds.add(taskId.getInstanceNo());
      }

      if (request.isRackSensitive() && configuration.isRebalanceRacksOnScaleDown()) {
        rebalanceRacks(request, state, pendingRequest, remainingActiveTasks);
      }
      if (request.getSlaveAttributeMinimums().isPresent()) {
        rebalanceAttributeDistribution(
          request,
          state,
          pendingRequest,
          remainingActiveTasks
        );
      }
    }
    return numMissingInstances;
  }

  private void rebalanceAttributeDistribution(
    SingularityRequest request,
    RequestState state,
    SingularityPendingRequest pendingRequest,
    List<SingularityTaskId> remainingActiveTasks
  ) {
    Set<SingularityTaskId> extraTasksToClean = rebalancingHelper.rebalanceAttributeDistribution(
      request,
      pendingRequest.getUser(),
      remainingActiveTasks
    );
    remainingActiveTasks.removeAll(extraTasksToClean);
    schedule(
      extraTasksToClean.size(),
      remainingActiveTasks,
      request,
      state,
      pendingRequest
    );
  }

  private void rebalanceRacks(
    SingularityRequest request,
    RequestState state,
    SingularityPendingRequest pendingRequest,
    List<SingularityTaskId> remainingActiveTasks
  ) {
    List<SingularityTaskId> extraCleanedTasks = rebalancingHelper.rebalanceRacks(
      request,
      remainingActiveTasks,
      pendingRequest.getUser()
    );
    remainingActiveTasks.removeAll(extraCleanedTasks);
    if (extraCleanedTasks.size() > 0) {
      schedule(
        extraCleanedTasks.size(),
        remainingActiveTasks,
        request,
        state,
        pendingRequest
      );
    }
  }

  private void schedule(
    int numMissingInstances,
    List<SingularityTaskId> matchingTaskIds,
    SingularityRequest request,
    RequestState state,
    SingularityPendingRequest pendingRequest
  ) {
    final List<SingularityPendingTask> scheduledTasks = getScheduledTaskIds(
      numMissingInstances,
      matchingTaskIds,
      request,
      state,
      pendingRequest.getDeployId(),
      pendingRequest
    );

    if (!scheduledTasks.isEmpty()) {
      LOG.trace("Scheduling tasks: {}", scheduledTasks);

      for (SingularityPendingTask scheduledTask : scheduledTasks) {
        taskManager.savePendingTask(scheduledTask);
      }
    } else {
      LOG.info(
        "No new scheduled tasks found for {}, setting state to {}",
        request.getId(),
        RequestState.FINISHED
      );
      requestManager.finish(request, System.currentTimeMillis());
    }
  }

  private boolean isRequestActive(
    Optional<SingularityRequestWithState> maybeRequestWithState
  ) {
    return SingularityRequestWithState.isActive(maybeRequestWithState);
  }

  private boolean isDeployInUse(
    Optional<SingularityRequestDeployState> requestDeployState,
    String deployId,
    boolean mustMatchActiveDeploy
  ) {
    if (!requestDeployState.isPresent()) {
      return false;
    }

    if (matchesDeployMarker(requestDeployState.get().getActiveDeploy(), deployId)) {
      return true;
    }

    if (mustMatchActiveDeploy) {
      return false;
    }

    return matchesDeployMarker(requestDeployState.get().getPendingDeploy(), deployId);
  }

  private Optional<PendingType> handleCompletedTaskWithStatistics(
    Optional<SingularityTask> task,
    SingularityTaskId taskId,
    long timestamp,
    ExtendedTaskState state,
    SingularityDeployStatistics deployStatistics,
    SingularityCreateResult taskHistoryUpdateCreateResult,
    Protos.TaskStatus status
  ) {
    final Optional<SingularityRequestWithState> maybeRequestWithState = requestManager.getRequest(
      taskId.getRequestId()
    );
    final Optional<SingularityPendingDeploy> maybePendingDeploy = deployManager.getPendingDeploy(
      taskId.getRequestId()
    );

    if (!isRequestActive(maybeRequestWithState)) {
      LOG.warn(
        "Not scheduling a new task, {} is {}",
        taskId.getRequestId(),
        SingularityRequestWithState.getRequestState(maybeRequestWithState)
      );
      return Optional.empty();
    }

    RequestState requestState = maybeRequestWithState.get().getState();
    final SingularityRequest request = maybePendingDeploy.isPresent()
      ? maybePendingDeploy
        .get()
        .getUpdatedRequest()
        .orElse(maybeRequestWithState.get().getRequest())
      : maybeRequestWithState.get().getRequest();

    final Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(
      request.getId()
    );

    if (!isDeployInUse(requestDeployState, taskId.getDeployId(), true)) {
      LOG.debug(
        "Task {} completed, but it didn't match active deploy state {} - ignoring",
        taskId.getId(),
        requestDeployState
      );
      return Optional.empty();
    }

    if (
      taskHistoryUpdateCreateResult == SingularityCreateResult.CREATED &&
      requestState != RequestState.SYSTEM_COOLDOWN
    ) {
      mailer.queueTaskCompletedMail(task, taskId, request, state);
    } else if (requestState == RequestState.SYSTEM_COOLDOWN) {
      LOG.debug(
        "Not sending a task completed email because task {} is in SYSTEM_COOLDOWN",
        taskId
      );
    } else {
      LOG.debug(
        "Not sending a task completed email for task {} because Singularity already processed this update",
        taskId
      );
    }

    if (!status.hasReason() || !status.getReason().equals(Reason.REASON_INVALID_OFFERS)) {
      if (
        state != ExtendedTaskState.TASK_KILLED &&
        !state.isSuccess() &&
        taskHistoryUpdateCreateResult == SingularityCreateResult.CREATED &&
        crashLoops.shouldEnterCooldown(request, requestState, deployStatistics, timestamp)
      ) {
        LOG.info(
          "Request {} is entering cooldown due to task {}",
          request.getId(),
          taskId
        );
        requestState = RequestState.SYSTEM_COOLDOWN;
        requestManager.cooldown(request, System.currentTimeMillis());
        mailer.sendRequestInCooldownMail(request);
      }
    } else {
      LOG.debug(
        "Not triggering cooldown due to TASK_LOST from invalid offers for request {}",
        request.getId()
      );
    }

    PendingType pendingType = PendingType.TASK_DONE;
    Optional<List<String>> cmdLineArgsList = Optional.empty();
    Optional<Resources> resources = Optional.empty();
    Optional<String> message = Optional.empty();

    if (!state.isSuccess() && shouldRetryImmediately(request, deployStatistics, task)) {
      LOG.debug("Retrying {} because {}", request.getId(), state);
      pendingType = PendingType.RETRY;
      if (task.isPresent()) {
        cmdLineArgsList =
          task.get().getTaskRequest().getPendingTask().getCmdLineArgsList();
        resources = task.get().getTaskRequest().getPendingTask().getResources();
        message = task.get().getTaskRequest().getPendingTask().getMessage();
      }
    } else if (!request.isAlwaysRunning()) {
      return Optional.empty();
    }

    if (
      state.isSuccess() &&
      !request.isLongRunning() &&
      requestState == RequestState.SYSTEM_COOLDOWN
    ) {
      LOG.info("Request {} succeeded a task, removing from cooldown", request.getId());
      requestManager.exitCooldown(
        request,
        System.currentTimeMillis(),
        Optional.<String>empty(),
        Optional.<String>empty()
      );
    }

    SingularityPendingRequest pendingRequest = new SingularityPendingRequest(
      request.getId(),
      requestDeployState.get().getActiveDeploy().get().getDeployId(),
      System.currentTimeMillis(),
      Optional.empty(),
      pendingType,
      cmdLineArgsList,
      Optional.empty(),
      Optional.empty(),
      message,
      Optional.empty(),
      resources,
      Collections.emptyList(),
      Optional.empty(),
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyMap(),
      Collections.emptyList(),
      Optional.empty()
    );

    requestManager.addToPendingQueue(pendingRequest);

    return Optional.of(pendingType);
  }

  private SingularityDeployStatistics getDeployStatistics(
    String requestId,
    String deployId
  ) {
    final Optional<SingularityDeployStatistics> maybeDeployStatistics = deployManager.getDeployStatistics(
      requestId,
      deployId
    );

    if (maybeDeployStatistics.isPresent()) {
      return maybeDeployStatistics.get();
    }

    return new SingularityDeployStatisticsBuilder(requestId, deployId).build();
  }

  public void handleCompletedTask(
    Optional<SingularityTask> task,
    SingularityTaskId taskId,
    long timestamp,
    ExtendedTaskState state,
    SingularityCreateResult taskHistoryUpdateCreateResult,
    Protos.TaskStatus status
  ) {
    final SingularityDeployStatistics deployStatistics = getDeployStatistics(
      taskId.getRequestId(),
      taskId.getDeployId()
    );

    if (!task.isPresent() || task.get().getTaskRequest().getRequest().isLoadBalanced()) {
      taskManager.createLBCleanupTask(taskId);
    }

    if (requestManager.isBouncing(taskId.getRequestId())) {
      List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(
        taskId.getRequestId()
      );
      boolean foundBouncingTask = false;
      for (SingularityTaskId activeTaskId : activeTaskIds) {
        Optional<SingularityTaskHistoryUpdate> maybeCleaningUpdate = taskManager.getTaskHistoryUpdate(
          activeTaskId,
          ExtendedTaskState.TASK_CLEANING
        );
        if (maybeCleaningUpdate.isPresent()) {
          if (maybeCleaningUpdate.get().getStatusReason().orElse("").contains("BOUNCE")) { // TaskCleanupType enum is included in status message
            LOG.debug("Found task {} still waiting for bounce to complete", activeTaskId);
            foundBouncingTask = true;
            break;
          } else if (!maybeCleaningUpdate.get().getPrevious().isEmpty()) {
            for (SingularityTaskHistoryUpdate previousUpdate : maybeCleaningUpdate
              .get()
              .getPrevious()) {
              if (previousUpdate.getStatusMessage().orElse("").contains("BOUNCE")) {
                LOG.debug(
                  "Found task {} still waiting for bounce to complete",
                  activeTaskId
                );
                foundBouncingTask = true;
                break;
              }
            }
          }
        }
      }
      if (!foundBouncingTask) {
        LOG.info(
          "Bounce completed for request {}, no cleaning tasks due to bounce found",
          taskId.getRequestId()
        );
        Optional<SingularityExpiringBounce> expiringBounce = requestManager.getExpiringBounce(
          taskId.getRequestId()
        );

        if (
          expiringBounce.isPresent() &&
          expiringBounce.get().getDeployId().equals(taskId.getDeployId())
        ) {
          requestManager.removeExpiringBounce(taskId.getRequestId());
        }
        requestManager.markBounceComplete(taskId.getRequestId());
      }
    }

    final Optional<PendingType> scheduleResult = handleCompletedTaskWithStatistics(
      task,
      taskId,
      timestamp,
      state,
      deployStatistics,
      taskHistoryUpdateCreateResult,
      status
    );

    if (taskHistoryUpdateCreateResult == SingularityCreateResult.EXISTED) {
      return;
    }

    updateDeployStatistics(
      deployStatistics,
      taskId,
      task,
      timestamp,
      state,
      scheduleResult,
      status
    );
  }

  private void updateDeployStatistics(
    SingularityDeployStatistics deployStatistics,
    SingularityTaskId taskId,
    Optional<SingularityTask> task,
    long timestamp,
    ExtendedTaskState state,
    Optional<PendingType> scheduleResult,
    Protos.TaskStatus status
  ) {
    SingularityDeployStatisticsBuilder bldr = deployStatistics.toBuilder();

    if (!state.isFailed()) {
      if (bldr.getAverageRuntimeMillis().isPresent()) {
        long newAvgRuntimeMillis =
          (
            bldr.getAverageRuntimeMillis().get() *
            bldr.getNumTasks() +
            (timestamp - taskId.getStartedAt())
          ) /
          (bldr.getNumTasks() + 1);

        bldr.setAverageRuntimeMillis(Optional.of(newAvgRuntimeMillis));
      } else {
        bldr.setAverageRuntimeMillis(Optional.of(timestamp - taskId.getStartedAt()));
      }
    }

    if (task.isPresent()) {
      long dueTime = task
        .get()
        .getTaskRequest()
        .getPendingTask()
        .getPendingTaskId()
        .getNextRunAt();
      long startedAt = taskId.getStartedAt();

      if (bldr.getAverageSchedulingDelayMillis().isPresent()) {
        long newAverageSchedulingDelayMillis =
          (
            bldr.getAverageSchedulingDelayMillis().get() *
            bldr.getNumTasks() +
            (startedAt - dueTime)
          ) /
          (bldr.getNumTasks() + 1);
        bldr.setAverageSchedulingDelayMillis(
          Optional.of(newAverageSchedulingDelayMillis)
        );
      } else {
        bldr.setAverageSchedulingDelayMillis(Optional.of(startedAt - dueTime));
      }
    }

    bldr.setNumTasks(bldr.getNumTasks() + 1);

    if (!bldr.getLastFinishAt().isPresent() || timestamp > bldr.getLastFinishAt().get()) {
      bldr.setLastFinishAt(Optional.of(timestamp));
      bldr.setLastTaskState(Optional.of(state));
    }

    if (
      task.isPresent() &&
      task.get().getTaskRequest().getRequest().isLongRunning() &&
      state == ExtendedTaskState.TASK_FINISHED
    ) {
      bldr.addTaskFailureEvent(
        new TaskFailureEvent(
          taskId.getInstanceNo(),
          timestamp,
          TaskFailureType.UNEXPECTED_EXIT
        )
      );
    }

    if (state == ExtendedTaskState.TASK_KILLED) {
      if (status.hasMessage()) {
        Optional<TaskCleanupType> maybeCleanupType = getCleanupType(
          taskId,
          status.getMessage()
        );
        if (
          maybeCleanupType.isPresent() &&
          (
            maybeCleanupType.get() == TaskCleanupType.OVERDUE_NEW_TASK ||
            maybeCleanupType.get() == TaskCleanupType.UNHEALTHY_NEW_TASK
          )
        ) {
          bldr.addTaskFailureEvent(
            new TaskFailureEvent(
              taskId.getInstanceNo(),
              timestamp,
              TaskFailureType.STARTUP_FAILURE
            )
          );
        }
      }
    }

    if (!state.isSuccess()) {
      if (
        SingularityTaskHistoryUpdate
          .getUpdate(
            taskManager.getTaskHistoryUpdates(taskId),
            ExtendedTaskState.TASK_CLEANING
          )
          .isPresent()
      ) {
        LOG.debug(
          "{} failed with {} after cleaning - ignoring it for cooldown/crash loop",
          taskId,
          state
        );
      } else {
        if (state.isFailed()) {
          if (
            (
              status.hasMessage() && status.getMessage().contains("Memory limit exceeded")
            ) ||
            (
              status.hasReason() &&
              status.getReason() == Reason.REASON_CONTAINER_LIMITATION_MEMORY
            )
          ) {
            bldr.addTaskFailureEvent(
              new TaskFailureEvent(taskId.getInstanceNo(), timestamp, TaskFailureType.OOM)
            );
          } else if (
            status.hasReason() &&
            status.getReason() == Reason.REASON_CONTAINER_LIMITATION_DISK
          ) {
            bldr.addTaskFailureEvent(
              new TaskFailureEvent(
                taskId.getInstanceNo(),
                timestamp,
                TaskFailureType.OUT_OF_DISK_SPACE
              )
            );
          } else {
            bldr.addTaskFailureEvent(
              new TaskFailureEvent(
                taskId.getInstanceNo(),
                timestamp,
                TaskFailureType.BAD_EXIT_CODE
              )
            );
          }
        }

        if (state == ExtendedTaskState.TASK_LOST && status.hasReason()) {
          if (isMesosError(status.getReason())) {
            bldr.addTaskFailureEvent(
              new TaskFailureEvent(
                taskId.getInstanceNo(),
                timestamp,
                TaskFailureType.MESOS_ERROR
              )
            );
          } else if (isLostSlave(status.getReason())) {
            bldr.addTaskFailureEvent(
              new TaskFailureEvent(
                taskId.getInstanceNo(),
                timestamp,
                TaskFailureType.LOST_SLAVE
              )
            );
          }
        }
        bldr.setNumSuccess(0);
        bldr.setNumFailures(bldr.getNumFailures() + 1);
      }
    } else {
      bldr.setNumSuccess(bldr.getNumSuccess() + 1);
      bldr.setNumFailures(0);
    }

    if (scheduleResult.isPresent() && scheduleResult.get() == PendingType.RETRY) {
      bldr.setNumSequentialRetries(bldr.getNumSequentialRetries() + 1);
    } else {
      bldr.setNumSequentialRetries(0);
    }

    final SingularityDeployStatistics newStatistics = bldr.build();

    LOG.trace("Saving new deploy statistics {}", newStatistics);

    deployManager.saveDeployStatistics(newStatistics);
  }

  private boolean isMesosError(Reason reason) {
    switch (reason) {
      case REASON_COMMAND_EXECUTOR_FAILED:
      case REASON_CONTAINER_LAUNCH_FAILED:
      case REASON_CONTAINER_PREEMPTED:
      case REASON_CONTAINER_UPDATE_FAILED:
      case REASON_EXECUTOR_REGISTRATION_TIMEOUT:
      case REASON_EXECUTOR_REREGISTRATION_TIMEOUT:
      case REASON_EXECUTOR_TERMINATED:
      case REASON_EXECUTOR_UNREGISTERED:
      case REASON_FRAMEWORK_REMOVED:
      case REASON_GC_ERROR:
      case REASON_INVALID_FRAMEWORKID:
      case REASON_INVALID_OFFERS:
      case REASON_MASTER_DISCONNECTED:
      case REASON_RECONCILIATION:
      case REASON_RESOURCES_UNKNOWN:
      case REASON_TASK_GROUP_INVALID:
      case REASON_TASK_GROUP_UNAUTHORIZED:
      case REASON_TASK_INVALID:
      case REASON_TASK_UNAUTHORIZED:
      case REASON_TASK_UNKNOWN:
        return true;
      default:
        return false;
    }
  }

  private boolean isLostSlave(Reason reason) {
    switch (reason) {
      case REASON_AGENT_REMOVED:
      case REASON_AGENT_RESTARTED:
      case REASON_AGENT_UNKNOWN:
      case REASON_AGENT_DISCONNECTED:
      case REASON_AGENT_REMOVED_BY_OPERATOR:
        return true;
      default:
        return false;
    }
  }

  private Optional<TaskCleanupType> getCleanupType(
    SingularityTaskId taskId,
    String statusMessage
  ) {
    try {
      String[] cleanupTypeString = statusMessage.split("\\s+");
      if (cleanupTypeString.length > 0) {
        return Optional.of(TaskCleanupType.valueOf(cleanupTypeString[0]));
      }
    } catch (Throwable t) {
      LOG.info("Could not parse cleanup type from {} for {}", statusMessage, taskId);
    }
    return Optional.empty();
  }

  private boolean shouldRetryImmediately(
    SingularityRequest request,
    SingularityDeployStatistics deployStatistics,
    Optional<SingularityTask> task
  ) {
    if (!request.getNumRetriesOnFailure().isPresent()) {
      return false;
    }

    if (task.isPresent()) {
      if (
        task
          .get()
          .getTaskRequest()
          .getPendingTask()
          .getPendingTaskId()
          .getPendingType() ==
        PendingType.IMMEDIATE &&
        request.getRequestType() == RequestType.SCHEDULED
      ) {
        return false; // don't retry UI triggered scheduled jobs (UI triggered on-demand jobs are okay to retry though)
      }

      Optional<SingularityTaskHistoryUpdate> taskHistoryUpdate = taskManager.getTaskHistoryUpdate(
        task.get().getTaskId(),
        ExtendedTaskState.TASK_CLEANING
      );

      if (
        taskHistoryUpdate.isPresent() &&
        request.getRequestType() == RequestType.ON_DEMAND &&
        taskHistoryUpdate.get().getStatusMessage().orElse("").contains("USER_REQUESTED")
      ) {
        return false; // don't retry one-off launches of on-demand jobs if they were killed by the user
      }
    }

    final int numRetriesInARow = deployStatistics.getNumSequentialRetries();

    if (numRetriesInARow >= request.getNumRetriesOnFailure().get()) {
      LOG.debug(
        "Request {} had {} retries in a row, not retrying again (num retries on failure: {})",
        request.getId(),
        numRetriesInARow,
        request.getNumRetriesOnFailure()
      );
      return false;
    }

    LOG.debug(
      "Request {} had {} retries in a row - retrying again (num retries on failure: {})",
      request.getId(),
      numRetriesInARow,
      request.getNumRetriesOnFailure()
    );

    return true;
  }

  private int getNumMissingInstances(
    List<SingularityTaskId> matchingTaskIds,
    SingularityRequest request,
    SingularityPendingRequest pendingRequest,
    Optional<SingularityPendingDeploy> maybePendingDeploy
  ) {
    PendingType pendingType = pendingRequest.getPendingType();
    if (request.isOneOff()) {
      if (pendingType == PendingType.ONEOFF || pendingType == PendingType.RETRY) {
        return 1;
      } else {
        return 0;
      }
    } else if (
      request.getRequestType() == RequestType.RUN_ONCE &&
      pendingType == PendingType.NEW_DEPLOY
    ) {
      return 1;
    }

    return (
      numInstancesExpected(request, pendingRequest, maybePendingDeploy) -
      matchingTaskIds.size()
    );
  }

  private int numInstancesExpected(
    SingularityRequest request,
    SingularityPendingRequest pendingRequest,
    Optional<SingularityPendingDeploy> maybePendingDeploy
  ) {
    if (
      !maybePendingDeploy.isPresent() ||
      (maybePendingDeploy.get().getCurrentDeployState() == DeployState.CANCELED) ||
      !maybePendingDeploy.get().getDeployProgress().isPresent()
    ) {
      return request.getInstancesSafe();
    }

    SingularityDeployProgress deployProgress = maybePendingDeploy
      .get()
      .getDeployProgress()
      .get();
    if (
      maybePendingDeploy
        .get()
        .getDeployMarker()
        .getDeployId()
        .equals(pendingRequest.getDeployId())
    ) {
      return deployProgress.getTargetActiveInstances();
    } else {
      if (deployProgress.isStepComplete()) {
        return Math.max(
          request.getInstancesSafe() - deployProgress.getTargetActiveInstances(),
          0
        );
      } else {
        return (
          request.getInstancesSafe() -
          (
            Math.max(
              deployProgress.getTargetActiveInstances() -
              deployProgress.getDeployInstanceCountPerStep(),
              0
            )
          )
        );
      }
    }
  }

  private List<SingularityPendingTask> getScheduledTaskIds(
    int numMissingInstances,
    List<SingularityTaskId> matchingTaskIds,
    SingularityRequest request,
    RequestState state,
    String deployId,
    SingularityPendingRequest pendingRequest
  ) {
    final Optional<Long> nextRunAt = getNextRunAt(request, state, pendingRequest);

    if (!nextRunAt.isPresent()) {
      return Collections.emptyList();
    }

    final Set<Integer> inuseInstanceNumbers = Sets.newHashSetWithExpectedSize(
      matchingTaskIds.size()
    );

    for (SingularityTaskId matchingTaskId : matchingTaskIds) {
      inuseInstanceNumbers.add(matchingTaskId.getInstanceNo());
    }

    final List<SingularityPendingTask> newTasks = Lists.newArrayListWithCapacity(
      numMissingInstances
    );

    int nextInstanceNumber = 1;

    for (int i = 0; i < numMissingInstances; i++) {
      while (inuseInstanceNumbers.contains(nextInstanceNumber)) {
        nextInstanceNumber++;
      }

      newTasks.add(
        new SingularityPendingTask(
          new SingularityPendingTaskId(
            request.getId(),
            deployId,
            nextRunAt.get(),
            nextInstanceNumber,
            pendingRequest.getPendingType(),
            pendingRequest.getTimestamp()
          ),
          pendingRequest.getCmdLineArgsList(),
          pendingRequest.getUser(),
          pendingRequest.getRunId(),
          pendingRequest.getSkipHealthchecks(),
          pendingRequest.getMessage(),
          pendingRequest.getResources(),
          pendingRequest.getS3UploaderAdditionalFiles(),
          pendingRequest.getRunAsUserOverride(),
          pendingRequest.getEnvOverrides(),
          pendingRequest.getRequiredSlaveAttributeOverrides(),
          pendingRequest.getAllowedSlaveAttributeOverrides(),
          pendingRequest.getExtraArtifacts(),
          pendingRequest.getActionId()
        )
      );

      nextInstanceNumber++;
    }

    return newTasks;
  }

  private Optional<Long> getNextRunAt(
    SingularityRequest request,
    RequestState state,
    SingularityPendingRequest pendingRequest
  ) {
    PendingType pendingType = pendingRequest.getPendingType();
    final long now = System.currentTimeMillis();

    long nextRunAt = now;

    if (request.isScheduled()) {
      if (pendingType == PendingType.IMMEDIATE || pendingType == PendingType.RETRY) {
        LOG.info("Scheduling requested immediate run of {}", request.getId());
      } else {
        try {
          Date nextRunAtDate = null;
          Date scheduleFrom = null;

          if (request.getScheduleTypeSafe() == ScheduleType.RFC5545) {
            final RFC5545Schedule rfc5545Schedule = new RFC5545Schedule(
              request.getSchedule().get()
            );
            nextRunAtDate = rfc5545Schedule.getNextValidTime();
            scheduleFrom = new Date(rfc5545Schedule.getStartDateTime().getMillis());
          } else {
            scheduleFrom = new Date(now);
            final CronExpression cronExpression = new CronExpression(
              request.getQuartzScheduleSafe()
            );
            if (request.getScheduleTimeZone().isPresent()) {
              cronExpression.setTimeZone(
                TimeZone.getTimeZone(request.getScheduleTimeZone().get())
              );
            }
            nextRunAtDate = cronExpression.getNextValidTimeAfter(scheduleFrom);
          }

          if (nextRunAtDate == null) {
            return Optional.empty();
          }

          LOG.trace(
            "Calculating nextRunAtDate for {} (schedule: {}): {} (from: {})",
            request.getId(),
            request.getSchedule(),
            nextRunAtDate,
            scheduleFrom
          );

          nextRunAt = Math.max(nextRunAtDate.getTime(), now); // don't create a schedule that is overdue as this is used to indicate that singularity is not fulfilling requests.

          LOG.trace(
            "Scheduling next run of {} (schedule: {}) at {} (from: {})",
            request.getId(),
            request.getSchedule(),
            nextRunAtDate,
            scheduleFrom
          );
        } catch (ParseException | InvalidRecurrenceRuleException pe) {
          throw new RuntimeException(pe);
        }
      }
    }

    if (!request.isLongRunning() && pendingRequest.getRunAt().isPresent()) {
      nextRunAt = Math.max(nextRunAt, pendingRequest.getRunAt().get());
    }

    if (
      pendingType == PendingType.TASK_DONE &&
      request.getWaitAtLeastMillisAfterTaskFinishesForReschedule().orElse(0L) > 0
    ) {
      nextRunAt =
        Math.max(
          nextRunAt,
          now + request.getWaitAtLeastMillisAfterTaskFinishesForReschedule().get()
        );

      LOG.trace(
        "Adjusted next run of {} to {} (by {}) due to waitAtLeastMillisAfterTaskFinishesForReschedule",
        request.getId(),
        nextRunAt,
        JavaUtils.durationFromMillis(
          request.getWaitAtLeastMillisAfterTaskFinishesForReschedule().get()
        )
      );
    }

    if (state == RequestState.SYSTEM_COOLDOWN && pendingType != PendingType.NEW_DEPLOY) {
      final long prevNextRunAt = nextRunAt;
      nextRunAt =
        Math.max(
          nextRunAt,
          now + TimeUnit.SECONDS.toMillis(configuration.getCooldownMinScheduleSeconds())
        );
      LOG.trace(
        "Adjusted next run of {} to {} (from: {}) due to cooldown",
        request.getId(),
        nextRunAt,
        prevNextRunAt
      );
    }

    return Optional.of(nextRunAt);
  }

  private SingularityRequest updatedRequest(
    Optional<SingularityPendingDeploy> maybePendingDeploy,
    SingularityPendingRequest pendingRequest,
    SingularityRequestWithState currentRequest
  ) {
    if (
      maybePendingDeploy.isPresent() &&
      pendingRequest
        .getDeployId()
        .equals(maybePendingDeploy.get().getDeployMarker().getDeployId())
    ) {
      return maybePendingDeploy
        .get()
        .getUpdatedRequest()
        .orElse(currentRequest.getRequest());
    } else {
      return currentRequest.getRequest();
    }
  }

  public void checkForStalledTaskLaunches() {
    long now = System.currentTimeMillis();
    taskManager
      .getLaunchingTasks()
      .stream()
      .filter(
        t -> {
          if (t.getStartedAt() < now - configuration.getReconcileLaunchAfterMillis()) {
            Long maybeLastReconcileTime = requestedReconciles.get(t);
            // Don't overwhelm ourselves with status updates, only reconcile each task every 15s
            return maybeLastReconcileTime == null || now - maybeLastReconcileTime > 15000;
          } else {
            return false;
          }
        }
      )
      .forEach(
        taskId -> {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
          if (maybeTask.isPresent()) {
            mesosSchedulerClient.reconcile(
              Collections.singletonList(
                Task
                  .newBuilder()
                  .setTaskId(TaskID.newBuilder().setValue(taskId.toString()).build())
                  .setAgentId(
                    AgentID
                      .newBuilder()
                      .setValue(maybeTask.get().getAgentId().getValue())
                      .build()
                  )
                  .build()
              )
            );
            LOG.info("Requested explicit reconcile of task {}", taskId);
          } else {
            LOG.warn("Could not find full content for task {}", taskId);
          }
        }
      );
    Set<SingularityTaskId> toRemove = requestedReconciles
      .keySet()
      .stream()
      .filter(t -> t.getStartedAt() < now - TimeUnit.MINUTES.toMillis(15))
      .collect(Collectors.toSet());
    toRemove.forEach(requestedReconciles::remove);
  }
}
