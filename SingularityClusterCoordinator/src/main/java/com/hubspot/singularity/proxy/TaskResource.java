package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityTaskMetadataRequest;
import com.hubspot.singularity.config.ApiPaths;

@Path(ApiPaths.TASK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource extends ProxyResource {

  @Inject
  public TaskResource() {}

  @GET
  @Path("/scheduled")
  public Response getScheduledTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/scheduled/ids")
  public Response getScheduledTaskIds(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/scheduled/task/{pendingTaskId}")
  public Response getPendingTask(@Context HttpServletRequest request, @PathParam("pendingTaskId") String pendingTaskIdStr) {
    SingularityPendingTaskId parsedId = SingularityPendingTaskId.valueOf(pendingTaskIdStr);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @GET
  @Path("/scheduled/request/{requestId}")
  public Response getScheduledTasksForRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/active/slave/{slaveId}")
  public Response getTasksForSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId);
  }

  @GET
  @Path("/active")
  public Response getActiveTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/cleaning")
  public Response getCleaningTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/killed")
  public Response getKilledTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/lbcleanup")
  public Response getLbCleanupTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/task/{taskId}")
  public Response getActiveTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @GET
  @Path("/task/{taskId}/statistics")
  public Response getTaskStatistics(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @GET
  @Path("/task/{taskId}/cleanup")
  public Response getTaskCleanup(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @DELETE
  @Path("/task/{taskId}")
  public Response killTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId, @Context HttpServletRequest requestContext) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }

  @DELETE
  @Path("/task/{taskId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response killTask(@PathParam("taskId") String taskId,
                                         @Context HttpServletRequest requestContext,
                                         SingularityKillTaskRequest killTaskRequest) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(requestContext, parsedId.getRequestId(), killTaskRequest);
  }

  @GET
  @Path("/commands/queued")
  public Response getQueuedShellCommands(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @POST
  @Path("/task/{taskId}/metadata")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response postTaskMetadata(@Context HttpServletRequest request, @PathParam("taskId") String taskId, final SingularityTaskMetadataRequest taskMetadataRequest) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), taskMetadataRequest);
  }

  @POST
  @Path("/task/{taskId}/command")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response runShellCommand(@Context HttpServletRequest request, @PathParam("taskId") String taskId, final SingularityShellCommand shellCommand) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), shellCommand);
  }
}
