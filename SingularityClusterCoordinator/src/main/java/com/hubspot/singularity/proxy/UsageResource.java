package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;

@Path("/api/usage")
public class UsageResource extends ProxyResource {

  @Inject
  public UsageResource() {}

  @GET
  @Path("/slaves")
  public Response getSlavesWithUsage(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/slaves/{slaveId}/tasks/current")
  public Response getSlaveCurrentTaskUsage(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @GET
  @Path("/slaves/{slaveId}/history")
  public Response getSlaveUsageHistory(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @GET
  @Path("/tasks/{taskId}/history")
  public Response getTaskUsageHistory(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

}
