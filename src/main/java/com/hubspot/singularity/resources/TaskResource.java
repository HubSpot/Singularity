package com.hubspot.singularity.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.data.SingularityTask;
import com.hubspot.singularity.data.TaskManager;

@Path("/task")
@Consumes({ MediaType.APPLICATION_JSON })
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource {
  
  private final TaskManager taskManager;
  
  @Inject
  public TaskResource(TaskManager taskManager) {
    this.taskManager = taskManager;
  }

  @POST
  public SingularityTask submit(SingularityRequest request) {
    return taskManager.persistRequest(request);
  }

}
