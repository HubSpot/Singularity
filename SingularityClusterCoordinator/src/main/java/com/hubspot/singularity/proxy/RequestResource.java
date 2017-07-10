package com.hubspot.singularity.proxy;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequestParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityExitCooldownRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

@Path(ApiPaths.REQUEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource extends ProxyResource {

  @Inject
  public RequestResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper, DataCenterLocator dataCenterLocator) {
    super(configuration, httpClient, objectMapper, dataCenterLocator);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent postRequest(@Context HttpServletRequest requestContext, SingularityRequest request) {
    // TODO - where to create new requests?
    if (request.getDataCenter().isPresent()) {
      return routeByDataCenter(requestContext, request.getDataCenter().get(), request, TypeRefs.REQUEST_PARENT_REF);
    } else {
      return routeByRequestId(requestContext, request.getId(), request, TypeRefs.REQUEST_PARENT_REF);
    }
  }

  @POST
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent bounce(@PathParam("requestId") String requestId,
                                         @Context HttpServletRequest requestContext) {
    return bounce(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent bounce(@PathParam("requestId") String requestId,
                                         @Context HttpServletRequest requestContext,
                                         SingularityBounceRequest bounceRequest) {
    return routeByRequestId(requestContext, requestId, bounceRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @POST
  @Path("/request/{requestId}/run")
  public SingularityPendingRequestParent scheduleImmediately(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return scheduleImmediately(request, requestId, null);
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityPendingRequestParent scheduleImmediately(@Context HttpServletRequest requestContext, @PathParam("requestId") String requestId,
                                                             SingularityRunNowRequest runNowRequest) {
    return routeByRequestId(requestContext, requestId, runNowRequest, TypeRefs.PENDING_REQUEST_PARENT_REF);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  public Optional<SingularityTaskId> getTaskByRunId(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    return routeByRequestId(request, requestId, TypeRefs.OPTIONAL_TASK_ID_REF);
  }

  @POST
  @Path("/request/{requestId}/pause")
  public SingularityRequestParent pause(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext) {
    return pause(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/pause")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent pause(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        SingularityPauseRequest pauseRequest) {
    return routeByRequestId(requestContext, requestId, pauseRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @POST
  @Path("/request/{requestId}/unpause")
  public SingularityRequestParent unpauseNoBody(@PathParam("requestId") String requestId,
                                                @Context HttpServletRequest requestContext) {
    return unpause(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/unpause")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent unpause(@PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          SingularityUnpauseRequest unpauseRequest) {
    return routeByRequestId(requestContext, requestId, unpauseRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  public SingularityRequestParent exitCooldown(@PathParam("requestId") String requestId,
                                               @Context HttpServletRequest requestContext) {
    return exitCooldown(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent exitCooldown(@PathParam("requestId") String requestId,
                                               @Context HttpServletRequest requestContext,
                                               SingularityExitCooldownRequest exitCooldownRequest) {
    return routeByRequestId(requestContext, requestId, exitCooldownRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @GET
  @Path("/active")
  public List<SingularityRequestParent> getActiveRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.REQUEST_PARENT_LIST_REF);
  }

  @GET
  @Path("/paused")
  public List<SingularityRequestParent> getPausedRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.REQUEST_PARENT_LIST_REF);
  }

  @GET
  @Path("/cooldown")
  public List<SingularityRequestParent> getCooldownRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.REQUEST_PARENT_LIST_REF);
  }

  @GET
  @Path("/finished")
  public List<SingularityRequestParent> getFinishedRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.REQUEST_PARENT_LIST_REF);
  }

  @GET
  public List<SingularityRequestParent> getRequests(@Context HttpServletRequest request) {
    // TODO - update internal cache list?
    return getMergedListResult(request, TypeRefs.REQUEST_PARENT_LIST_REF);
  }

  @GET
  @Path("/queued/pending")
  public List<SingularityPendingRequest> getPendingRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.PENDING_REQUEST_LIST_REF);
  }

  @GET
  @Path("/queued/cleanup")
  public List<SingularityRequestCleanup> getCleanupRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.REQUEST_CLEANUP_LIST_REF);
  }

  @GET
  @Path("/request/{requestId}")
  public SingularityRequestParent getRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @DELETE
  @Path("/request/{requestId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequest deleteRequest(@PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          SingularityDeleteRequestRequest deleteRequest) {
    // TODO - update internal list?
    return routeByRequestId(requestContext, requestId, deleteRequest, TypeRefs.REQUEST_REF);
  }

  @PUT
  @Path("/request/{requestId}/scale")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent scale(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        SingularityScaleRequest scaleRequest) {
    return routeByRequestId(requestContext, requestId, scaleRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @DELETE
  @Path("/request/{requestId}/scale")
  public SingularityRequestParent deleteExpiringScale(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @Deprecated
  @DELETE
  @Path("/request/{requestId}/skipHealthchecks")
  public SingularityRequestParent deleteExpiringSkipHealthchecksDeprecated(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @DELETE
  @Path("/request/{requestId}/skip-healthchecks")
  public SingularityRequestParent deleteExpiringSkipHealthchecks(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @DELETE
  @Path("/request/{requestId}/pause")
  public SingularityRequestParent deleteExpiringPause(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @DELETE
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent deleteExpiringBounce(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId, TypeRefs.REQUEST_PARENT_REF);
  }

  @Deprecated
  @PUT
  @Path("/request/{requestId}/skipHealthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent skipHealthchecksDeprecated(@PathParam("requestId") String requestId,
                                                             @Context HttpServletRequest requestContext,
                                                             SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return skipHealthchecks(requestId, requestContext, skipHealthchecksRequest);
  }

  @PUT
  @Path("/request/{requestId}/skip-healthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent skipHealthchecks(@PathParam("requestId") String requestId,
                                                   @Context HttpServletRequest requestContext,
                                                   SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return routeByRequestId(requestContext, requestId, skipHealthchecksRequest, TypeRefs.REQUEST_PARENT_REF);
  }

  @GET
  @Path("/lbcleanup")
  public List<String> getLbCleanupRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request, TypeRefs.LIST_STRING_REF);
  }
}
