package com.hubspot.singularity.proxy;

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
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityExitCooldownRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.DataCenter;
import com.hubspot.singularity.exceptions.DataCenterConflictException;
import com.hubspot.singularity.exceptions.DataCenterNotFoundException;

@Path(ApiPaths.REQUEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource extends ProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  @Inject
  public RequestResource() {}

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response postRequest(@Context HttpServletRequest requestContext, SingularityRequest request) {
    Optional<DataCenter> maybeDataCenter = dataCenterLocator.getMaybeDataCenterForRequest(request.getId(), false);
    if (maybeDataCenter.isPresent()) {
      LOG.trace("Found data center {} for request {}", maybeDataCenter.get().getName(), request.getId());
      if (request.getDataCenter().isPresent() && !request.getDataCenter().get().equals(maybeDataCenter.get().getName())) {
        throw new DataCenterConflictException(String.format("Cannot create request with id %s in multiple datacenters (requested: %s), already found in %s", request.getId(), request.getDataCenter().get(), maybeDataCenter.get().getName()));
      }
      return routeByDataCenter(requestContext, maybeDataCenter.get().getName(), request);
    }

    LOG.trace("Check request {}", request);
    if (request.getDataCenter().isPresent()) {
      return routeByDataCenter(requestContext, request.getDataCenter().get(), request);
    }
    if (configuration.isErrorOnDataCenterNotSpecified()) {
      throw new DataCenterNotFoundException(String.format("No data center specified in request %s, and no existing request found in any data center", request.getId()), 500);
    } else {
      return routeToDefaultDataCenter(requestContext, request);
    }
  }

  @POST
  @Path("/request/{requestId}/bounce")
  public Response bounce(@PathParam("requestId") String requestId,
                                         @Context HttpServletRequest requestContext) {
    return bounce(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response bounce(@PathParam("requestId") String requestId,
                                         @Context HttpServletRequest requestContext,
                                         SingularityBounceRequest bounceRequest) {
    return routeByRequestId(requestContext, requestId, bounceRequest);
  }

  @POST
  @Path("/request/{requestId}/run")
  public Response scheduleImmediately(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return scheduleImmediately(request, requestId, null);
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response scheduleImmediately(@Context HttpServletRequest requestContext, @PathParam("requestId") String requestId,
                                                             SingularityRunNowRequest runNowRequest) {
    return routeByRequestId(requestContext, requestId, runNowRequest);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  public Response getTaskByRunId(@Context HttpServletRequest request, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    return routeByRequestId(request, requestId);
  }

  @POST
  @Path("/request/{requestId}/pause")
  public Response pause(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext) {
    return pause(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/pause")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response pause(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        SingularityPauseRequest pauseRequest) {
    return routeByRequestId(requestContext, requestId, pauseRequest);
  }

  @POST
  @Path("/request/{requestId}/unpause")
  public Response unpauseNoBody(@PathParam("requestId") String requestId,
                                                @Context HttpServletRequest requestContext) {
    return unpause(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/unpause")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response unpause(@PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          SingularityUnpauseRequest unpauseRequest) {
    return routeByRequestId(requestContext, requestId, unpauseRequest);
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  public Response exitCooldown(@PathParam("requestId") String requestId,
                                               @Context HttpServletRequest requestContext) {
    return exitCooldown(requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response exitCooldown(@PathParam("requestId") String requestId,
                                               @Context HttpServletRequest requestContext,
                                               SingularityExitCooldownRequest exitCooldownRequest) {
    return routeByRequestId(requestContext, requestId, exitCooldownRequest);
  }

  @GET
  @Path("/active")
  public Response getActiveRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/paused")
  public Response getPausedRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/cooldown")
  public Response getCooldownRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/finished")
  public Response getFinishedRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  public Response getRequests(@Context HttpServletRequest request) {
    // TODO - update internal cache list?
    return getMergedListResult(request);
  }

  @GET
  @Path("/queued/pending")
  public Response getPendingRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/queued/cleanup")
  public Response getCleanupRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }

  @GET
  @Path("/request/{requestId}")
  public Response getRequest(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @DELETE
  @Path("/request/{requestId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response deleteRequest(@PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          SingularityDeleteRequestRequest deleteRequest) {
    // TODO - update internal list?
    return routeByRequestId(requestContext, requestId, deleteRequest);
  }

  @PUT
  @Path("/request/{requestId}/scale")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response scale(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        SingularityScaleRequest scaleRequest) {
    return routeByRequestId(requestContext, requestId, scaleRequest);
  }

  @DELETE
  @Path("/request/{requestId}/scale")
  public Response deleteExpiringScale(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @Deprecated
  @DELETE
  @Path("/request/{requestId}/skipHealthchecks")
  public Response deleteExpiringSkipHealthchecksDeprecated(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/skip-healthchecks")
  public Response deleteExpiringSkipHealthchecks(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/pause")
  public Response deleteExpiringPause(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/bounce")
  public Response deleteExpiringBounce(@Context HttpServletRequest request, @PathParam("requestId") String requestId) {
    return routeByRequestId(request, requestId);
  }

  @Deprecated
  @PUT
  @Path("/request/{requestId}/skipHealthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response skipHealthchecksDeprecated(@PathParam("requestId") String requestId,
                                                             @Context HttpServletRequest requestContext,
                                                             SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return skipHealthchecks(requestId, requestContext, skipHealthchecksRequest);
  }

  @PUT
  @Path("/request/{requestId}/skip-healthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response skipHealthchecks(@PathParam("requestId") String requestId,
                                                   @Context HttpServletRequest requestContext,
                                                   SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return routeByRequestId(requestContext, requestId, skipHealthchecksRequest);
  }

  @GET
  @Path("/lbcleanup")
  public Response getLbCleanupRequests(@Context HttpServletRequest request) {
    return getMergedListResult(request);
  }
}
