package com.hubspot.singularity.proxy;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.TASK_TRACKER_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class TaskTrackerResource {

  @GET
  @Path("/task/{taskId}")
  public Optional<SingularityTaskState> getTaskState(@PathParam("taskId") String taskId) {

  }

  @GET
  @Path("/run/{requestId}/{runId}")
  public Optional<SingularityTaskState> getTaskStateByRunId(@PathParam("requestId") String requestId, @PathParam("runId") String runId) {

  }
}
