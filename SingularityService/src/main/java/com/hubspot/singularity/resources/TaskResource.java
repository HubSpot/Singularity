package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCleanup.CleanupType;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.sun.jersey.api.NotFoundException;

@Path("/tasks")
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource {
  
  private final TaskManager taskManager;
  private final RequestManager requestManager;
    
  @Inject
  public TaskResource(TaskManager taskManager, RequestManager requestManager) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
  }
  
  @GET
  @Path("/scheduled")
  public List<SingularityTaskRequest> getScheduledTasks() {
    final List<SingularityPendingTaskId> taskIds = taskManager.getScheduledTasks();
    
    return requestManager.fetchTasks(taskIds);
  }
  
  @GET
  @Path("/active")
  public List<SingularityTask> getActiveTasks() {
    return taskManager.getActiveTasks();
  }
  
  @DELETE
  @Path("/task/{taskId}")
  public String deleteTask(@PathParam("taskId") String taskId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityTask> task = taskManager.getActiveTask(taskId);
    
    if (!task.isPresent()) {
      throw new NotFoundException(String.format("Couldn't find active task with id %s", taskId));
    }
    
    return taskManager.createCleanupTask(new SingularityTaskCleanup(user, CleanupType.USER_REQUESTED, System.currentTimeMillis(), taskId, task.get().getTaskRequest().getRequest().getId())).name();
  }
  
}
