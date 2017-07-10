package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityPaginatedResponse;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

// Omits some or all @QueryParam annotated args, will be copied from request context
@Path(ApiPaths.HISTORY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class HistoryResource extends ProxyResource {

  @Inject
  public HistoryResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(configuration, httpClient, objectMapper);
  }

  @GET
  @Path("/task/{taskId}")
  public SingularityTaskHistory getHistoryForTask(@Context HttpServletRequest request, @PathParam("taskId") String taskId) {
    SingularityTaskId parsedId = SingularityTaskId.valueOf(taskId);
    return routeByRequestId(request, parsedId.getRequestId(), TypeRefs.TASK_HISTORY_REF);
  }


  @GET
  @Path("/request/{requestId}/tasks/active")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.TASK_ID_HISTORY_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}")
  public SingularityDeployHistory getDeploy(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId, TypeRefs.DEPLOY_HISTORY_REF);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/active")
  public List<SingularityTaskIdHistory> getActiveDeployTasks(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId, TypeRefs.TASK_ID_HISTORY_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive")
  public List<SingularityTaskIdHistory> getInactiveDeployTasks(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId, TypeRefs.TASK_ID_HISTORY_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/deploy/{deployId}/tasks/inactive/withmetadata")
  public SingularityPaginatedResponse<SingularityTaskIdHistory> getInactiveDeployTasksWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("deployId") String deployId) {
    return routeByRequestId(request, requestId, TypeRefs.PAGINATED_TASK_ID_HISTORY_REF);
  }

  @GET
  @Path("/tasks")
  public List<SingularityTaskIdHistory> getTaskHistory(
      @Context HttpServletRequest request, @QueryParam("requestId") Optional<String> requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    // TODO - what if requestId not present?
    return routeByRequestId(request, requestId.or(""), TypeRefs.TASK_ID_HISTORY_LIST_REF);
  }

  @GET
  @Path("/tasks/withmetadata")
  public SingularityPaginatedResponse<SingularityTaskIdHistory> getTaskHistoryWithMetadata(
      @Context HttpServletRequest request, @QueryParam("requestId") Optional<String> requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    // TODO - what if requestId not present?
    return routeByRequestId(request, requestId.or(""), TypeRefs.PAGINATED_TASK_ID_HISTORY_REF);
  }

  @GET
  @Path("/request/{requestId}/tasks")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    return routeByRequestId(request, requestId, TypeRefs.TASK_ID_HISTORY_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/tasks/withmetadata")
  public SingularityPaginatedResponse<SingularityTaskIdHistory> getTaskHistoryForRequestWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @QueryParam("deployId") Optional<String> deployId,
      @QueryParam("runId") Optional<String> runId, @QueryParam("host") Optional<String> host) {
    return routeByRequestId(request, requestId, TypeRefs.PAGINATED_TASK_ID_HISTORY_REF);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  public Optional<SingularityTaskIdHistory> getTaskHistoryForRequestAndRunId(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    return routeByRequestId(request, requestId, TypeRefs.OPTIONAL_TASK_ID_HISTORY_REF);
  }

  @GET
  @Path("/request/{requestId}/deploys")
  public List<SingularityDeployHistory> getDeploys(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.DEPLOY_HISTORY_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/deploys/withmetadata")
  public SingularityPaginatedResponse<SingularityDeployHistory> getDeploysWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.PAGINATED_DEPLOY_HISTORY_REF);
  }

  @GET
  @Path("/request/{requestId}/requests")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_HISTORY_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}/requests/withmetadata")
  public SingularityPaginatedResponse<SingularityRequestHistory> getRequestHistoryForRequestWithMetadata(
      @Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.PAGINATED_REQUEST_HISTORY_REF);
  }

  @GET
  @Path("/requests/search")
  public List<String> getRequestHistoryForRequestLike(@Context HttpServletRequest request, @QueryParam("requestIdLike") String requestIdLike) {
    return getMergedListResult(request, TypeRefs.LIST_STRING_REF);
  }

  @GET
  @Path("/request/{requestId}/command-line-args")
  public List<List<String>> getRecentCommandLineArgs(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return getMergedListResult(request, TypeRefs.LIST_LIST_STRING_REF);
  }
}
