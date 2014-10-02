package com.hubspot.singularity.resources;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

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
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.SingularityTaskCleanupResult;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.sun.jersey.api.NotFoundException;

@Path(SingularityService.API_BASE_PATH + "/tasks")
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource {

  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final TaskRequestManager taskRequestManager;
  private final MesosClient mesosClient;

  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager, MesosClient mesosClient) {
    this.taskManager = taskManager;
    this.taskRequestManager = taskRequestManager;
    this.slaveManager = slaveManager;
    this.mesosClient = mesosClient;
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled")
  public List<SingularityTaskRequest> getScheduledTasks() {
    final List<SingularityPendingTask> tasks = taskManager.getPendingTasks();

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/ids")
  public List<SingularityPendingTaskId> getScheduledTaskIds() {
    return taskManager.getPendingTaskIds();
  }

  private SingularityPendingTaskId getTaskIdFromStr(String pendingTaskIdStr) {
    try {
      return SingularityPendingTaskId.fromString(pendingTaskIdStr);
    } catch (InvalidSingularityTaskIdException e) {
      throw WebExceptions.badRequest("%s is not a valid pending task id: %s", pendingTaskIdStr, e.getMessage());
    }
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/task/{pendingTaskId}")
  public SingularityTaskRequest getPendingTask(@PathParam("pendingTaskId") String pendingTaskIdStr) {
    SingularityPendingTask pendingTask = taskManager.getPendingTask(getTaskIdFromStr(pendingTaskIdStr));

    List<SingularityTaskRequest> taskRequestList = taskRequestManager.getTaskRequests(Collections.singletonList(pendingTask));

    if (taskRequestList.isEmpty()) {
      throw new NotFoundException("Couldn't find: " + pendingTaskIdStr);
    }

    return Iterables.getFirst(taskRequestList, null);
  }

  @GET
  @PropertyFiltering
  @Path("/scheduled/request/{requestId}")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@PathParam("requestId") String requestId) {
    final List<SingularityPendingTask> tasks = Lists.newArrayList(Iterables.filter(taskManager.getPendingTasks(), SingularityPendingTask.matchingRequest(requestId)));

    return taskRequestManager.getTaskRequests(tasks);
  }

  @GET
  @Path("active/slave/{slaveId}")
  public List<SingularityTask> getTasksForSlave(@PathParam("slaveId") String slaveId) {
    Optional<SingularitySlave> maybeSlave = slaveManager.getActiveObject(slaveId);

    if (!maybeSlave.isPresent()) {
      maybeSlave = slaveManager.getDecomissioning(slaveId);
    }

    if (!maybeSlave.isPresent()) {
      maybeSlave = slaveManager.getDeadObject(slaveId);
    }

    if (!maybeSlave.isPresent()) {
      throw new NotFoundException(String.format("Couldn't find a slave in any state with id %s", slaveId));
    }

    return taskManager.getTasksOnSlave(taskManager.getActiveTaskIds(), maybeSlave.get());
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  public List<SingularityTask> getActiveTasks() {
    return taskManager.getActiveTasks();
  }

  @GET
  @PropertyFiltering
  @Path("/cleaning")
  public List<SingularityTaskCleanup> getCleaningTasks() {
    return taskManager.getCleanupTasks();
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  public List<SingularityTaskId> getLbCleanupTasks() {
    return taskManager.getLBCleanupTasks();
  }

  @GET
  @Path("/task/{taskId}")
  public SingularityTask getActiveTask(@PathParam("taskId") String taskId) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);

    if (!task.isPresent()) {
      throw new NotFoundException(String.format("No active task with id %s", taskId));
    }

    return task.get();
  }

  @GET
  @Path("/task/{taskId}/statistics")
  public MesosTaskStatisticsObject getTaskStatistics(@PathParam("taskId") String taskId) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);

    if (!task.isPresent()) {
      throw new NotFoundException(String.format("No active task found in Singularity with id %s", taskId));
    }

    String executorIdToMatch = null;

    if (task.get().getMesosTask().getExecutor().hasExecutorId()) {
      executorIdToMatch = task.get().getMesosTask().getExecutor().getExecutorId().getValue();
    } else {
      executorIdToMatch = taskId;
    }

    for (MesosTaskMonitorObject taskMonitor : mesosClient.getSlaveResourceUsage(task.get().getOffer().getHostname())) {
      if (taskMonitor.getExecutorId().equals(executorIdToMatch)) {
        return taskMonitor.getStatistics();
      }
    }

    throw new NotFoundException(String.format("Couldn't find executor %s for %s on slave %s", executorIdToMatch, taskId, task.get().getOffer().getHostname()));
  }

  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanupResult killTask(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);

    if (!task.isPresent()) {
      throw new NotFoundException(String.format("Couldn't find active task with id %s", taskId));
    }

    final SingularityTaskCleanup taskCleanup = new SingularityTaskCleanup(user, TaskCleanupType.USER_REQUESTED, System.currentTimeMillis(), task.get().getTaskId());

    final SingularityCreateResult result = taskManager.createCleanupTask(taskCleanup);

    return new SingularityTaskCleanupResult(result, task.get());
  }

}
