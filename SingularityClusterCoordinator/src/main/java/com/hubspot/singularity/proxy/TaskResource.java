package com.hubspot.singularity.proxy;

import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityTaskMetadataRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.TEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class TaskResource extends ProxyResource {

  @Inject
  public TaskResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/scheduled")
  public List<SingularityTaskRequest> getScheduledTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.TASK_REQUEST_LIST_REF);
  }

  @GET
  @Path("/scheduled/ids")
  public List<SingularityPendingTaskId> getScheduledTaskIds(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.PENDING_TASK_ID_LIST_REF);
  }

  @GET
  @Path("/scheduled/task/{pendingTaskId}")
  public SingularityTaskRequest getPendingTask(@Context HttpServletRequest request, @PathParam("pendingTaskId") String pendingTaskIdStr) {
    SingularityPendingTaskId parsedId = SingularityPendingTaskId.valueOf(pendingTaskIdStr);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.TASK_REQUEST_REF);
  }

  @GET
  @Path("/scheduled/request/{requestId}")
  public List<SingularityTaskRequest> getScheduledTasksForRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.TASK_REQUEST_LIST_REF);
  }

  @GET
  @Path("/active/slave/{slaveId}")
  public List<SingularityTask> getTasksForSlave(@Context HttpServletRequest request, @PathParam("slaveId") String slaveId) {
    return routeBySlaveId(request, slaveId, TypeRefs.TASK_LIST_REF);
  }

  @GET
  @Path("/active")
  public List<SingularityTask> getActiveTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.TASK_LIST_REF);
  }

  @GET
  @Path("/cleaning")
  public List<SingularityTaskCleanup> getCleaningTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.TASK_CLEANUP_LIST_REF);
  }

  @GET
  @Path("/killed")
  public List<SingularityKilledTaskIdRecord> getKilledTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.TASK_KILLED_LIST_REF);
  }

  @GET
  @Path("/lbcleanup")
  public List<SingularityTaskId> getLbCleanupTasks(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.TASK_ID_LIST_REF);
  }

  @GET
  @Path("/task/{taskId}")
  public SingularityTask getActiveTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.TASK_REF);
  }

  @GET
  @Path("/task/{taskId}/statistics")
  public MesosTaskStatisticsObject getTaskStatistics(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.TASK_STATISTICS_REF);
  }

  @GET
  @Path("/task/{taskId}/cleanup")
  public Optional<SingularityTaskCleanup> getTaskCleanup(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.OPTIONAL_TASK_CLEANUP_REF);
  }

  @DELETE
  @Path("/task/{taskId}")
  public SingularityTaskCleanup killTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId, @Context HttpServletRequest requestContext) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.TASK_CLEANUP_REF);
  }

  @DELETE
  @Path("/task/{taskId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskCleanup killTask(@PathParam("taskId") String taskId,
                                         @Context HttpServletRequest requestContext,
                                         SingularityKillTaskRequest killTaskRequest) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(requestContext, parsedId.getRequestId(), killTaskRequest, TypeRefs.TASK_CLEANUP_REF);
  }

  @Path("/commands/queued")
  public List<SingularityTaskShellCommandRequest> getQueuedShellCommands(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.SHELL_COMMAND_LIST_REF);
  }

  @POST
  @Path("/task/{taskId}/metadata")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response postTaskMetadata(@Context HttpServletRequest request, @PathParam("taskId") String taskId, final SingularityTaskMetadataRequest taskMetadataRequest) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.RESPONSE_REF);
  }

  @POST
  @Path("/task/{taskId}/command")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityTaskShellCommandRequest runShellCommand(@Context HttpServletRequest request, @PathParam("taskId") String taskId, final SingularityShellCommand shellCommand) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.SHELL_COMMAND_REF);
  }
}
