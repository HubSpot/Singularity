package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityExitCooldownRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringParent;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.helpers.RequestHelper;
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

  private final SingularityMailer mailer;
  private final TaskManager taskManager;
  private final SlaveManager slaveManager;
  private final RequestHelper requestHelper;
  private final SingularityConfiguration configuration;

  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, TaskManager taskManager, RequestManager requestManager, SingularityMailer mailer,
      SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user, SlaveManager slaveManager, SingularityConfiguration configuration, RequestHelper requestHelper) {
    super(requestManager, deployManager, user, validator, authorizationHelper);

    this.mailer = mailer;
    this.taskManager = taskManager;
    this.requestHelper = requestHelper;
    this.slaveManager = slaveManager;
    this.configuration = configuration;
  }

  private void submitRequest(SingularityRequest request, Optional<SingularityRequestWithState> oldRequestWithState, Optional<RequestHistoryType> historyType,
      Optional<Boolean> skipHealthchecks, Optional<String> message) {
    checkNotNullBadRequest(request.getId(), "Request must have an id");
    checkConflict(!requestManager.cleanupRequestExists(request.getId()), "Request %s is currently cleaning. Try again after a few moments", request.getId());

    Optional<SingularityRequest> oldRequest = oldRequestWithState.isPresent() ? Optional.of(oldRequestWithState.get().getRequest()) : Optional.<SingularityRequest> absent();

    if (oldRequest.isPresent()) {
      authorizationHelper.checkForAuthorization(oldRequest.get(), user, SingularityAuthorizationScope.WRITE);
    }
    authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);

    RequestState requestState = RequestState.ACTIVE;

    if (oldRequestWithState.isPresent()) {
      requestState = oldRequestWithState.get().getState();
    }

    requestHelper.updateRequest(request, oldRequest, requestState, historyType, JavaUtils.getUserEmail(user), skipHealthchecks, message);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Create or update a Singularity Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Request object is invalid"),
    @ApiResponse(code=409, message="Request object is being cleaned. Try again shortly"),
  })
  public SingularityRequestParent postRequest(@ApiParam("The Singularity request to create or update") SingularityRequest request) {
    submitRequest(request, requestManager.getRequest(request.getId()), Optional.<RequestHistoryType> absent(), Optional.<Boolean> absent(), Optional.<String> absent());
    return fillEntireRequest(fetchRequestWithState(request.getId()));
  }

  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    checkConflict(maybeDeployId.isPresent(), "Can not schedule/bounce a request (%s) with no deploy", requestId);

    return maybeDeployId.get();
  }

  @POST
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent bounce(@PathParam("requestId") String requestId) {
    return bounce(requestId, Optional.<SingularityBounceRequest> absent());
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.",
  response=SingularityRequestParent.class)
  public SingularityRequestParent bounce(@ApiParam("The request ID to bounce") @PathParam("requestId") String requestId,
      @ApiParam("Bounce request options") Optional<SingularityBounceRequest> bounceRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkBadRequest(requestWithState.getRequest().isLongRunning(), "Can not bounce a %s request (%s)", requestWithState.getRequest().getRequestType(), requestWithState);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to bounce (it must be manually unpaused first)", requestWithState.getRequest().getId());

    SlavePlacement placement = requestWithState.getRequest().getSlavePlacement().or(configuration.getDefaultSlavePlacement());

    final boolean isIncrementalBounce = bounceRequest.isPresent() && bounceRequest.get().getIncremental().or(false);

    if (placement != SlavePlacement.GREEDY && placement != SlavePlacement.OPTIMISTIC) {
      int currentActiveSlaveCount = slaveManager.getNumObjectsAtState(MachineState.ACTIVE);
      int requiredSlaveCount = isIncrementalBounce ? requestWithState.getRequest().getInstancesSafe() + 1 : requestWithState.getRequest().getInstancesSafe() * 2;

      checkBadRequest(currentActiveSlaveCount >= requiredSlaveCount, "Not enough active slaves to successfully complete a bounce of request %s (minimum required: %s, current: %s). Consider deploying, or changing the slave placement strategy instead.", requestId, requiredSlaveCount, currentActiveSlaveCount);
    }

    final Optional<Boolean> skipHealthchecks = bounceRequest.isPresent() ? bounceRequest.get().getSkipHealthchecks() : Optional.<Boolean> absent();

    Optional<String> message = Optional.absent();
    Optional<String> actionId = Optional.absent();

    if (bounceRequest.isPresent()) {
      actionId = bounceRequest.get().getActionId();
      message = bounceRequest.get().getMessage();
    }

    if (!actionId.isPresent()) {
      actionId = Optional.of(UUID.randomUUID().toString());
    }

    final String deployId = getAndCheckDeployId(requestId);

    SingularityCreateResult createResult = requestManager.createCleanupRequest(
        new SingularityRequestCleanup(JavaUtils.getUserEmail(user), isIncrementalBounce ? RequestCleanupType.INCREMENTAL_BOUNCE : RequestCleanupType.BOUNCE,
            System.currentTimeMillis(), Optional.<Boolean> absent(), requestId, Optional.of(deployId), skipHealthchecks, message, actionId));

    checkConflict(createResult != SingularityCreateResult.EXISTED, "%s is already bouncing", requestId);

    requestManager.bounce(requestWithState.getRequest(), System.currentTimeMillis(), JavaUtils.getUserEmail(user), message);

    requestManager.saveExpiringObject(new SingularityExpiringBounce(requestId, deployId, JavaUtils.getUserEmail(user),
        System.currentTimeMillis(), bounceRequest.or(SingularityBounceRequest.defaultRequest()), actionId.get()));

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Path("/request/{requestId}/run")
  public SingularityPendingRequestParent scheduleImmediately(@PathParam("requestId") String requestId) {
    return scheduleImmediately(requestId, Optional.<SingularityRunNowRequest> absent());
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Schedule a one-off or scheduled Singularity request for immediate execution.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Singularity Request is not scheduled or one-off"),
  })
  public SingularityPendingRequestParent scheduleImmediately(@ApiParam("The request ID to run") @PathParam("requestId") String requestId,
      Optional<SingularityRunNowRequest> runNowRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

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

    Optional<String> runId = Optional.absent();
    Optional<String> message = Optional.absent();
    Optional<Boolean> skipHealthchecks = Optional.absent();
    Optional<List<String>> commandLineArgs = Optional.absent();

    if (runNowRequest.isPresent()) {
      message = runNowRequest.get().getMessage();
      runId = runNowRequest.get().getRunId();
      skipHealthchecks = runNowRequest.get().getSkipHealthchecks();
      commandLineArgs = runNowRequest.get().getCommandLineArgs();
    }

    if (runId.isPresent() && runId.get().length() > 100) {
      throw badRequest("runId must be less than 100 characters. RunId %s has %s characters", runId.get(), runId.get().length());
    }

    if (!runId.isPresent()) {
      runId = Optional.of(UUID.randomUUID().toString());
    }

    final SingularityPendingRequest pendingRequest = new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(),
        JavaUtils.getUserEmail(user), pendingType, commandLineArgs, runId, skipHealthchecks, message, Optional.<String> absent());

    SingularityCreateResult result = requestManager.addToPendingQueue(pendingRequest);

    checkConflict(result != SingularityCreateResult.EXISTED, "%s is already pending, please try again soon", requestId);

    return SingularityPendingRequestParent.fromSingularityRequestParent(fillEntireRequest(requestWithState), pendingRequest);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  @ApiOperation("Retrieve an active task by runId")
  public Optional<SingularityTaskId> getTaskByRunId(@PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.READ);
    return taskManager.getTaskByRunId(requestId, runId);
  }

  @POST
  @Path("/request/{requestId}/pause")
  public SingularityRequestParent pause(@PathParam("requestId") String requestId) {
    return pause(requestId, Optional.<SingularityPauseRequest> absent());
  }

  @POST
  @Path("/request/{requestId}/pause")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is already paused or being cleaned"),
  })
  public SingularityRequestParent pause(@ApiParam("The request ID to pause") @PathParam("requestId") String requestId,
      @ApiParam("Pause Request Options") Optional<SingularityPauseRequest> pauseRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to pause (it must be manually unpaused first)", requestWithState.getRequest().getId());

    Optional<Boolean> killTasks = Optional.absent();
    Optional<String> message = Optional.absent();
    Optional<String> actionId = Optional.absent();

    if (pauseRequest.isPresent()) {
      killTasks = pauseRequest.get().getKillTasks();
      message = pauseRequest.get().getMessage();

      if (pauseRequest.get().getDurationMillis().isPresent() && !actionId.isPresent()) {
        actionId = Optional.of(UUID.randomUUID().toString());
      }
    }

    final long now = System.currentTimeMillis();

    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(JavaUtils.getUserEmail(user),
        RequestCleanupType.PAUSING, now, killTasks, requestId, Optional.<String> absent(), Optional.<Boolean> absent(), message, actionId));

    checkConflict(result == SingularityCreateResult.CREATED, "%s is already pausing - try again soon", requestId, result);

    mailer.sendRequestPausedMail(requestWithState.getRequest(), pauseRequest, JavaUtils.getUserEmail(user));

    requestManager.pause(requestWithState.getRequest(), now, JavaUtils.getUserEmail(user), message);

    if (pauseRequest.isPresent() && pauseRequest.get().getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringPause(requestId, JavaUtils.getUserEmail(user),
          System.currentTimeMillis(), pauseRequest.get(), actionId.get()));
    }

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.PAUSED, now));
  }

  @POST
  @Path("/request/{requestId}/unpause")
  public SingularityRequestParent unpauseNoBody(@PathParam("requestId") String requestId) {
    return unpause(requestId, Optional.<SingularityUnpauseRequest> absent());
  }

  @POST
  @Path("/request/{requestId}/unpause")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Unpause a Singularity Request, scheduling new tasks immediately", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is not paused"),
  })
  public SingularityRequestParent unpause(@ApiParam("The request ID to unpause") @PathParam("requestId") String requestId,
      Optional<SingularityUnpauseRequest> unpauseRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() == RequestState.PAUSED, "Request %s is not in PAUSED state, it is in %s", requestId, requestWithState.getState());

    Optional<String> message = Optional.absent();
    Optional<Boolean> skipHealthchecks = Optional.absent();

    if (unpauseRequest.isPresent()) {
      message = unpauseRequest.get().getMessage();
      skipHealthchecks = unpauseRequest.get().getSkipHealthchecks();
    }

    final long now = requestHelper.unpause(requestWithState.getRequest(), JavaUtils.getUserEmail(user), message, skipHealthchecks);

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.ACTIVE, now));
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  public SingularityRequestParent exitCooldown(@PathParam("requestId") String requestId) {
    return exitCooldown(requestId, Optional.<SingularityExitCooldownRequest> absent());
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Immediately exits cooldown, scheduling new tasks immediately", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is not in cooldown"),
  })
  public SingularityRequestParent exitCooldown(@PathParam("requestId") String requestId, Optional<SingularityExitCooldownRequest> exitCooldownRequest) {
    final SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() == RequestState.SYSTEM_COOLDOWN, "Request %s is not in SYSTEM_COOLDOWN state, it is in %s", requestId, requestWithState.getState());

    final Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    final long now = System.currentTimeMillis();

    Optional<String> message = Optional.absent();
    Optional<Boolean> skipHealthchecks = Optional.absent();

    if (exitCooldownRequest.isPresent()) {
      message = exitCooldownRequest.get().getMessage();
      skipHealthchecks = exitCooldownRequest.get().getSkipHealthchecks();
    }

    requestManager.exitCooldown(requestWithState.getRequest(), now, JavaUtils.getUserEmail(user), message);

    if (maybeDeployId.isPresent() && !requestWithState.getRequest().isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), now, JavaUtils.getUserEmail(user),
          PendingType.IMMEDIATE, skipHealthchecks, message));
    }

    return fillEntireRequest(requestWithState);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation(value="Retrieve the list of active requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getActiveRequests() {
    return getRequestsWithDeployState(requestManager.getActiveRequests(), SingularityAuthorizationScope.READ);
  }

  private List<SingularityRequestParent> getRequestsWithDeployState(Iterable<SingularityRequestWithState> requests, final SingularityAuthorizationScope scope) {
    if (!authorizationHelper.hasAdminAuthorization(user)) {
      requests = Iterables.filter(requests, new Predicate<SingularityRequestWithState>() {
        @Override
        public boolean apply(SingularityRequestWithState input) {
          return authorizationHelper.isAuthorizedForRequest(input.getRequest(), user, scope);
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
      parents.add(new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), deployState));
    }

    return parents;
  }

  @GET
  @PropertyFiltering
  @Path("/paused")
  @ApiOperation(value="Retrieve the list of paused requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getPausedRequests() {
    return getRequestsWithDeployState(requestManager.getPausedRequests(), SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/cooldown")
  @ApiOperation(value="Retrieve the list of requests in system cooldown", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getCooldownRequests() {
    return getRequestsWithDeployState(requestManager.getCooldownRequests(), SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/finished")
  @ApiOperation(value="Retreive the list of finished requests (Scheduled requests which have exhausted their schedules)", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getFinishedRequests() {
    return getRequestsWithDeployState(requestManager.getFinishedRequests(), SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @ApiOperation(value="Retrieve the list of all requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getRequests() {
    return getRequestsWithDeployState(requestManager.getRequests(), SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/queued/pending")
  @ApiOperation(value="Retrieve the list of pending requests", response=SingularityPendingRequest.class, responseContainer="List")
  public Iterable<SingularityPendingRequest> getPendingRequests() {
    return authorizationHelper.filterByAuthorizedRequests(user, requestManager.getPendingRequests(), SingularityTransformHelpers.PENDING_REQUEST_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/queued/cleanup")
  @ApiOperation(value="Retrieve the list of requests being cleaned up", response=SingularityRequestCleanup.class, responseContainer="List")
  public Iterable<SingularityRequestCleanup> getCleanupRequests() {
    return authorizationHelper.filterByAuthorizedRequests(user, requestManager.getCleanupRequests(), SingularityTransformHelpers.REQUEST_CLEANUP_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
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
  public SingularityRequest deleteRequest(@PathParam("requestId") String requestId) {
    return deleteRequest(requestId, Optional.<SingularityDeleteRequestRequest> absent());
  }

  @DELETE
  @Path("/request/{requestId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Delete a specific Request by ID and return the deleted Request", response=SingularityRequest.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequest deleteRequest(@ApiParam("The request ID to delete.") @PathParam("requestId") String requestId,
      @ApiParam("Delete options") Optional<SingularityDeleteRequestRequest> deleteRequest) {
    SingularityRequest request = fetchRequest(requestId);

    authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);

    Optional<String> message = Optional.absent();
    Optional<String> actionId = Optional.absent();

    if (deleteRequest.isPresent()) {
      actionId = deleteRequest.get().getActionId();
      message = deleteRequest.get().getMessage();
    }

    requestManager.deleteRequest(request, JavaUtils.getUserEmail(user), actionId, message);

    mailer.sendRequestRemovedMail(request, JavaUtils.getUserEmail(user), message);

    return request;
  }

  @PUT
  @Path("/request/{requestId}/scale")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Scale the number of instances up or down for a specific Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent scale(@ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
      @ApiParam("Object to hold number of instances to request") SingularityScaleRequest scaleRequest) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId);

    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(scaleRequest.getInstances()).build();

    checkBadRequest(oldRequest.getInstancesSafe() != newRequest.getInstancesSafe(), "Scale request has no affect on the # of instances (%s)", newRequest.getInstancesSafe());

    submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.of(RequestHistoryType.SCALED), scaleRequest.getSkipHealthchecks(), scaleRequest.getMessage());

    if (scaleRequest.getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringScale(requestId, JavaUtils.getUserEmail(user),
          System.currentTimeMillis(), scaleRequest, oldRequest.getInstances(), scaleRequest.getActionId().or(UUID.randomUUID().toString())));
    }

    mailer.sendRequestScaledMail(newRequest, Optional.of(scaleRequest), oldRequest.getInstances(), JavaUtils.getUserEmail(user));

    return fillEntireRequest(fetchRequestWithState(requestId));
  }

  private <T extends SingularityExpiringParent<?>> SingularityRequestParent deleteExpiringObject(Class<T> clazz, String requestId) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    SingularityDeleteResult deleteResult = requestManager.deleteExpiringObject(clazz, requestId);

    WebExceptions.checkNotFound(deleteResult == SingularityDeleteResult.DELETED, "%s didn't have an expiring %s request", clazz.getSimpleName(), requestId);

    return fillEntireRequest(requestWithState);
  }

  @DELETE
  @Path("/request/{requestId}/scale")
  @ApiOperation(value="Delete/cancel the expiring scale. This makes the scale request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring scale request for that ID"),
  })
  public SingularityRequestParent deleteExpiringScale(@ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringScale.class, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/skipHealthchecks")
  @ApiOperation(value="Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring skipHealthchecks request for that ID"),
  })
  public SingularityRequestParent deleteExpiringSkipHealthchecks(@ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringSkipHealthchecks.class, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/pause")
  @ApiOperation(value="Delete/cancel the expiring pause. This makes the pause request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring pause request for that ID"),
  })
  public SingularityRequestParent deleteExpiringPause(@ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringPause.class, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/bounce")
  @ApiOperation(value="Delete/cancel the expiring bounce. This makes the bounce request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring bounce request for that ID"),
  })
  public SingularityRequestParent deleteExpiringBounce(@ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringBounce.class, requestId);
  }

  @PUT
  @Path("/request/{requestId}/skipHealthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Update the skipHealthchecks field for the request, possibly temporarily", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent skipHealthchecks(@ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
      @ApiParam("SkipHealtchecks options") SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId);

    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    SingularityRequest newRequest = oldRequest.toBuilder().setSkipHealthchecks(skipHealthchecksRequest.getSkipHealthchecks()).build();

    submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.<RequestHistoryType> absent(), Optional.<Boolean> absent(), skipHealthchecksRequest.getMessage());

    if (skipHealthchecksRequest.getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringSkipHealthchecks(requestId, JavaUtils.getUserEmail(user),
          System.currentTimeMillis(), skipHealthchecksRequest, oldRequest.getSkipHealthchecks(), skipHealthchecksRequest.getActionId().or(UUID.randomUUID().toString())));
    }

    return fillEntireRequest(fetchRequestWithState(requestId));
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @ApiOperation("Retrieve the list of tasks being cleaned from load balancers.")
  public Iterable<String> getLbCleanupRequests() {
    return authorizationHelper.filterAuthorizedRequestIds(user, requestManager.getLbCleanupRequestIds(), SingularityAuthorizationScope.READ);
  }

}
