package com.hubspot.singularity.proxy;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.ApiPaths;

// Omits some or all @QueryParam annotated args, will be copied from request context
@Path(ApiPaths.HISTORY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class HistoryResource extends ProxyResource {

  @Inject
  public HistoryResource() {}

  @GET
  @Path("/task/{taskId}")
  public Response getHistoryForTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId());
  }


  @GET
  @Path("/request/{requestId}/tasks/active")
  public Response getTaskHistoryForRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  public Response getDeploy(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/active")
  public Response getActiveDeployTasks(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive")
  public Response getInactiveDeployTasks(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive/withmetadata")
  public Response getInactiveDeployTasksWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/tasks")
  public Response getTaskHistory(
      @Context HttpServletRequest request, @QueryParam("requestId") Optional<String> requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    // TODO - what if requestId not present?
    return routeByRequestId(request, requestId.or(""));
  }

  @GET
  @Path("/tasks/withmetadata")
  public Response getTaskHistoryWithMetadata(
      @Context HttpServletRequest request, @QueryParam("requestId") Optional<String> requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    // TODO - what if requestId not present?
    return routeByRequestId(request, requestId.or(""));
  }

  @GET
  @Path("/request/{requestId}/tasks")
  public Response getTaskHistoryForRequest(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/tasks/withmetadata")
  public Response getTaskHistoryForRequestWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  public Response getTaskHistoryForRequestAndRunId(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploys")
  public Response getDeploys(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/deploys/withmetadata")
  public Response getDeploysWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/requests")
  public Response getRequestHistoryForRequest(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/request/{requestId}/requests/withmetadata")
  public Response getRequestHistoryForRequestWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @GET
  @Path("/requests/search")
  public Response getRequestHistoryForRequestLike(@Context HttpServletRequest request, @QueryParam("requestIdLike") String requestIdLike) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/request/{requestId}/command-line-args")
  public Response getRecentCommandLineArgs(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return getMergedListResult(request);
  }
}
