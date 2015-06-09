package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import java.util.Collections;
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

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestInstances;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
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
  private final SingularityAuthorizationHelper authHelper;

  private final SingularityMailer mailer;
  private final TaskManager taskManager;

  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, TaskManager taskManager, RequestManager requestManager, SingularityMailer mailer, SingularityAuthorizationHelper authHelper, Optional<SingularityUser> user) {
    super(requestManager, deployManager, user);

    this.validator = validator;
    this.mailer = mailer;
    this.taskManager = taskManager;
    this.authHelper = authHelper;
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
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Create or update a Singularity Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Request object is invalid"),
    @ApiResponse(code=409, message="Request object is being cleaned. Try again shortly"),
  })
  public SingularityRequestParent submit(@ApiParam("The Singularity request to create or update") SingularityRequest request,
      @ApiParam("Username of the person requesting to create or update") @QueryParam("user") Optional<String> queryUser) {
    checkNotNullBadRequest(request.getId(), "Request must have an id");
    checkConflict(!requestManager.cleanupRequestExists(request.getId()), "Request %s is currently cleaning. Try again after a few moments", request.getId());

    Optional<SingularityRequestWithState> maybeOldRequestWithState = requestManager.getRequest(request.getId());
    Optional<SingularityRequest> maybeOldRequest = maybeOldRequestWithState.isPresent() ? Optional.of(maybeOldRequestWithState.get().getRequest()) : Optional.<SingularityRequest> absent();

    SingularityRequestDeployHolder deployHolder = getDeployHolder(request.getId());

    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy(), user);

    checkConflict(maybeOldRequest.isPresent() || !requestManager.cleanupRequestExists(request.getId()), "Request %s is currently cleaning. Try again after a few moments", request.getId());

    final long now = System.currentTimeMillis();

    requestManager.activate(newRequest, maybeOldRequest.isPresent() ? RequestHistoryType.UPDATED : RequestHistoryType.CREATED, now, queryUser);

    checkReschedule(newRequest, maybeOldRequest, now);

    return fillEntireRequest(fetchRequestWithState(request.getId()));
  }

  private void checkReschedule(SingularityRequest newRequest, Optional<SingularityRequest> maybeOldRequest, long timestamp) {
    if (!maybeOldRequest.isPresent()) {
      return;
    }

    if (shouldReschedule(newRequest, maybeOldRequest.get())) {
      Optional<String> maybeDeployId = deployManager.getInUseDeployId(newRequest.getId());

      if (maybeDeployId.isPresent()) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), timestamp, PendingType.UPDATED_REQUEST));
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

    checkConflict(maybeDeployId.isPresent(), "Can not schedule/bounce a request (%s) with no deploy", requestId);

    return maybeDeployId.get();
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @ApiOperation(value="Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.",
  response=SingularityRequestParent.class)
  public SingularityRequestParent bounce(@ApiParam("The request ID to bounce") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the bounce") @QueryParam("user") Optional<String> queryUser) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    validator.checkForAuthorization(requestWithState.getRequest(), Optional.<SingularityRequest>absent(), user);

    checkBadRequest(requestWithState.getRequest().isLongRunning(), "Can not bounce a %s request (%s)", requestWithState.getRequest().getRequestType(), requestWithState);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to bounce (it must be manually unpaused first)", requestWithState.getRequest().getId());

    SingularityCreateResult createResult = requestManager.createCleanupRequest(
            new SingularityRequestCleanup(queryUser, RequestCleanupType.BOUNCE, System.currentTimeMillis(), Optional.<Boolean>absent(), requestId, Optional.of(getAndCheckDeployId(requestId))));

    checkConflict(createResult != SingularityCreateResult.EXISTED, "%s is already bouncing", requestId);

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Schedule a one-off or scheduled Singularity request for immediate execution.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Singularity Request is not scheduled or one-off"),
  })
  public SingularityRequestParent scheduleImmediately(@ApiParam("The request ID to run") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the execution") @QueryParam("user") Optional<String> queryUser,
      @ApiParam("Additional command line arguments to append to the task") List<String> commandLineArgs) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    validator.checkForAuthorization(requestWithState.getRequest(), Optional.<SingularityRequest>absent(), user);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to run now (it must be manually unpaused first)", requestWithState.getRequest().getId());

    PendingType pendingType = null;

    if (requestWithState.getRequest().isScheduled()) {
      pendingType = PendingType.IMMEDIATE;
      checkConflict(taskManager.getActiveTaskIdsForRequest(requestId).isEmpty(), "Can not request an immediate run of a scheduled job which is currently running (%s)", taskManager.getActiveTaskIdsForRequest(requestId));
    } else if (requestWithState.getRequest().isOneOff()) {
      pendingType = PendingType.ONEOFF;
    } else {
      throw badRequest("Can not request an immediate run of a non-scheduled / always running request (%s)", requestWithState.getRequest());
    }

    final SingularityPendingRequest pendingRequest = new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(), queryUser, pendingType, commandLineArgs);

    SingularityCreateResult result = requestManager.addToPendingQueue(pendingRequest);

    checkConflict(result != SingularityCreateResult.EXISTED, "%s is already pending, please try again soon", requestId);

    return SingularityPendingRequestParent.fromSingularityRequestParent(fillEntireRequest(requestWithState), pendingRequest);
  }

  @POST
  @Path("/request/{requestId}/pause")
  @ApiOperation(value="Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is already paused or being cleaned"),
  })
  public SingularityRequestParent pause(@ApiParam("The request ID to pause") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the pause") @QueryParam("user") Optional<String> queryUser,
      @ApiParam("Pause Request Options") Optional<SingularityPauseRequest> pauseRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    validator.checkForAuthorization(requestWithState.getRequest(), Optional.<SingularityRequest>absent(), user);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to pause (it must be manually unpaused first)", requestWithState.getRequest().getId());

    Optional<Boolean> killTasks = Optional.absent();
    Optional<String> pauseUser = Optional.absent();
    if (pauseRequest.isPresent()) {
      pauseUser = queryUser.or(pauseRequest.get().getUser());
      killTasks = pauseRequest.get().getKillTasks();
    }

    final long now = System.currentTimeMillis();

    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(queryUser, RequestCleanupType.PAUSING, now, killTasks, requestId, Optional.<String> absent()));

    checkConflict(result == SingularityCreateResult.CREATED, "%s is already pausing - try again soon", requestId, result);

    mailer.sendRequestPausedMail(requestWithState.getRequest(), queryUser);

    requestManager.pause(requestWithState.getRequest(), now, pauseUser);

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.PAUSED, now));
  }

  @POST
  @Path("/request/{requestId}/unpause")
  @ApiOperation(value="Unpause a Singularity Request, scheduling new tasks immediately", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is not paused"),
  })
  public SingularityRequestParent unpause(@ApiParam("The request ID to unpause") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the unpause") @QueryParam("user") Optional<String> queryUser) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    validator.checkForAuthorization(requestWithState.getRequest(), Optional.<SingularityRequest>absent(), user);

    checkConflict(requestWithState.getState() == RequestState.PAUSED, "Request %s is not in PAUSED state, it is in %s", requestId, requestWithState.getState());

    mailer.sendRequestUnpausedMail(requestWithState.getRequest(), queryUser);

    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    final long now = System.currentTimeMillis();

    requestManager.unpause(requestWithState.getRequest(), now, queryUser);

    if (maybeDeployId.isPresent() && !requestWithState.getRequest().isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), now, queryUser, PendingType.UNPAUSED, Collections.<String> emptyList()));
    }

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.ACTIVE, now));
  }

  private List<SingularityRequestParent> getRequestsWithDeployState(Iterable<SingularityRequestWithState> requests) {
    if (!validator.hasAdminAuthorization(user)) {
      requests = Iterables.filter(requests, new Predicate<SingularityRequestWithState>() {
        @Override
        public boolean apply(SingularityRequestWithState input) {
          return validator.isAuthorizedForRequest(input.getRequest(), user);
        }
      });
    }

    final List<String> requestIds = Lists.newArrayList();
    for (SingularityRequestWithState requestWithState : requests) {
      requestIds.add(requestWithState.getRequest().getId());
    }

    final List<SingularityRequestParent> parents = Lists.newArrayListWithCapacity(requestIds.size());

    final Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(requestIds);

    for (SingularityRequestWithState requestWithState : requests) {
      Optional<SingularityRequestDeployState> deployState = Optional.fromNullable(deployStates.get(requestWithState.getRequest().getId()));
      parents.add(new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), deployState, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeploy> absent(), Optional.<SingularityPendingDeploy> absent()));
    }

    return parents;
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation(value="Retrieve the list of active requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getActiveRequests() {
    return getRequestsWithDeployState(requestManager.getActiveRequests());
  }

  @GET
  @PropertyFiltering
  @Path("/paused")
  @ApiOperation(value="Retrieve the list of paused requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getPausedRequests() {
    return getRequestsWithDeployState(requestManager.getPausedRequests());
  }

  @GET
  @PropertyFiltering
  @Path("/cooldown")
  @ApiOperation(value="Retrieve the list of requests in system cooldown", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getCooldownRequests() {
    return getRequestsWithDeployState(requestManager.getCooldownRequests());
  }

  @GET
  @PropertyFiltering
  @Path("/finished")
  @ApiOperation(value="Retreive the list of finished requests (Scheduled requests which have exhausted their schedules)", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getFinishedRequests() {
    return getRequestsWithDeployState(requestManager.getFinishedRequests());
  }

  @GET
  @PropertyFiltering
  @ApiOperation(value="Retrieve the list of all requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getRequests() {
    return getRequestsWithDeployState(requestManager.getRequests());
  }

  @GET
  @PropertyFiltering
  @Path("/queued/pending")
  @ApiOperation(value="Retrieve the list of pending requests", response=SingularityPendingRequest.class, responseContainer="List")
  public List<SingularityPendingRequest> getPendingRequests() {
    return authHelper.filterByAuthorizedRequests(user, requestManager.getPendingRequests(), SingularityTransformHelpers.PENDING_REQUEST_TO_REQUEST_ID);
  }

  @GET
  @PropertyFiltering
  @Path("/queued/cleanup")
  @ApiOperation(value="Retrieve the list of requests being cleaned up", response=SingularityRequestCleanup.class, responseContainer="List")
  public Iterable<SingularityRequestCleanup> getCleanupRequests() {
    return authHelper.filterByAuthorizedRequests(user, requestManager.getCleanupRequests(), SingularityTransformHelpers.REQUEST_CLEANUP_TO_REQUEST_ID);
  }

  @GET
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
  @Path("/request/{requestId}")
  @ApiOperation(value="Delete a specific Request by ID and return the deleted Request", response=SingularityRequest.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequest deleteRequest(@ApiParam("The request ID to delete.") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the delete") @QueryParam("user") Optional<String> queryUser) {
    SingularityRequest request = fetchRequest(requestId);

    validator.checkForAuthorization(request, Optional.<SingularityRequest>absent(), user);

    requestManager.deleteRequest(request, queryUser);

    mailer.sendRequestRemovedMail(request, queryUser);

    return request;
  }

  @PUT
  @Path("/request/{requestId}/instances")
  @ApiOperation(value="Scale the number of instances up or down for a specific Request", response=SingularityRequest.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Posted object did not match Request ID"),
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequest updateInstances(@ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
      @ApiParam("Username of the person requesting the scale") @QueryParam("user") Optional<String> queryUser,
      @ApiParam("Object to hold number of instances to request") SingularityRequestInstances newInstances) {

    checkBadRequest(requestId != null && newInstances.getId() != null && requestId.equals(newInstances.getId()), "Update for request instance must pass a matching non-null requestId in path (%s) and object (%s)", requestId, newInstances.getId());
    checkConflict(!requestManager.cleanupRequestExists(requestId), "Request %s is currently cleaning. Try again after a few moments", requestId);

    SingularityRequest oldRequest = fetchRequest(requestId);
    Optional<SingularityRequest> maybeOldRequest = Optional.of(oldRequest);

    SingularityRequestDeployHolder deployHolder = getDeployHolder(newInstances.getId());
    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(newInstances.getInstances()).build();

    validator.checkSingularityRequest(newRequest, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy(), user);

    final long now = System.currentTimeMillis();

    requestManager.update(newRequest, now, queryUser);

    checkReschedule(newRequest, maybeOldRequest, now);

    return newRequest;
  }

}
