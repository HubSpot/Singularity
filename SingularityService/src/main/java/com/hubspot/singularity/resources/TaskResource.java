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
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.config.shell.ShellCommandDescriptor;
import com.hubspot.singularity.config.shell.ShellCommandOptionDescriptor;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(TaskResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity tasks.", value=TaskResource.PATH)
public class TaskResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/tasks";

  private final MesosClient mesosClient;
  private final SlaveManager slaveManager;
  private final TaskManager taskManager;
  private final TaskRequestManager taskRequestManager;
  private final UIConfiguration uiConfiguration;

  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager, MesosClient mesosClient, UIConfiguration uiConfiguration) {
    this.taskManager = taskManager;
    this.taskRequestManager = taskRequestManager;
    this.slaveManager = slaveManager;
    this.mesosClient = mesosClient;

    this.uiConfiguration = uiConfiguration;
  }

  private SingularityTask checkActiveTask(String taskId) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    Optional<SingularityTask> task = taskManager.getTask(taskIdObj);

    checkNotFound(task.isPresent() && taskManager.isActiveTask(taskId), "No active task with id %s", taskId);

    return task.get();
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
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve information about a specific active task.")
  public SingularityTask getActiveTask(@PathParam("taskId") String taskId) {
    return checkActiveTask(taskId);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation("Retrieve the list of active tasks.")
  public List<SingularityTask> getActiveTasks() {
    return taskManager.getActiveTasks();
  }

  @GET
  @PropertyFiltering
  @Path("/cleaning")
  @ApiOperation("Retrieve the list of cleaning tasks.")
  public List<SingularityTaskCleanup> getCleaningTasks() {
    return taskManager.getCleanupTasks();
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @ApiOperation("Retrieve the list of tasks being cleaned from load balancers.")
  public List<SingularityTaskId> getLbCleanupTasks() {
    return taskManager.getLBCleanupTasks();
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

    return Iterables.getFirst(taskRequestList, null);
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/ids")
  @ApiOperation("Retrieve list of scheduled task IDs.")
  public List<SingularityPendingTaskId> getScheduledTaskIds() {
    return taskManager.getPendingTaskIds();
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled")
  @ApiOperation("Retrieve list of scheduled tasks.")
  public List<SingularityTaskRequest> getScheduledTasks() {
    final List<SingularityPendingTask> tasks = taskManager.getPendingTasks();

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/request/{requestId}")
  @ApiOperation("Retrieve list of scheduled tasks for a specific request.")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@PathParam("requestId") String requestId) {
    final List<SingularityPendingTask> tasks = Lists.newArrayList(Iterables.filter(taskManager.getPendingTasks(), SingularityPendingTask.matchingRequest(requestId)));

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @Path("/task/{taskId}/cleanup")
  @ApiOperation("Get the cleanup object for the task, if it exists")
  public Optional<SingularityTaskCleanup> getTaskCleanup(@PathParam("taskId") String taskId) {
    return taskManager.getTaskCleanup(taskId);
  }

  @GET
  @Path("/active/slave/{slaveId}")
  @ApiOperation("Retrieve list of active tasks on a specific slave.")
  public List<SingularityTask> getTasksForSlave(@PathParam("slaveId") String slaveId) {
    Optional<SingularitySlave> maybeSlave = slaveManager.getObject(slaveId);

    checkNotFound(maybeSlave.isPresent(), "Couldn't find a slave in any state with id %s", slaveId);

    return taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), maybeSlave.get());
  }

  @GET
  @Path("/task/{taskId}/statistics")
  @ApiOperation("Retrieve statistics about a specific active task.")
  public MesosTaskStatisticsObject getTaskStatistics(@PathParam("taskId") String taskId) {
    SingularityTask task = checkActiveTask(taskId);

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
  @Path("/commands/queued")
  @ApiOperation(value="Retrieve a list of all the shell commands queued for execution")
  public List<SingularityTaskShellCommandRequest> getQueuedShellCommands() {
    return taskManager.getAllQueuedTaskShellCommandRequests();
  }

  @POST
  @Path("/task/{taskId}/command")
  @ApiOperation(value="Run a configured shell command against the given task")
  @ApiResponses({
    @ApiResponse(code=400, message="Given shell command option doesn't exist"),
    @ApiResponse(code=403, message="Given shell command doesn't exist")
  })
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskShellCommandRequest runShellCommand(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> user, final SingularityShellCommand shellCommand) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

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

    for (String option : shellCommand.getOptions()) {
      if (!options.contains(option)) {
        throw WebExceptions.badRequest("Shell command %s does not have option %s (%s)", shellCommand.getName(), option, options);
      }
    }

    SingularityTaskShellCommandRequest shellRequest = new SingularityTaskShellCommandRequest(taskIdObj, user, System.currentTimeMillis(), shellCommand);

    taskManager.saveTaskShellCommandRequestToQueue(shellRequest);

    return shellRequest;
  }

  @DELETE
  @Path("/task/{taskId}")
  @ApiOperation(value="Attempt to kill task, optionally overriding an existing cleanup request (that may be waiting for replacement tasks to become healthy)", response=SingularityTaskCleanup.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Task already has a cleanup request (can be overridden with override=true)")
  })
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> user, @ApiParam("Pass true to save over any existing cleanup requests") @QueryParam("override") Optional<Boolean> override) {
    final SingularityTask task = checkActiveTask(taskId);

    final SingularityTaskCleanup taskCleanup = new SingularityTaskCleanup(user, TaskCleanupType.USER_REQUESTED, System.currentTimeMillis(), task.getTaskId(), Optional.<String> absent());

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

    return taskCleanup;
  }

}
