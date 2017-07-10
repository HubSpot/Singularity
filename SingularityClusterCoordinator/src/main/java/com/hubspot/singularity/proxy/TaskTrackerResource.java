package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.TASK_TRACKER_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class TaskTrackerResource extends ProxyResource {

  @Inject
  public TaskTrackerResource() {}

  @GET
  @Path("/task/{taskId}")
  public Optional<SingularityTaskState> getTaskState(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.OPTIONAL_TASK_STATE_REF);
  }

  @GET
  @Path("/run/{requestId}/{runId}")
  public Optional<SingularityTaskState> getTaskStateByRunId(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    return routeByRequestId(request, requestId, TypeRefs.OPTIONAL_TASK_STATE_REF);
  }
}
