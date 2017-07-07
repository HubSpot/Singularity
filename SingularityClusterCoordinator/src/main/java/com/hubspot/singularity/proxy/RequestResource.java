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
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
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

@Path(ApiPaths.REQUEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource {

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent postRequest(@Context HttpServletRequest requestContext, SingularityRequest request) {

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

  }

  @POST
  @Path("/request/{requestId}/run")
  public SingularityPendingRequestParent scheduleImmediately(@PathParam("requestId") String requestId) {
    return scheduleImmediately(requestId, null);
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityPendingRequestParent scheduleImmediately(@PathParam("requestId") String requestId,
                                                             SingularityRunNowRequest runNowRequest) {

  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  public Optional<SingularityTaskId> getTaskByRunId(@PathParam("requestId") String requestId, @PathParam("runId") String runId) {

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

  }

  @GET
  @Path("/active")
  public List<SingularityRequestParent> getActiveRequests(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/paused")
  public List<SingularityRequestParent> getPausedRequests(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/cooldown")
  public List<SingularityRequestParent> getCooldownRequests(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/finished")
  public List<SingularityRequestParent> getFinishedRequests(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  public List<SingularityRequestParent> getRequests(@QueryParam("useWebCache") Boolean useWebCache) {

  }

  @GET
  @Path("/queued/pending")
  public Iterable<SingularityPendingRequest> getPendingRequests() {

  }

  @GET
  @Path("/queued/cleanup")
  public Iterable<SingularityRequestCleanup> getCleanupRequests() {

  }

  @GET
  @Path("/request/{requestId}")
  public SingularityRequestParent getRequest(@PathParam("requestId") String requestId, @QueryParam("useWebCache") Boolean useWebCache) {

  }

  @DELETE
  @Path("/request/{requestId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequest deleteRequest(@PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          SingularityDeleteRequestRequest deleteRequest) {

  }

  @PUT
  @Path("/request/{requestId}/scale")
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent scale(@PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        SingularityScaleRequest scaleRequest) {

  }

  @DELETE
  @Path("/request/{requestId}/scale")
  public SingularityRequestParent deleteExpiringScale(@PathParam("requestId") String requestId) {

  }

  @Deprecated
  @DELETE
  @Path("/request/{requestId}/skipHealthchecks")
  public SingularityRequestParent deleteExpiringSkipHealthchecksDeprecated(@PathParam("requestId") String requestId) {

  }

  @DELETE
  @Path("/request/{requestId}/skip-healthchecks")
  public SingularityRequestParent deleteExpiringSkipHealthchecks(@PathParam("requestId") String requestId) {

  }

  @DELETE
  @Path("/request/{requestId}/pause")
  public SingularityRequestParent deleteExpiringPause(@PathParam("requestId") String requestId) {

  }

  @DELETE
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent deleteExpiringBounce(@PathParam("requestId") String requestId) {

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

  }

  @GET
  @Path("/lbcleanup")
  public Iterable<String> getLbCleanupRequests(@QueryParam("useWebCache") Boolean useWebCache) {

  }
}
