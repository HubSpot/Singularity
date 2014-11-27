package com.hubspot.singularity.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestInstances;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(RequestResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity Requests, the parent object for any deployed task", value=RequestResource.PATH, position=1)
public class RequestResource extends AbstractRequestResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/requests";

  private final SingularityValidator validator;

  private final SingularityMailer mailer;
  private final RequestManager requestManager;
  private final DeployManager deployManager;

  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, RequestManager requestManager, SingularityMailer mailer) {
    super(requestManager, deployManager);

    this.validator = validator;
    this.mailer = mailer;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
  }

  private static class SingularityRequestDeployHolder {

    private final Optional<SingularityDeploy> activeDeploy;
    private final Optional<SingularityDeploy> pendingDeploy;

    public SingularityRequestDeployHolder(Optional<SingularityDeploy> activeDeploy, Optional<SingularityDeploy> pendingDeploy) {
      this.activeDeploy = activeDeploy;
      this.pendingDeploy = pendingDeploy;
    }

    public Optional<SingularityDeploy> getActiveDeploy() {
      return activeDeploy;
    }

    public Optional<SingularityDeploy> getPendingDeploy() {
      return pendingDeploy;
    }

  }

  private SingularityRequestDeployHolder getDeployHolder(String requestId) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestId);

    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();

    if (requestDeployState.isPresent()) {
      if (requestDeployState.get().getActiveDeploy().isPresent()) {
        activeDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getActiveDeploy().get().getDeployId());
      }
      if (requestDeployState.get().getPendingDeploy().isPresent()) {
        pendingDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getPendingDeploy().get().getDeployId());
      }
    }

    return new SingularityRequestDeployHolder(activeDeploy, pendingDeploy);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Create or update a Singularity Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Request object is invalid"),
    @ApiResponse(code=409, message="Request object is being cleaned. Try again shortly"),
  })
  public SingularityRequestParent submit(@ApiParam("The Singularity request to create or update") SingularityRequest request,
      @ApiParam("Username of the person requesting to create or update") @QueryParam("user") Optional<String> user) {
    if (request.getId() == null) {
      throw WebExceptions.badRequest("Request must have an id");
    }

    Optional<SingularityRequestWithState> maybeOldRequestWithState = requestManager.getRequest(request.getId());
    Optional<SingularityRequest> maybeOldRequest = maybeOldRequestWithState.isPresent() ? Optional.of(maybeOldRequestWithState.get().getRequest()) : Optional.<SingularityRequest> absent();

    SingularityRequestDeployHolder deployHolder = getDeployHolder(request.getId());

    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy());

    if (!maybeOldRequest.isPresent() && requestManager.getCleanupRequest(request.getId()).isPresent()) {
      throw WebExceptions.conflict("Request %s is currently cleaning. Try again after a few moments", request.getId());
    }

    requestManager.activate(newRequest, maybeOldRequest.isPresent() ? RequestHistoryType.UPDATED : RequestHistoryType.CREATED, user);

    checkReschedule(newRequest, maybeOldRequest);

    return fillEntireRequest(fetchRequestWithState(request.getId()));
  }

  private void checkReschedule(SingularityRequest newRequest, Optional<SingularityRequest> maybeOldRequest) {
    if (!maybeOldRequest.isPresent()) {
      return;
    }

    if (shouldReschedule(newRequest, maybeOldRequest.get())) {
      Optional<String> maybeDeployId = deployManager.getInUseDeployId(newRequest.getId());

      if (maybeDeployId.isPresent()) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), PendingType.UPDATED_REQUEST));
      }
    }
  }

  private boolean shouldReschedule(SingularityRequest newRequest, SingularityRequest oldRequest) {
    if (newRequest.getInstancesSafe() != oldRequest.getInstancesSafe()) {
      return true;
    }
    if (newRequest.isScheduled() && oldRequest.isScheduled()) {
      if (!newRequest.getQuartzScheduleSafe().equals(oldRequest.getQuartzScheduleSafe())) {
        return true;
      }
    }

    return false;
  }

  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    if (!maybeDeployId.isPresent()) {
      throw WebExceptions.conflict("Can not schedule a request (%s) with no deploy", requestId);
    }

    return maybeDeployId.get();
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}/bounce")
  @ApiOperation(value="Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.",
  response=SingularityRequestParent.class)
  public SingularityRequestParent bounce(@ApiParam("The request ID to bounce") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the bounce") @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    if (!requestWithState.getRequest().isLongRunning()) {
      throw WebExceptions.badRequest("Can not bounce a scheduled or one-off request (%s)", requestWithState);
    }

    checkRequestStateNotPaused(requestWithState, "bounce");

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.BOUNCE));

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}/run")
  @ApiOperation(value="Schedule a one-off or scheduled Singularity request for immediate execution.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Singularity Request is not scheduled or one-off"),
  })
  public SingularityRequestParent scheduleImmediately(@ApiParam("The request ID to run") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the execution") @QueryParam("user") Optional<String> user,
      @ApiParam("Additional command line arguments to append to the task") String commandLineArgs) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    checkRequestStateNotPaused(requestWithState, "run now");

    Optional<String> maybeCmdLineArgs = Optional.absent();

    PendingType pendingType = null;

    if (requestWithState.getRequest().isScheduled()) {
      pendingType = PendingType.IMMEDIATE;
    } else if (requestWithState.getRequest().isOneOff()) {
      pendingType = PendingType.ONEOFF;
    } else {
      throw WebExceptions.badRequest("Can not request an immediate run of a non-scheduled / always running request (%s)", requestWithState.getRequest());
    }

    if (!Strings.isNullOrEmpty(commandLineArgs)) {
      maybeCmdLineArgs = Optional.of(commandLineArgs);
    }

    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(), maybeCmdLineArgs, user, pendingType));

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}/pause")
  @ApiOperation(value="Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is already paused or being cleaned"),
  })
  public SingularityRequestParent pause(@ApiParam("The request ID to pause") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the pause") @QueryParam("user") Optional<String> user,
      @ApiParam("Pause Request Options") Optional<SingularityPauseRequest> pauseRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    checkRequestStateNotPaused(requestWithState, "pause");

    Optional<Boolean> killTasks = Optional.absent();
    if (pauseRequest.isPresent()) {
      user = pauseRequest.get().getUser();
      killTasks = pauseRequest.get().getKillTasks();
    }

    mailer.sendRequestPausedMail(requestWithState.getRequest(), user);
    requestManager.pause(requestWithState.getRequest(), user);

    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.PAUSING, System.currentTimeMillis(), killTasks, requestId));

    if (result != SingularityCreateResult.CREATED) {
      throw WebExceptions.conflict("A cleanup/pause request for %s failed to create because it was in state %s", requestId, result);
    }

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.PAUSED));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}/unpause")
  @ApiOperation(value="Unpause a Singularity Request, scheduling new tasks immediately", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is not paused"),
  })
  public SingularityRequestParent unpause(@ApiParam("The request ID to unpause") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the unpause") @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    if (requestWithState.getState() != RequestState.PAUSED) {
      throw WebExceptions.conflict("Request %s is not in PAUSED state, it is in %s", requestId, requestWithState.getState());
    }

    mailer.sendRequestUnpausedMail(requestWithState.getRequest(), user);

    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    requestManager.unpause(requestWithState.getRequest(), user);

    if (maybeDeployId.isPresent() && !requestWithState.getRequest().isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.UNPAUSED));
    }

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.ACTIVE));
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @Path("/active")
  @ApiOperation(value="Retrieve the list of active requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getActiveRequests() {
    return getRequestsWithDeployState(requestManager.getActiveRequests());
  }

  private List<SingularityRequestParent> getRequestsWithDeployState(Iterable<SingularityRequestWithState> requests) {
    List<String> requestIds = Lists.newArrayList();
    for (SingularityRequestWithState requestWithState : requests) {
      requestIds.add(requestWithState.getRequest().getId());
    }

    List<SingularityRequestParent> parents = Lists.newArrayListWithCapacity(requestIds.size());

    Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(requestIds);

    for (SingularityRequestWithState requestWithState : requests) {
      Optional<SingularityRequestDeployState> deployState = Optional.fromNullable(deployStates.get(requestWithState.getRequest().getId()));
      parents.add(new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), deployState, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeploy> absent(), Optional.<SingularityPendingDeploy> absent()));
    }

    return parents;
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @Path("/paused")
  @ApiOperation(value="Retrieve the list of paused requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getPausedRequests() {
    return getRequestsWithDeployState(requestManager.getPausedRequests());
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @Path("/cooldown")
  @ApiOperation(value="Retrieve the list of requests in system cooldown", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getCooldownRequests() {
    return getRequestsWithDeployState(requestManager.getCooldownRequests());
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @Path("/finished")
  @ApiOperation(value="Retreive the list of finished requests (Scheduled requests which have exhausted their schedules)", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getFinishedRequests() {
    return getRequestsWithDeployState(requestManager.getFinishedRequests());
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @ApiOperation(value="Retrieve the list of all requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getRequests() {
    return getRequestsWithDeployState(requestManager.getRequests());
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @Path("/queued/pending")
  @ApiOperation(value="Retrieve the list of pending requests", response=SingularityPendingRequest.class, responseContainer="List")
  public List<SingularityPendingRequest> getPendingRequests() {
    return requestManager.getPendingRequests();
  }

  @GET
  @Timed
  @ExceptionMetered
  @PropertyFiltering
  @Path("/queued/cleanup")
  @ApiOperation(value="Retrieve the list of requests being cleaned up", response=SingularityRequestCleanup.class, responseContainer="List")
  public List<SingularityRequestCleanup> getCleanupRequests() {
    return requestManager.getCleanupRequests();
  }

  @GET
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}")
  @ApiOperation(value="Retrieve a specific Request by ID", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent getRequest(@ApiParam("Request ID") @PathParam("requestId") String requestId) {
    return fillEntireRequest(fetchRequestWithState(requestId));
  }

  private SingularityRequest fetchRequest(String requestId) {
    return fetchRequestWithState(requestId).getRequest();
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}")
  @ApiOperation(value="Delete a specific Request by ID and return the deleted Request", response=SingularityRequest.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequest deleteRequest(@ApiParam("The request ID to delete.") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the delete") @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);

    mailer.sendRequestRemovedMail(request, user);
    requestManager.deleteRequest(request, user);

    return request;
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Path("/request/{requestId}/instances")
  @ApiOperation(value="Scale the number of instances up or down for a specific Request", response=SingularityRequest.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Posted object did not match Request ID"),
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequest updateInstances(@ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the scale") @QueryParam("user") Optional<String> user,
      @ApiParam("Object to hold number of instances to request") SingularityRequestInstances newInstances) {
    if (requestId == null || newInstances.getId() == null || !requestId.equals(newInstances.getId())) {
      throw WebExceptions.badRequest("Update for request instance must pass a matching non-null requestId in path (%s) and object (%s)", requestId, newInstances.getId());
    }

    SingularityRequest oldRequest = fetchRequest(requestId);
    Optional<SingularityRequest> maybeOldRequest = Optional.of(oldRequest);

    SingularityRequestDeployHolder deployHolder = getDeployHolder(newInstances.getId());
    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(newInstances.getInstances()).build();

    validator.checkSingularityRequest(newRequest, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy());

    requestManager.update(newRequest, user);

    checkReschedule(newRequest, maybeOldRequest);

    return newRequest;
  }

}
