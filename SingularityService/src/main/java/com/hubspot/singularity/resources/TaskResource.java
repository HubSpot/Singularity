package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.*;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import java.util.List;

@Path("/tasks")
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource {
  
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final SlaveManager slaveManager;  
  
  @Inject
  public TaskResource(TaskManager taskManager, RequestManager requestManager, SlaveManager slaveManager) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.slaveManager = slaveManager;
  }
  
  @GET
  @Path("/scheduled")
  public List<SingularityTaskRequest> getScheduledTasks() {
    final List<SingularityPendingTaskId> taskIds = taskManager.getScheduledTasks();
    
    return requestManager.fetchTasks(taskIds);
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
  @Path("/active")
  public List<SingularityTask> getActiveTasks() {
    return taskManager.getActiveTasks();
  }
  
  @GET
  @Path("/cleaning")
  public List<SingularityTaskCleanup> getCleaningTasks() {
    return taskManager.getCleanupTasks();
  }
  
  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanupResult deleteTask(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);
    
    if (!task.isPresent()) {
      throw new NotFoundException(String.format("Couldn't find active task with id %s", taskId));
    }

    final SingularityTaskCleanup taskCleanup = new SingularityTaskCleanup(user, TaskCleanupType.USER_REQUESTED, System.currentTimeMillis(), taskId, task.get().getTaskRequest().getRequest().getId());
    
    final SingularityCreateResult result = taskManager.createCleanupTask(taskCleanup);

    return new SingularityTaskCleanupResult(result, task.get());
  }
  
}
