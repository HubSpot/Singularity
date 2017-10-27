package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.notFound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdsByStatus;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandHistory;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequestId;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityTaskMetadataRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityTaskMetadataConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper;
import com.ning.http.client.AsyncHttpClient;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.TASK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity tasks.", value=ApiPaths.TASK_RESOURCE_PATH)
public class TaskResource extends AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(TaskResource.class);

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final TaskRequestManager taskRequestManager;
  private final MesosClient mesosClient;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityTaskMetadataConfiguration taskMetadataConfiguration;
  private final SingularityValidator validator;
  private final DisasterManager disasterManager;
  private final SingularityDeployHealthHelper deployHealthHelper;
  private final DeployManager deployManager;

  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager, MesosClient mesosClient, SingularityTaskMetadataConfiguration taskMetadataConfiguration,
                      SingularityAuthorizationHelper authorizationHelper, RequestManager requestManager, SingularityValidator validator, DisasterManager disasterManager,
                      AsyncHttpClient httpClient, LeaderLatch leaderLatch, ObjectMapper objectMapper, SingularityDeployHealthHelper deployHealthHelper, DeployManager deployManager) {
    super(httpClient, leaderLatch, objectMapper);
    this.taskManager = taskManager;
    this.taskRequestManager = taskRequestManager;
    this.taskMetadataConfiguration = taskMetadataConfiguration;
    this.slaveManager = slaveManager;
    this.mesosClient = mesosClient;
    this.requestManager = requestManager;
    this.authorizationHelper = authorizationHelper;
    this.validator = validator;
    this.disasterManager = disasterManager;
    this.deployHealthHelper = deployHealthHelper;
    this.deployManager = deployManager;
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled")
  @ApiOperation("Retrieve list of scheduled tasks.")
  public List<SingularityTaskRequest> getScheduledTasks(@Auth SingularityUser user, @QueryParam("useWebCache") Boolean useWebCache) {
    if (!authorizationHelper.hasAdminAuthorization(user) && disasterManager.isDisabled(SingularityAction.EXPENSIVE_API_CALLS)) {
      LOG.trace("Short circuting getScheduledTasks() to [] due to EXPENSIVE_API_CALLS disabled");
      return Collections.emptyList();
    }

    return taskRequestManager.getTaskRequests(ImmutableList.copyOf(authorizationHelper.filterByAuthorizedRequests(user,
        taskManager.getPendingTasks(useWebCache(useWebCache)), SingularityTransformHelpers.PENDING_TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ)));
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/ids")
  @ApiOperation("Retrieve list of scheduled task IDs.")
  public Iterable<SingularityPendingTaskId> getScheduledTaskIds(@Auth SingularityUser user, @QueryParam("useWebCache") Boolean useWebCache) {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getPendingTaskIds(useWebCache(useWebCache)), SingularityTransformHelpers.PENDING_TASK_ID_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  private SingularityPendingTaskId getPendingTaskIdFromStr(String pendingTaskIdStr) {
    try {
      return SingularityPendingTaskId.valueOf(pendingTaskIdStr);
    } catch (InvalidSingularityTaskIdException e) {
      throw badRequest("%s is not a valid pendingTaskId: %s", pendingTaskIdStr, e.getMessage());
    }
  }

  private SingularityTaskId getTaskIdFromStr(String activeTaskIdStr) {
    try {
      return SingularityTaskId.valueOf(activeTaskIdStr);
    } catch (InvalidSingularityTaskIdException e) {
      throw badRequest("%s is not a valid taskId: %s", activeTaskIdStr, e.getMessage());
    }
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/task/{pendingTaskId}")
  @ApiOperation("Retrieve information about a pending task.")
  public SingularityTaskRequest getPendingTask(@Auth SingularityUser user, @PathParam("pendingTaskId") String pendingTaskIdStr) {
    Optional<SingularityPendingTask> pendingTask = taskManager.getPendingTask(getPendingTaskIdFromStr(pendingTaskIdStr));

    checkNotFound(pendingTask.isPresent(), "Couldn't find %s", pendingTaskIdStr);

    List<SingularityTaskRequest> taskRequestList = taskRequestManager.getTaskRequests(Collections.singletonList(pendingTask.get()));

    checkNotFound(!taskRequestList.isEmpty(), "Couldn't find: " + pendingTaskIdStr);

    authorizationHelper.checkForAuthorization(taskRequestList.get(0).getRequest(), user, SingularityAuthorizationScope.READ);

    return taskRequestList.get(0);
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/request/{requestId}")
  @ApiOperation("Retrieve list of scheduled tasks for a specific request.")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@Auth SingularityUser user, @PathParam("requestId") String requestId, @QueryParam("useWebCache") Boolean useWebCache) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final List<SingularityPendingTask> tasks = Lists.newArrayList(Iterables.filter(taskManager.getPendingTasks(useWebCache(useWebCache)), SingularityPendingTask.matchingRequest(requestId)));

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @Path("/ids/request/{requestId}")
  @ApiOperation("Retrieve a list of task ids separated by status")
  public Optional<SingularityTaskIdsByStatus> getTaskIdsByStatusForRequest(@Auth SingularityUser user, @PathParam("requestId") String requestId) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    Optional<SingularityRequestWithState> requestWithState = requestManager.getRequest(requestId);
    if (!requestWithState.isPresent()) {
       return Optional.absent();
    }
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
          requestWithState.get().getRequest(),
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

  @GET
  @Path("/active/slave/{slaveId}")
  @ApiOperation("Retrieve list of active tasks on a specific slave.")
  public Iterable<SingularityTask> getTasksForSlave(@Auth SingularityUser user, @PathParam("slaveId") String slaveId, @QueryParam("useWebCache") Boolean useWebCache) {
    Optional<SingularitySlave> maybeSlave = slaveManager.getObject(slaveId);

    checkNotFound(maybeSlave.isPresent(), "Couldn't find a slave in any state with id %s", slaveId);

    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(useWebCache(useWebCache)), maybeSlave.get()), SingularityTransformHelpers.TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation("Retrieve the list of active tasks.")
  public Iterable<SingularityTask> getActiveTasks(@Auth SingularityUser user, @QueryParam("useWebCache") Boolean useWebCache) {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getActiveTasks(useWebCache(useWebCache)), SingularityTransformHelpers.TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/cleaning")
  @ApiOperation("Retrieve the list of cleaning tasks.")
  public Iterable<SingularityTaskCleanup> getCleaningTasks(@Auth SingularityUser user, @QueryParam("useWebCache") Boolean useWebCache) {
    if (!authorizationHelper.hasAdminAuthorization(user) && disasterManager.isDisabled(SingularityAction.EXPENSIVE_API_CALLS)) {
      LOG.trace("Short circuting getCleaningTasks() to [] due to EXPENSIVE_API_CALLS disabled");
      return Collections.emptyList();
    }

    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getCleanupTasks(useWebCache(useWebCache)), SingularityTransformHelpers.TASK_CLEANUP_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @Path("/killed")
  @ApiOperation("Retrieve the list of killed tasks.")
  public Iterable<SingularityKilledTaskIdRecord> getKilledTasks(@Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getKilledTaskIdRecords(), SingularityTransformHelpers.KILLED_TASK_ID_RECORD_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @ApiOperation("Retrieve the list of tasks being cleaned from load balancers.")
  public Iterable<SingularityTaskId> getLbCleanupTasks(@Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getLBCleanupTasks(), SingularityTransformHelpers.TASK_ID_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  private SingularityTask checkActiveTask(String taskId, SingularityAuthorizationScope scope, SingularityUser user) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    Optional<SingularityTask> task = taskManager.getTask(taskIdObj);

    checkNotFound(task.isPresent() && taskManager.isActiveTask(taskId), "No active task with id %s", taskId);

    if (task.isPresent()) {
      authorizationHelper.checkForAuthorizationByRequestId(task.get().getTaskId().getRequestId(), user, scope);
    }

    return task.get();
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve information about a specific active task.")
  public SingularityTask getActiveTask(@Auth SingularityUser user, @PathParam("taskId") String taskId) {
    return checkActiveTask(taskId, SingularityAuthorizationScope.READ, user);
  }

  @GET
  @Path("/task/{taskId}/statistics")
  @ApiOperation("Retrieve statistics about a specific active task.")
  public MesosTaskStatisticsObject getTaskStatistics(@Auth SingularityUser user, @PathParam("taskId") String taskId) {
    SingularityTask task = checkActiveTask(taskId, SingularityAuthorizationScope.READ, user);

    String executorIdToMatch = null;

    if (task.getMesosTask().hasExecutor()) {
      executorIdToMatch = task.getMesosTask().getExecutor().getExecutorId().getValue();
    } else {
      executorIdToMatch = taskId;
    }

    for (MesosTaskMonitorObject taskMonitor : mesosClient.getSlaveResourceUsage(task.getHostname())) {
      if (taskMonitor.getExecutorId().equals(executorIdToMatch)) {
        return taskMonitor.getStatistics();
      }
    }

    throw notFound("Couldn't find executor %s for %s on slave %s", executorIdToMatch, taskId, task.getHostname());
  }

  @GET
  @Path("/task/{taskId}/cleanup")
  @ApiOperation("Get the cleanup object for the task, if it exists")
  public Optional<SingularityTaskCleanup> getTaskCleanup(@Auth SingularityUser user, @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    return taskManager.getTaskCleanup(taskId);
  }

  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanup killTask(@Auth SingularityUser user, @PathParam("taskId") String taskId, @Context HttpServletRequest requestContext) {
    return killTask(taskId, requestContext, null, user);
  }

  @DELETE
  @Path("/task/{taskId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Attempt to kill task, optionally overriding an existing cleanup request (that may be waiting for replacement tasks to become healthy)", response=SingularityTaskCleanup.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Task already has a cleanup request (can be overridden with override=true)")
  })
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId,
                                         @Context HttpServletRequest requestContext,
                                         SingularityKillTaskRequest killTaskRequest,
                                         @Auth SingularityUser user) {
    final Optional<SingularityKillTaskRequest> maybeKillTaskRequest = Optional.fromNullable(killTaskRequest);
    return maybeProxyToLeader(requestContext, SingularityTaskCleanup.class, maybeKillTaskRequest.orNull(), () -> killTask(taskId, maybeKillTaskRequest, user));
  }

  public SingularityTaskCleanup killTask(String taskId, Optional<SingularityKillTaskRequest> killTaskRequest, SingularityUser user) {
    final SingularityTask task = checkActiveTask(taskId, SingularityAuthorizationScope.WRITE, user);

    Optional<String> message = Optional.absent();
    Optional<Boolean> override = Optional.absent();
    Optional<String> actionId = Optional.absent();
    Optional<Boolean> waitForReplacementTask = Optional.absent();
    Optional<SingularityTaskShellCommandRequestId> runBeforeKillId = Optional.absent();

    if (killTaskRequest.isPresent()) {
      actionId = killTaskRequest.get().getActionId();
      message = killTaskRequest.get().getMessage();
      override = killTaskRequest.get().getOverride();
      waitForReplacementTask = killTaskRequest.get().getWaitForReplacementTask();
      if (killTaskRequest.get().getRunShellCommandBeforeKill().isPresent()) {
        SingularityTaskShellCommandRequest shellCommandRequest = startShellCommand(task.getTaskId(), killTaskRequest.get().getRunShellCommandBeforeKill().get(), user);
        runBeforeKillId = Optional.of(shellCommandRequest.getId());
      }
    }

    TaskCleanupType cleanupType = TaskCleanupType.USER_REQUESTED;

    if (waitForReplacementTask.or(Boolean.FALSE)) {
      cleanupType = TaskCleanupType.USER_REQUESTED_TASK_BOUNCE;
      validator.checkActionEnabled(SingularityAction.BOUNCE_TASK);
    } else {
      validator.checkActionEnabled(SingularityAction.KILL_TASK);
    }

    final long now = System.currentTimeMillis();

    final SingularityTaskCleanup taskCleanup;

    if (override.isPresent() && override.get()) {
      LOG.debug("Requested destroy of {}", taskId);
      cleanupType = TaskCleanupType.USER_REQUESTED_DESTROY;
      taskCleanup = new SingularityTaskCleanup(user.getEmail(), cleanupType, now,
        task.getTaskId(), message, actionId, runBeforeKillId);
      taskManager.saveTaskCleanup(taskCleanup);
    } else {
      taskCleanup = new SingularityTaskCleanup(user.getEmail(), cleanupType, now,
        task.getTaskId(), message, actionId, runBeforeKillId);
      SingularityCreateResult result = taskManager.createTaskCleanup(taskCleanup);

      if (result == SingularityCreateResult.EXISTED && userRequestedKillTakesPriority(taskId)) {
        taskManager.saveTaskCleanup(taskCleanup);
      } else {
        while (result == SingularityCreateResult.EXISTED) {
          Optional<SingularityTaskCleanup> cleanup = taskManager.getTaskCleanup(taskId);

          if (cleanup.isPresent()) {
            throw new WebApplicationException(Response.status(Status.CONFLICT).entity(cleanup.get()).type(MediaType.APPLICATION_JSON).build());
          }

          result = taskManager.createTaskCleanup(taskCleanup);
        }
      }
    }

    if (cleanupType == TaskCleanupType.USER_REQUESTED_TASK_BOUNCE) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(task.getTaskId().getRequestId(), task.getTaskId().getDeployId(), now, user.getEmail(),
          PendingType.TASK_BOUNCE, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), message, actionId));
    }

    return taskCleanup;
  }

  boolean userRequestedKillTakesPriority(String taskId) {
    Optional<SingularityTaskCleanup> existingCleanup = taskManager.getTaskCleanup(taskId);
    if (!existingCleanup.isPresent()) {
      return true;
    }
    return existingCleanup.get().getCleanupType() != TaskCleanupType.USER_REQUESTED_DESTROY;
  }

  @Path("/commands/queued")
  @ApiOperation(value="Retrieve a list of all the shell commands queued for execution")
  public List<SingularityTaskShellCommandRequest> getQueuedShellCommands(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return taskManager.getAllQueuedTaskShellCommandRequests();
  }

  @POST
  @Path("/task/{taskId}/metadata")
  @ApiOperation(value="Post metadata about a task that will be persisted along with it and displayed in the UI")
  @ApiResponses({
    @ApiResponse(code=400, message="Invalid metadata object or doesn't match allowed types"),
    @ApiResponse(code=404, message="Task doesn't exist"),
    @ApiResponse(code=409, message="Metadata with this type/timestamp already existed")
  })
  @Consumes({ MediaType.APPLICATION_JSON })
  public void postTaskMetadata(@Auth SingularityUser user, @PathParam("taskId") String taskId, final SingularityTaskMetadataRequest taskMetadataRequest) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.ADD_METADATA);

    WebExceptions.checkBadRequest(taskMetadataRequest.getTitle().length() < taskMetadataConfiguration.getMaxMetadataTitleLength(),
      "Task metadata title too long, must be less than %s bytes", taskMetadataConfiguration.getMaxMetadataTitleLength());

    int messageLength = taskMetadataRequest.getMessage().isPresent() ? taskMetadataRequest.getMessage().get().length() : 0;
    WebExceptions.checkBadRequest(!taskMetadataRequest.getMessage().isPresent() || messageLength < taskMetadataConfiguration.getMaxMetadataMessageLength(),
      "Task metadata message too long, must be less than %s bytes", taskMetadataConfiguration.getMaxMetadataMessageLength());

    if (taskMetadataConfiguration.getAllowedMetadataTypes().isPresent()) {
      WebExceptions.checkBadRequest(taskMetadataConfiguration.getAllowedMetadataTypes().get().contains(taskMetadataRequest.getType()), "%s is not one of the allowed metadata types %s",
          taskMetadataRequest.getType(), taskMetadataConfiguration.getAllowedMetadataTypes().get());
    }

    WebExceptions.checkNotFound(taskManager.taskExistsInZk(taskIdObj), "Task %s not found in ZooKeeper (can not save metadata to tasks which have been persisted", taskIdObj);

    final SingularityTaskMetadata taskMetadata = new SingularityTaskMetadata(taskIdObj, System.currentTimeMillis(), taskMetadataRequest.getType(), taskMetadataRequest.getTitle(),
        taskMetadataRequest.getMessage(),  user.getEmail(), taskMetadataRequest.getLevel());

    SingularityCreateResult result = taskManager.saveTaskMetadata(taskMetadata);

    WebExceptions.checkConflict(result == SingularityCreateResult.CREATED, "Task metadata conficted with existing metadata for %s at %s", taskMetadata.getType(), taskMetadata.getTimestamp());
  }

  @POST
  @Path("/task/{taskId}/command")
  @ApiOperation(value="Run a configured shell command against the given task")
  @ApiResponses({
    @ApiResponse(code=400, message="Given shell command option doesn't exist"),
    @ApiResponse(code=403, message="Given shell command doesn't exist")
  })
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskShellCommandRequest runShellCommand(@Auth SingularityUser user, @PathParam("taskId") String taskId, final SingularityShellCommand shellCommand) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.RUN_SHELL_COMMAND);

    if (!taskManager.isActiveTask(taskId)) {
      throw WebExceptions.badRequest("%s is not an active task, can't run %s on it", taskId, shellCommand.getName());
    }

    return startShellCommand(taskIdObj, shellCommand, user);
  }

  private SingularityTaskShellCommandRequest startShellCommand(SingularityTaskId taskId, final SingularityShellCommand shellCommand, SingularityUser user) {
    validator.checkValidShellCommand(shellCommand);

    SingularityTaskShellCommandRequest shellRequest = new SingularityTaskShellCommandRequest(taskId, user.getEmail(), System.currentTimeMillis(), shellCommand);
    taskManager.saveTaskShellCommandRequestToQueue(shellRequest);
    return shellRequest;
  }

  @GET
  @Path("/task/{taskId}/command")
  @ApiOperation(value="Retrieve a list of shell commands that have run for a task")
  public List<SingularityTaskShellCommandHistory> getShellCommandHisotry(@Auth SingularityUser user, @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);
    return taskManager.getTaskShellCommandHistory(taskIdObj);
  }

  @GET
  @Path("/task/{taskId}/command/{commandName}/{commandTimestamp}")
  @ApiOperation(value="Retrieve a list of shell commands updates for a particular shell command on a task")
  public List<SingularityTaskShellCommandUpdate> getShellCommandHisotryUpdates(@Auth SingularityUser user, @PathParam("taskId") String taskId, @PathParam("commandName") String commandName, @PathParam("commandTimestamp") Long commandTimestamp) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);
    return taskManager.getTaskShellCommandUpdates(new SingularityTaskShellCommandRequestId(taskIdObj, commandName, commandTimestamp));
  }
}
