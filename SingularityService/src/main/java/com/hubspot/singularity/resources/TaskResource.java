package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.notFound;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityTaskMetadataRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.SingularityTaskMetadataConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.config.shell.ShellCommandDescriptor;
import com.hubspot.singularity.config.shell.ShellCommandOptionDescriptor;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(TaskResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity tasks.", value=TaskResource.PATH)
public class TaskResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/tasks";

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;
  private final TaskRequestManager taskRequestManager;
  private final MesosClient mesosClient;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final Optional<SingularityUser> user;
  private final SingularityTaskMetadataConfiguration taskMetadataConfiguration;
  private final UIConfiguration uiConfiguration;

  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager, MesosClient mesosClient, SingularityTaskMetadataConfiguration taskMetadataConfiguration,
      SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user, UIConfiguration uiConfiguration, RequestManager requestManager) {
    this.taskManager = taskManager;
    this.taskRequestManager = taskRequestManager;
    this.taskMetadataConfiguration = taskMetadataConfiguration;
    this.slaveManager = slaveManager;
    this.mesosClient = mesosClient;
    this.requestManager = requestManager;
    this.authorizationHelper = authorizationHelper;
    this.user = user;
    this.uiConfiguration = uiConfiguration;
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled")
  @ApiOperation("Retrieve list of scheduled tasks.")
  public List<SingularityTaskRequest> getScheduledTasks() {
    return taskRequestManager.getTaskRequests(ImmutableList.copyOf(authorizationHelper.filterByAuthorizedRequests(user, taskManager.getPendingTasks(), SingularityTransformHelpers.PENDING_TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ)));
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/ids")
  @ApiOperation("Retrieve list of scheduled task IDs.")
  public Iterable<SingularityPendingTaskId> getScheduledTaskIds() {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getPendingTaskIds(), SingularityTransformHelpers.PENDING_TASK_ID_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
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
  public SingularityTaskRequest getPendingTask(@PathParam("pendingTaskId") String pendingTaskIdStr) {
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
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@PathParam("requestId") String requestId) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    final List<SingularityPendingTask> tasks = Lists.newArrayList(Iterables.filter(taskManager.getPendingTasks(), SingularityPendingTask.matchingRequest(requestId)));

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @Path("/active/slave/{slaveId}")
  @ApiOperation("Retrieve list of active tasks on a specific slave.")
  public Iterable<SingularityTask> getTasksForSlave(@PathParam("slaveId") String slaveId) {
    Optional<SingularitySlave> maybeSlave = slaveManager.getObject(slaveId);

    checkNotFound(maybeSlave.isPresent(), "Couldn't find a slave in any state with id %s", slaveId);

    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), maybeSlave.get()), SingularityTransformHelpers.TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation("Retrieve the list of active tasks.")
  public Iterable<SingularityTask> getActiveTasks() {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getActiveTasks(), SingularityTransformHelpers.TASK_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/cleaning")
  @ApiOperation("Retrieve the list of cleaning tasks.")
  public Iterable<SingularityTaskCleanup> getCleaningTasks() {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getCleanupTasks(), SingularityTransformHelpers.TASK_CLEANUP_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @Path("/killed")
  @ApiOperation("Retrieve the list of killed tasks.")
  public Iterable<SingularityKilledTaskIdRecord> getKilledTasks() {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getKilledTaskIdRecords(), SingularityTransformHelpers.KILLED_TASK_ID_RECORD_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @ApiOperation("Retrieve the list of tasks being cleaned from load balancers.")
  public Iterable<SingularityTaskId> getLbCleanupTasks() {
    return authorizationHelper.filterByAuthorizedRequests(user, taskManager.getLBCleanupTasks(), SingularityTransformHelpers.TASK_ID_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  private SingularityTask checkActiveTask(String taskId, SingularityAuthorizationScope scope) {
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
  public SingularityTask getActiveTask(@PathParam("taskId") String taskId) {
    return checkActiveTask(taskId, SingularityAuthorizationScope.READ);
  }

  @GET
  @Path("/task/{taskId}/statistics")
  @ApiOperation("Retrieve statistics about a specific active task.")
  public MesosTaskStatisticsObject getTaskStatistics(@PathParam("taskId") String taskId) {
    SingularityTask task = checkActiveTask(taskId, SingularityAuthorizationScope.READ);

    String executorIdToMatch = null;

    if (task.getMesosTask().getExecutor().hasExecutorId()) {
      executorIdToMatch = task.getMesosTask().getExecutor().getExecutorId().getValue();
    } else {
      executorIdToMatch = taskId;
    }

    for (MesosTaskMonitorObject taskMonitor : mesosClient.getSlaveResourceUsage(task.getOffer().getHostname())) {
      if (taskMonitor.getExecutorId().equals(executorIdToMatch)) {
        return taskMonitor.getStatistics();
      }
    }

    throw notFound("Couldn't find executor %s for %s on slave %s", executorIdToMatch, taskId, task.getOffer().getHostname());
  }

  @GET
  @Path("/task/{taskId}/cleanup")
  @ApiOperation("Get the cleanup object for the task, if it exists")
  public Optional<SingularityTaskCleanup> getTaskCleanup(@PathParam("taskId") String taskId) {
    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.READ);

    return taskManager.getTaskCleanup(taskId);
  }

  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId) {
    return killTask(taskId, Optional.<SingularityKillTaskRequest> absent());
  }

  @DELETE
  @Path("/task/{taskId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Attempt to kill task, optionally overriding an existing cleanup request (that may be waiting for replacement tasks to become healthy)", response=SingularityTaskCleanup.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Task already has a cleanup request (can be overridden with override=true)")
  })
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId, Optional<SingularityKillTaskRequest> killTaskRequest) {
    final SingularityTask task = checkActiveTask(taskId, SingularityAuthorizationScope.WRITE);

    Optional<String> message = Optional.absent();
    Optional<Boolean> override = Optional.absent();
    Optional<String> actionId = Optional.absent();
    Optional<Boolean> waitForReplacementTask = Optional.absent();

    if (killTaskRequest.isPresent()) {
      actionId = killTaskRequest.get().getActionId();
      message = killTaskRequest.get().getMessage();
      override = killTaskRequest.get().getOverride();
      waitForReplacementTask = killTaskRequest.get().getWaitForReplacementTask();
    }

    TaskCleanupType cleanupType = TaskCleanupType.USER_REQUESTED;

    if (waitForReplacementTask.or(Boolean.FALSE)) {
      cleanupType = TaskCleanupType.USER_REQUESTED_TASK_BOUNCE;
    }

    final long now = System.currentTimeMillis();

    final SingularityTaskCleanup taskCleanup = new SingularityTaskCleanup(JavaUtils.getUserEmail(user), cleanupType, now,
        task.getTaskId(), message, actionId);

    if (override.isPresent() && override.get().booleanValue()) {
      taskManager.saveTaskCleanup(taskCleanup);
    } else {
      SingularityCreateResult result = taskManager.createTaskCleanup(taskCleanup);

      while (result == SingularityCreateResult.EXISTED) {
        Optional<SingularityTaskCleanup> cleanup = taskManager.getTaskCleanup(taskId);

        if (cleanup.isPresent()) {
          throw new WebApplicationException(Response.status(Status.CONFLICT).entity(cleanup.get()).type(MediaType.APPLICATION_JSON).build());
        }

        result = taskManager.createTaskCleanup(taskCleanup);
      }
    }

    if (cleanupType == TaskCleanupType.USER_REQUESTED_TASK_BOUNCE) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(task.getTaskId().getRequestId(), task.getTaskId().getDeployId(), now, JavaUtils.getUserEmail(user),
          PendingType.TASK_BOUNCE, Optional.<List<String>> absent(), Optional.<String> absent(), Optional.<Boolean> absent(), message, actionId));
    }

    return taskCleanup;
  }

  @Path("/commands/queued")
  @ApiOperation(value="Retrieve a list of all the shell commands queued for execution")
  public List<SingularityTaskShellCommandRequest> getQueuedShellCommands() {
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
  public void postTaskMetadata(@PathParam("taskId") String taskId, final SingularityTaskMetadataRequest taskMetadataRequest) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.WRITE);

    if (taskMetadataConfiguration.getAllowedMetadataTypes().isPresent()) {
      WebExceptions.checkBadRequest(taskMetadataConfiguration.getAllowedMetadataTypes().get().contains(taskMetadataRequest.getType()), "%s is not one of the allowed metadata types %s",
          taskMetadataRequest.getType(), taskMetadataConfiguration.getAllowedMetadataTypes().get());
    }

    WebExceptions.checkNotFound(taskManager.taskExistsInZk(taskIdObj), "Task {} not found in ZooKeeper (can not save metadata to tasks which have been persisted", taskIdObj);

    final SingularityTaskMetadata taskMetadata = new SingularityTaskMetadata(taskIdObj, System.currentTimeMillis(), taskMetadataRequest.getType(), taskMetadataRequest.getTitle(),
        taskMetadataRequest.getMessage(), JavaUtils.getUserEmail(user), taskMetadataRequest.getLevel());

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
  public SingularityTaskShellCommandRequest runShellCommand(@PathParam("taskId") String taskId, final SingularityShellCommand shellCommand) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    authorizationHelper.checkForAuthorizationByTaskId(taskId, user, SingularityAuthorizationScope.WRITE);

    if (!taskManager.isActiveTask(taskId)) {
      throw WebExceptions.badRequest("%s is not an active task, can't run %s on it", taskId, shellCommand.getName());
    }

    Optional<ShellCommandDescriptor> commandDescriptor = Iterables.tryFind(uiConfiguration.getShellCommands(), new Predicate<ShellCommandDescriptor>() {

      @Override
      public boolean apply(ShellCommandDescriptor input) {
        return input.getName().equals(shellCommand.getName());
      }
    });

    if (!commandDescriptor.isPresent()) {
      throw WebExceptions.forbidden("Shell command %s not in %s", shellCommand.getName(), uiConfiguration.getShellCommands());
    }

    Set<String> options = Sets.newHashSetWithExpectedSize(commandDescriptor.get().getOptions().size());
    for (ShellCommandOptionDescriptor option : commandDescriptor.get().getOptions()) {
      options.add(option.getName());
    }

    if (shellCommand.getOptions().isPresent()) {
      for (String option : shellCommand.getOptions().get()) {
        if (!options.contains(option)) {
          throw WebExceptions.badRequest("Shell command %s does not have option %s (%s)", shellCommand.getName(), option, options);
        }
      }
    }

    SingularityTaskShellCommandRequest shellRequest = new SingularityTaskShellCommandRequest(taskIdObj, JavaUtils.getUserEmail(user), System.currentTimeMillis(), shellCommand);

    taskManager.saveTaskShellCommandRequestToQueue(shellRequest);

    return shellRequest;
  }

}
