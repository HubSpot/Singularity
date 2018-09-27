package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.notFound;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.activation.MimetypesFileTypeMap;
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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

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
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
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
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityTaskMetadataConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SandboxManager.SlaveNotFoundException;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.hubspot.singularity.helpers.RequestHelper;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.PerRequestConfig;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.TASK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manage Singularity tasks")
@Tags({@Tag(name = "Tasks")})
public class TaskResource extends AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(TaskResource.class);

  private final AsyncHttpClient httpClient;
  private final MesosConfiguration configuration;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final TaskRequestManager taskRequestManager;
  private final MesosClient mesosClient;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityTaskMetadataConfiguration taskMetadataConfiguration;
  private final SingularityValidator validator;
  private final DisasterManager disasterManager;
  private final RequestHelper requestHelper;
  private final MimetypesFileTypeMap fileTypeMap;

  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager, MesosClient mesosClient, SingularityTaskMetadataConfiguration taskMetadataConfiguration,
                      SingularityAuthorizationHelper authorizationHelper, RequestManager requestManager, SingularityValidator validator, DisasterManager disasterManager,
                      AsyncHttpClient httpClient, LeaderLatch leaderLatch, ObjectMapper objectMapper, RequestHelper requestHelper, MesosConfiguration configuration) {
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
    this.requestHelper = requestHelper;
    this.httpClient = httpClient;
    this.configuration = configuration;
    this.fileTypeMap = new MimetypesFileTypeMap();
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled")
  @Operation(summary = "Retrieve list of scheduled tasks")
  public List<SingularityTaskRequest> getScheduledTasks(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Use the cached version of this data to limit expensive api calls") @QueryParam("useWebCache") Boolean useWebCache) {
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
  @Operation(
      summary = "Retrieve list of pending task IDs",
      description = "A list of tasks that are scheduled and waiting to be launched"
  )
  public List<SingularityPendingTaskId> getScheduledTaskIds(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Use the cached version of this data to limit expensive api calls") @QueryParam("useWebCache") Boolean useWebCache) {
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
  @Operation(summary = "Retrieve information about a pending task")
  public SingularityTaskRequest getPendingTask(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "The unique id of the pending task") @PathParam("pendingTaskId") String pendingTaskIdStr) {
    Optional<SingularityPendingTask> pendingTask = taskManager.getPendingTask(getPendingTaskIdFromStr(pendingTaskIdStr));

    checkNotFound(pendingTask.isPresent(), "Couldn't find %s", pendingTaskIdStr);

    List<SingularityTaskRequest> taskRequestList = taskRequestManager.getTaskRequests(Collections.singletonList(pendingTask.get()));

    checkNotFound(!taskRequestList.isEmpty(), "Couldn't find: " + pendingTaskIdStr);

    authorizationHelper.checkForAuthorization(taskRequestList.get(0).getRequest(), user, SingularityAuthorizationScope.READ);

    return taskRequestList.get(0);
  }

  @DELETE
  @Path("/scheduled/task/{scheduledTaskId}")
  @Operation(
      summary = "Delete a scheduled task by id",
      responses = {
          @ApiResponse(responseCode = "200", description = "Deletion has been enqueued and the task will be deleted when the scheduler poller runs next"),
          @ApiResponse(responseCode = "404", description = "A request with the specified id did not exist or the pending task with the specified id was not found"),
          @ApiResponse(responseCode = "400", description = "The request is not of a type that allows pending task deletes (only ON_DEMAND requests allow deletes)")
      }
  )
  public Optional<SingularityPendingTask> deleteScheduledTask(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "The id of the scheduled/pending task to delete") @PathParam("scheduledTaskId") String taskId,
      @Context HttpServletRequest requestContext) {
    return maybeProxyToLeader(requestContext, Optional.class, null, () -> deleteScheduledTask(taskId, user));
  }

  public Optional<SingularityPendingTask> deleteScheduledTask(String taskId, SingularityUser user) {
    Optional<SingularityPendingTask> maybePendingTask = taskManager.getPendingTask(getPendingTaskIdFromStr(taskId));

    if (maybePendingTask.isPresent()) {
      SingularityPendingTaskId pendingTaskId = maybePendingTask.get().getPendingTaskId();

      Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(pendingTaskId.getRequestId());
      checkNotFound(maybeRequest.isPresent(), "Couldn't find: " + taskId);

      SingularityRequest request = maybeRequest.get().getRequest();
      authorizationHelper.checkForAuthorizationByRequestId(request.getId(), user, SingularityAuthorizationScope.WRITE);
      checkBadRequest(request.getRequestType() == RequestType.ON_DEMAND, "Only ON_DEMAND tasks may be deleted.");

      taskManager.markPendingTaskForDeletion(pendingTaskId);
    }
    return maybePendingTask;
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/request/{requestId}")
  @Operation(summary = "Retrieve list of pending/scheduled tasks for a specific request")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request id to retrieve pending tasks for") @PathParam("requestId") String requestId,
      @Parameter(description = "Use the cached version of this data to limit expensive api calls") @QueryParam("useWebCache") Boolean useWebCache) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final List<SingularityPendingTask> tasks = Lists.newArrayList(Iterables.filter(taskManager.getPendingTasks(useWebCache(useWebCache)), SingularityPendingTask.matchingRequest(requestId)));

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @Path("/ids/request/{requestId}")
  @Operation(
      summary = "Retrieve a list of task ids separated by status",
      description = "Includes pending, active, and cleaning tasks",
      responses = {
          @ApiResponse(responseCode = "404", description = "A request with the specified id was not found")
      }
  )
  public Optional<SingularityTaskIdsByStatus> getTaskIdsByStatusForRequest(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "The request id to retrieve tasks for") @PathParam("requestId") String requestId) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    return requestHelper.getTaskIdsByStatusForRequest(requestId);
  }

  @GET
  @Path("/active/slave/{slaveId}")
  @Operation(
      summary = "Retrieve list of active tasks on a specific slave",
      responses = {
          @ApiResponse(responseCode = "404", description = "A slave with the specified id was not found")
      }
  )
  public List<SingularityTask> getTasksForSlave(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "The mesos slave id to retrieve tasks for") @PathParam("slaveId") String slaveId,
      @Parameter(description = "Use the cached version of this data to limit expensive api calls") @QueryParam("useWebCache") Boolean useWebCache) {
    Optional<SingularitySlave> maybeSlave = slaveManager.getObject(slaveId);

    checkNotFound(maybeSlave.isPresent(), "Couldn't find a slave in any state with id %s", slaveId);

    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(useWebCache(useWebCache)), maybeSlave.get()), SingularityTransformHelpers.TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @Operation(summary = "Retrieve the list of active tasks for all requests")
  public List<SingularityTask> getActiveTasks(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Use the cached version of this data to limit expensive api calls") @QueryParam("useWebCache") Boolean useWebCache) {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getActiveTasks(useWebCache(useWebCache)), SingularityTransformHelpers.TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/cleaning")
  @Operation(summary = "Retrieve the list of cleaning tasks for all requests")
  public List<SingularityTaskCleanup> getCleaningTasks(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Use the cached version of this data to limit expensive api calls") @QueryParam("useWebCache") Boolean useWebCache) {
    if (!authorizationHelper.hasAdminAuthorization(user) && disasterManager.isDisabled(SingularityAction.EXPENSIVE_API_CALLS)) {
      LOG.trace("Short circuting getCleaningTasks() to [] due to EXPENSIVE_API_CALLS disabled");
      return Collections.emptyList();
    }

    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getCleanupTasks(useWebCache(useWebCache)), SingularityTransformHelpers.TASK_CLEANUP_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @Path("/killed")
  @Operation(
      summary = "Retrieve the list of killed task ids for all requests",
      description = "A list of task ids where the task has been sent a kill but has not yet sent a status update with a terminal state"
  )
  public List<SingularityKilledTaskIdRecord> getKilledTasks(@Parameter(hidden = true) @Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getKilledTaskIdRecords(), SingularityTransformHelpers.KILLED_TASK_ID_RECORD_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @Operation(summary = "Retrieve the list of task ids being cleaned from load balancers")
  public List<SingularityTaskId> getLbCleanupTasks(@Parameter(hidden = true) @Auth SingularityUser user) {
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
  @Operation(
      summary = "Retrieve information about a specific active task",
      responses = {
          @ApiResponse(responseCode = "404", description = "A task with this id was not found")
      }
  )
  public SingularityTask getActiveTask(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Id of the task") @PathParam("taskId") String taskId) {
    return checkActiveTask(taskId, SingularityAuthorizationScope.READ, user);
  }

  @GET
  @Path("/task/{taskId}/statistics")
  @Operation(
      summary = "Retrieve resource usage statistics about a specific active task",
      responses = {
          @ApiResponse(responseCode = "404", description = "A task with this id, or slave and executor with matching statistics was not found")
      }
  )
  public MesosTaskStatisticsObject getTaskStatistics(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Id of the task") @PathParam("taskId") String taskId) {
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
  @Operation(
      summary = "Get the cleanup object for the task, if it exists",
      responses = {
          @ApiResponse(responseCode = "404", description = "No cleanup exists for this task, or no task with this id exists")
      }
  )
  public Optional<SingularityTaskCleanup> getTaskCleanup(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Id of the task") @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    return taskManager.getTaskCleanup(taskId);
  }

  @DELETE
  @Path("/task/{taskId}")
  @Operation(
      summary = "Trigger a task kill",
      responses = {
          @ApiResponse(responseCode = "200", description = "Returns the cleanup created to trigger a task kill")
      }
  )
  public SingularityTaskCleanup killTask(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Id of the task to kill") @PathParam("taskId") String taskId,
      @Context HttpServletRequest requestContext) {
    return killTask(taskId, requestContext, null, user);
  }

  @DELETE
  @Path("/task/{taskId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Attempt to kill task, optionally overriding an existing cleanup request (that may be waiting for replacement tasks to become healthy)",
      responses = {
          @ApiResponse(responseCode = "200", description = "Returns the cleanup created to trigger a task kill"),
          @ApiResponse(responseCode = "409", description = "Task already has a cleanup request (can be overridden with override=true)")
      }
  )
  public SingularityTaskCleanup killTask(
      @Parameter(description = "Id of the task to kill") @PathParam("taskId") String taskId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "Overrides related to how the task kill is performed") SingularityKillTaskRequest killTaskRequest,
      @Parameter(hidden = true) @Auth SingularityUser user) {
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

  @GET
  @Path("/commands/queued")
  @Operation(summary = "Retrieve a list of all the shell commands queued for execution")
  public List<SingularityTaskShellCommandRequest> getQueuedShellCommands(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return taskManager.getAllQueuedTaskShellCommandRequests();
  }

  @POST
  @Path("/task/{taskId}/metadata")
  @Operation(
      summary = "Post metadata about a task that will be persisted along with it and displayed in the UI",
      responses = {
          @ApiResponse(responseCode = "400", description = "Invalid metadata object or doesn't match allowed types"),
          @ApiResponse(responseCode = "404", description = "Task doesn't exist"),
          @ApiResponse(responseCode = "409", description = "Metadata with this type/timestamp already existed")
      }
  )
  @Consumes({ MediaType.APPLICATION_JSON })
  public void postTaskMetadata(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the task") @PathParam("taskId") String taskId,
      @RequestBody(description = "Metadata to attach to the task", required = true) final SingularityTaskMetadataRequest taskMetadataRequest) {
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
  @Operation(
      summary = "Run a configured shell command against the given task",
      responses = {
          @ApiResponse(responseCode = "400", description = "Given shell command option doesn't exist"),
          @ApiResponse(responseCode = "403", description = "Given shell command doesn't exist")
      }
  )
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskShellCommandRequest runShellCommand(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the task")@PathParam("taskId") String taskId,
      @RequestBody(required = true, description = "Object describing the command to be run") final SingularityShellCommand shellCommand) {
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
  @Operation(summary = "Retrieve a list of shell commands that have run for a task")
  public List<SingularityTaskShellCommandHistory> getShellCommandHisotry(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the task") @PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);
    return taskManager.getTaskShellCommandHistory(taskIdObj);
  }

  @GET
  @Path("/task/{taskId}/command/{commandName}/{commandTimestamp}")
  @Operation(summary = "Retrieve a list of shell commands updates for a particular shell command on a task")
  public List<SingularityTaskShellCommandUpdate> getShellCommandHisotryUpdates(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the task") @PathParam("taskId") String taskId,
      @Parameter(required = true, description = "name of the command that was run") @PathParam("commandName") String commandName,
      @Parameter(required = true, description = "Timestamp of the original shell command request") @PathParam("commandTimestamp") Long commandTimestamp) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);
    return taskManager.getTaskShellCommandUpdates(new SingularityTaskShellCommandRequestId(taskIdObj, commandName, commandTimestamp));
  }

  @GET
  @Path("/download/")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @Operation(summary = "Proxy a file download from a Mesos Slave through Singularity")
  public Response downloadFileOverProxy(
      @Parameter(required = true, description = "Mesos slave hostname") @QueryParam("slaveHostname") String slaveHostname,
      @Parameter(required = true, description = "Full file path to file on Mesos slave to be downloaded") @QueryParam("path") String fileFullPath
  ) {
    return getFile(slaveHostname, fileFullPath, true);
  }

  @GET
  @Path("/open/")
  @Produces("*/*")
  @Operation(summary = "Open a file from a Mesos Slave through Singularity")
  public Response openFileOverProxy(
      @Parameter(required = true, description = "Mesos slave hostname") @QueryParam("slaveHostname") String slaveHostname,
      @Parameter(required = true, description = "Full file path to file on Mesos slave to be downloaded") @QueryParam("path") String fileFullPath
  ) {
    return getFile(slaveHostname, fileFullPath, false);
  }

  private Response getFile(String slaveHostname, String fileFullPath, boolean download) {
    String httpPrefix = configuration.getSlaveHttpsPort().isPresent() ? "https" : "http";
    int httpPort = configuration.getSlaveHttpsPort().isPresent() ? configuration.getSlaveHttpsPort().get() : configuration.getSlaveHttpPort();

    String url = String.format("%s://%s:%s/files/download.json",
        httpPrefix, slaveHostname, httpPort);

    try {
      PerRequestConfig unlimitedTimeout = new PerRequestConfig();
      unlimitedTimeout.setRequestTimeoutInMs(-1);

      NingOutputToJaxRsStreamingOutputWrapper streamingOutputNingHandler = new NingOutputToJaxRsStreamingOutputWrapper(
          httpClient
              .prepareGet(url)
              .addQueryParameter("path", fileFullPath)
              .setPerRequestConfig(unlimitedTimeout)
      );

      // Strip file path down to just a file name if we can
      java.nio.file.Path filePath = Paths.get(fileFullPath).getFileName();
      String fileName = filePath != null ? filePath.toString() : fileFullPath;

      ResponseBuilder responseBuilder = Response.ok(streamingOutputNingHandler);

      if (download) {
        final String headerValue = String.format("attachment; filename=\"%s\"", fileName);
        responseBuilder.header("Content-Disposition", headerValue);
      } else {
        // Guess type based on extension since we don't have the file locally to check content
        final String maybeContentType = fileTypeMap.getContentType(fileFullPath);
        responseBuilder.header("Content-Type", maybeContentType);
      }
      return responseBuilder.build();
    } catch (Exception e) {
      if (e.getCause().getClass() == ConnectException.class) {
        throw new SlaveNotFoundException(e);
      } else {
        throw new RuntimeException(e);
      }
    }
  }

  private static class NingOutputToJaxRsStreamingOutputWrapper implements AsyncHandler<Void>, StreamingOutput {
    private OutputStream wrappedOutputStream;
    private BoundRequestBuilder requestBuilder;

    public NingOutputToJaxRsStreamingOutputWrapper(BoundRequestBuilder requestBuilder) {
      this.requestBuilder = requestBuilder;
    }

    @Override
    public void onThrowable(Throwable t) {
      LOG.error("Unable to proxy file download", t);
    }

    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
      bodyPart.writeTo(wrappedOutputStream);
      wrappedOutputStream.flush();
      return STATE.CONTINUE;
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus responseStatus) {
      LOG.trace("Proxying download of {} from Mesos: Status={}", requestBuilder.build().getUrl(), responseStatus.getStatusCode());
      return STATE.CONTINUE;
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) {
      LOG.trace("Proxying download of {} from Mesos: Headers={}", requestBuilder.build().getUrl(), headers.getHeaders());
      return STATE.CONTINUE;
    }

    @Override
    public Void onCompleted() {
      LOG.info("Proxying download of {} from Mesos: Completed.", requestBuilder.build().getUrl());
      return null;
    }

    // StreamingOutput implementation: just make the OutputStream available to the AsyncHandler<> implementation.
    @Override
    public void write(OutputStream output) throws WebApplicationException, IOException {
      if (wrappedOutputStream == null) {
        wrappedOutputStream = output;
        try {
          requestBuilder.execute(this).get();
        } catch (ExecutionException | InterruptedException e) {
          LOG.error("Failed or interrupted while proxying a download from Mesos", e);
        }
      }
    }
  }
}
