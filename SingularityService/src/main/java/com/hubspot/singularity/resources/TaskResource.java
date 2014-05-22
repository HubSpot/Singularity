package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.*;
import com.hubspot.singularity.SingularityTaskCleanup.TaskCleanupType;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path(SingularityService.API_BASE_PATH + "/tasks")
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource {
  
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;  
  private final TaskRequestManager taskRequestManager;
  
  @Inject
  public TaskResource(TaskRequestManager taskRequestManager, TaskManager taskManager, SlaveManager slaveManager) {
    this.taskManager = taskManager;
    this.taskRequestManager = taskRequestManager;
    this.slaveManager = slaveManager;
  }
  
  @GET
  @PropertyFiltering
  @Path("/scheduled")
  public List<SingularityTaskRequest> getScheduledTasks() {
    final List<SingularityPendingTask> tasks = taskManager.getScheduledTasks();
    
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
  @Path("/task/{taskId}")
  public SingularityTask getActiveTask(@PathParam("taskId") String taskId) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);
  
    if (!task.isPresent()) {
      throw new NotFoundException(String.format("No active task with id %s", taskId));
    }
    
    return task.get();
  }
  
  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanupResult deleteTask(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);
    
    if (!task.isPresent()) {
      throw new NotFoundException(String.format("Couldn't find active task with id %s", taskId));
    }

    final SingularityTaskCleanup taskCleanup = new SingularityTaskCleanup(user, TaskCleanupType.USER_REQUESTED, System.currentTimeMillis(), task.get().getTaskId());
    
    final SingularityCreateResult result = taskManager.createCleanupTask(taskCleanup);

    return new SingularityTaskCleanupResult(result, task.get());
  }
  
}
