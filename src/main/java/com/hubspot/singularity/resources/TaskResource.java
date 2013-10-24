package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityHistory;
import com.hubspot.singularity.data.SingularityTask;
import com.hubspot.singularity.data.SingularityTaskId;
import com.hubspot.singularity.data.TaskManager;

@Path("/task")
@Consumes({ MediaType.APPLICATION_JSON })
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
  @Path("/pending")
  public List<SingularityTask> getPendingTasks() {
    final List<SingularityTaskId> taskIds = taskManager.getPendingTasks();
    
    return requestManager.fetchTasks(taskIds);
  }
  
  @GET
  @Path("/active")
  public List<SingularityTask> getActiveTasks() {
    return taskManager.getActiveTasks();
  }
  
  @GET
  @Path("/history/{taskId}")
  public List<SingularityHistory> getHistoryForTask(@PathParam("taskId") String taskId) {
    return taskManager.getHistory(taskId);
  }

}
