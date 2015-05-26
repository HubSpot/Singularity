package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.notFound;
import static com.hubspot.singularity.data.SingularityValidator.userIsAuthorizedForRequest;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
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
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
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

  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final TaskRequestManager taskRequestManager;
  private final MesosClient mesosClient;
  private final RequestManager requestManager;
  private final SingularityValidator validator;
  private final Optional<SingularityUser> user;

  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager, MesosClient mesosClient,
                      RequestManager requestManager, SingularityValidator validator, Optional<SingularityUser> user) {
    this.taskManager = taskManager;
    this.taskRequestManager = taskRequestManager;
    this.slaveManager = slaveManager;
    this.mesosClient = mesosClient;
    this.requestManager = requestManager;
    this.validator = validator;
    this.user = user;
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled")
  @ApiOperation("Retrieve list of scheduled tasks.")
  public Iterable<SingularityTaskRequest> getScheduledTasks() {
    final List<SingularityPendingTask> tasks = taskManager.getPendingTasks();

    return Iterables.filter(taskRequestManager.getTaskRequests(tasks), new Predicate<SingularityTaskRequest>() {
      @Override
      public boolean apply(SingularityTaskRequest input) {
        return userIsAuthorizedForRequest(user, input.getRequest());
      }
    });
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/ids")
  @ApiOperation("Retrieve list of scheduled task IDs.")
  public Iterable<SingularityPendingTaskId> getScheduledTaskIds() {
    return Iterables.filter(taskManager.getPendingTaskIds(), new Predicate<SingularityPendingTaskId>() {
      @Override
      public boolean apply(SingularityPendingTaskId input) {
        return userIsAuthorizedForRequest(user, requestManager.getRequest(input.getRequestId()));
      }
    });
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

    validator.checkForAuthorization(taskRequestList.get(0).getRequest(), Optional.<SingularityRequest>absent(), user);

    return Iterables.getFirst(taskRequestList, null);
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/request/{requestId}")
  @ApiOperation("Retrieve list of scheduled tasks for a specific request.")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@PathParam("requestId") String requestId) {
    validator.checkForAuthorizationByRequestId(requestId, user);

    final List<SingularityPendingTask> tasks = Lists.newArrayList(Iterables.filter(taskManager.getPendingTasks(), SingularityPendingTask.matchingRequest(requestId)));

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @Path("/active/slave/{slaveId}")
  @ApiOperation("Retrieve list of active tasks on a specific slave.")
  public Iterable<SingularityTask> getTasksForSlave(@PathParam("slaveId") String slaveId) {
    Optional<SingularitySlave> maybeSlave = slaveManager.getObject(slaveId);

    checkNotFound(maybeSlave.isPresent(), "Couldn't find a slave in any state with id %s", slaveId);

    return Iterables.filter(taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), maybeSlave.get()), new Predicate<SingularityTask>() {
      @Override
      public boolean apply(SingularityTask input) {
        return userIsAuthorizedForRequest(user, input.getTaskRequest().getRequest());
      }
    });
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation("Retrieve the list of active tasks.")
  public Iterable<SingularityTask> getActiveTasks() {
    return Iterables.filter(taskManager.getActiveTasks(), new Predicate<SingularityTask>() {
      @Override
      public boolean apply(SingularityTask input) {
        return userIsAuthorizedForRequest(user, input.getTaskRequest().getRequest());
      }
    });
  }

  @GET
  @PropertyFiltering
  @Path("/cleaning")
  @ApiOperation("Retrieve the list of cleaning tasks.")
  public Iterable<SingularityTaskCleanup> getCleaningTasks() {
    return Iterables.filter(taskManager.getCleanupTasks(), new Predicate<SingularityTaskCleanup>() {
      @Override
      public boolean apply(SingularityTaskCleanup input) {
        return userIsAuthorizedForRequest(user, requestManager.getRequest(input.getTaskId().getRequestId()));
      }
    });
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @ApiOperation("Retrieve the list of tasks being cleaned from load balancers.")
  public Iterable<SingularityTaskId> getLbCleanupTasks() {
    return Iterables.filter(taskManager.getLBCleanupTasks(), new Predicate<SingularityTaskId>() {
      @Override
      public boolean apply(SingularityTaskId input) {
        return userIsAuthorizedForRequest(user, requestManager.getRequest(input.getRequestId()));
      }
    });
  }

  private SingularityTask checkActiveTask(String taskId) {
    SingularityTaskId taskIdObj = getTaskIdFromStr(taskId);

    Optional<SingularityTask> task = taskManager.getTask(taskIdObj);

    checkNotFound(task.isPresent() && taskManager.isActiveTask(taskId), "No active task with id %s", taskId);

    if (task.isPresent()) {
      validator.checkForAuthorization(task.get(), user);
    }

    return task.get();
  }

  @GET
  @Path("/task/{taskId}")
  @ApiOperation("Retrieve information about a specific active task.")
  public SingularityTask getActiveTask(@PathParam("taskId") String taskId) {
    return checkActiveTask(taskId);
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
  @Path("/task/{taskId}/cleanup")
  @ApiOperation("Get the cleanup object for the task, if it exists")
  public Optional<SingularityTaskCleanup> getTaskCleanup(@PathParam("taskId") String taskId) {
    validator.checkForAuthorizationByTaskId(taskId, user);

    return taskManager.getTaskCleanup(taskId);
  }

  @DELETE
  @Path("/task/{taskId}")
  @ApiOperation(value="Attempt to kill task, optionally overriding an existing cleanup request (that may be waiting for replacement tasks to become healthy)", response=SingularityTaskCleanup.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Task already has a cleanup request (can be overridden with override=true)")
  })
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> queryUser, @ApiParam("Pass true to save over any existing cleanup requests") @QueryParam("override") Optional<Boolean> override) {
    final SingularityTask task = checkActiveTask(taskId);

    final SingularityTaskCleanup taskCleanup = new SingularityTaskCleanup(queryUser, TaskCleanupType.USER_REQUESTED, System.currentTimeMillis(), task.getTaskId(), Optional.<String> absent());

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
