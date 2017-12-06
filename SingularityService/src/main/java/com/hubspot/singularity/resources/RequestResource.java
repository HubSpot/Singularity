package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityShellCommand;
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
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringRequestActionParent;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.ning.http.client.AsyncHttpClient;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.REQUEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity Requests, the parent object for any deployed task", value=ApiPaths.REQUEST_RESOURCE_PATH, position=1)
public class RequestResource extends AbstractRequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  private final SingularityMailer mailer;
  private final TaskManager taskManager;
  private final RequestHelper requestHelper;
  private final SlaveManager slaveManager;

  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, TaskManager taskManager, RequestManager requestManager, SingularityMailer mailer,
                         SingularityAuthorizationHelper authorizationHelper, RequestHelper requestHelper, LeaderLatch leaderLatch,
                         SlaveManager slaveManager, AsyncHttpClient httpClient, ObjectMapper objectMapper, RequestHistoryHelper requestHistoryHelper) {
    super(requestManager, deployManager, validator, authorizationHelper, httpClient, leaderLatch, objectMapper, requestHelper, requestHistoryHelper);
    this.mailer = mailer;
    this.taskManager = taskManager;
    this.requestHelper = requestHelper;
    this.slaveManager = slaveManager;
  }

  private void submitRequest(SingularityRequest request, Optional<SingularityRequestWithState> oldRequestWithState, Optional<RequestHistoryType> historyType,
      Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<SingularityBounceRequest> maybeBounceRequest, SingularityUser user) {
    checkNotNullBadRequest(request.getId(), "Request must have an id");
    checkConflict(!requestManager.cleanupRequestExists(request.getId()), "Request %s is currently cleaning. Try again after a few moments", request.getId());

    Optional<SingularityPendingDeploy> maybePendingDeploy = deployManager.getPendingDeploy(request.getId());
    checkConflict(!(maybePendingDeploy.isPresent() && maybePendingDeploy.get().getUpdatedRequest().isPresent()), "Request %s has a pending deploy that may change the request data. Try again when the deploy has finished", request.getId());

    Optional<SingularityRequest> oldRequest = oldRequestWithState.isPresent() ? Optional.of(oldRequestWithState.get().getRequest()) : Optional.<SingularityRequest> absent();

    if (oldRequest.isPresent()) {
      authorizationHelper.checkForAuthorization(oldRequest.get(), user, SingularityAuthorizationScope.WRITE);
      authorizationHelper.checkForAuthorizedChanges(request, oldRequest.get(), user);
      validator.checkActionEnabled(SingularityAction.UPDATE_REQUEST);
    } else {
      validator.checkActionEnabled(SingularityAction.CREATE_REQUEST);
    }

    if (request.getSlavePlacement().isPresent() && request.getSlavePlacement().get() == SlavePlacement.SPREAD_ALL_SLAVES) {
      checkBadRequest(validator.isSpreadAllSlavesEnabled(), "You must enabled spread to all slaves in order to use the SPREAD_ALL_SLAVES request type");
      int currentActiveSlaveCount =  slaveManager.getNumObjectsAtState(MachineState.ACTIVE);
      request = request.toBuilder().setInstances(Optional.of(currentActiveSlaveCount)).build();
    }

    if (!oldRequest.isPresent() || !(oldRequest.get().getInstancesSafe() == request.getInstancesSafe())) {
      validator.checkScale(request, Optional.<Integer>absent());
    }

    authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);

    RequestState requestState = RequestState.ACTIVE;

    if (oldRequestWithState.isPresent()) {
      requestState = oldRequestWithState.get().getState();
    }

    requestHelper.updateRequest(request, oldRequest, requestState, historyType, user.getEmail(), skipHealthchecks, message, maybeBounceRequest);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Create or update a Singularity Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Request object is invalid"),
    @ApiResponse(code=409, message="Request object is being cleaned. Try again shortly"),
  })
  public SingularityRequestParent postRequest(@Auth SingularityUser user,
                                              @Context HttpServletRequest requestContext,
                                              @ApiParam("The Singularity request to create or update") SingularityRequest request) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, request, () -> postRequest(request, user));
  }

  public SingularityRequestParent postRequest(SingularityRequest request, SingularityUser user) {
    submitRequest(request, requestManager.getRequest(request.getId()), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), user);
    return fillEntireRequest(fetchRequestWithState(request.getId(), user));
  }

  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    checkConflict(maybeDeployId.isPresent(), "Can not schedule/bounce a request (%s) with no deploy", requestId);

    return maybeDeployId.get();
  }

  @POST
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent bounce(@Auth SingularityUser user,
                                         @PathParam("requestId") String requestId,
                                         @Context HttpServletRequest requestContext) {
    return bounce(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy.",
  response=SingularityRequestParent.class)
  public SingularityRequestParent bounce(@Auth SingularityUser user,
                                         @ApiParam("The request ID to bounce") @PathParam("requestId") String requestId,
                                         @Context HttpServletRequest requestContext,
                                         @ApiParam("Bounce request options") SingularityBounceRequest bounceRequest) {
    final Optional<SingularityBounceRequest> maybeBounceRequest = Optional.fromNullable(bounceRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybeBounceRequest.orNull(), () -> bounce(requestId, maybeBounceRequest, user));
  }

  public SingularityRequestParent bounce(String requestId, Optional<SingularityBounceRequest> bounceRequest, SingularityUser user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.BOUNCE_REQUEST);

    checkBadRequest(requestWithState.getRequest().isLongRunning(), "Can not bounce a %s request (%s)", requestWithState.getRequest().getRequestType(), requestWithState);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to bounce (it must be manually unpaused first)", requestWithState.getRequest().getId());

    final boolean isIncrementalBounce = bounceRequest.isPresent() && bounceRequest.get().getIncremental().or(false);

    validator.checkResourcesForBounce(requestWithState.getRequest(), isIncrementalBounce);
    validator.checkRequestForPriorityFreeze(requestWithState.getRequest());

    final Optional<Boolean> skipHealthchecks = bounceRequest.isPresent() ? bounceRequest.get().getSkipHealthchecks() : Optional.<Boolean> absent();

    Optional<String> message = Optional.absent();
    Optional<String> actionId = Optional.absent();
    Optional<SingularityShellCommand> runBeforeKill = Optional.absent();

    if (bounceRequest.isPresent()) {
      actionId = bounceRequest.get().getActionId();
      message = bounceRequest.get().getMessage();
      if (bounceRequest.get().getRunShellCommandBeforeKill().isPresent()) {
        validator.checkValidShellCommand(bounceRequest.get().getRunShellCommandBeforeKill().get());
        runBeforeKill = bounceRequest.get().getRunShellCommandBeforeKill();
      }
    }

    if (!actionId.isPresent()) {
      actionId = Optional.of(UUID.randomUUID().toString());
    }

    final String deployId = getAndCheckDeployId(requestId);

    checkConflict(!(requestManager.markAsBouncing(requestId) == SingularityCreateResult.EXISTED), "%s is already bouncing", requestId);

    requestManager.createCleanupRequest(
        new SingularityRequestCleanup(user.getEmail(), isIncrementalBounce ? RequestCleanupType.INCREMENTAL_BOUNCE : RequestCleanupType.BOUNCE,
            System.currentTimeMillis(), Optional.absent(), Optional.absent(), requestId, Optional.of(deployId), skipHealthchecks, message, actionId, runBeforeKill));

    requestManager.bounce(requestWithState.getRequest(), System.currentTimeMillis(), Optional.of(user.getId()), message);

    final SingularityBounceRequest validatedBounceRequest = validator.checkBounceRequest(bounceRequest.or(SingularityBounceRequest.defaultRequest()));

    requestManager.saveExpiringObject(new SingularityExpiringBounce(requestId, deployId, Optional.of(user.getId()),
        System.currentTimeMillis(), validatedBounceRequest, actionId.get()));

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Path("/request/{requestId}/run")
  public SingularityPendingRequestParent scheduleImmediately(@Auth SingularityUser user, @PathParam("requestId") String requestId) {
    return scheduleImmediately(user, requestId, null);
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Schedule a one-off or scheduled Singularity request for immediate or delayed execution.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Singularity Request is not scheduled or one-off"),
  })
  public SingularityPendingRequestParent scheduleImmediately(@Auth SingularityUser user,
                                                             @ApiParam("The request ID to run") @PathParam("requestId") String requestId,
                                                             SingularityRunNowRequest runNowRequest) {
    final Optional<SingularityRunNowRequest> maybeRunNowRequest = Optional.fromNullable(runNowRequest);
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to run now (it must be manually unpaused first)", requestWithState.getRequest().getId());


    final SingularityPendingRequest pendingRequest = validator.checkRunNowRequest(
        getAndCheckDeployId(requestId),
        user.getEmail(),
        requestWithState.getRequest(),
        maybeRunNowRequest,
        taskManager.getActiveTaskIdsForRequest(requestId),
        taskManager.getPendingTaskIdsForRequest(requestId));

    SingularityCreateResult result = requestManager.addToPendingQueue(pendingRequest);

    checkConflict(result != SingularityCreateResult.EXISTED, "%s is already pending, please try again soon", requestId);

    return SingularityPendingRequestParent.fromSingularityRequestParent(fillEntireRequest(requestWithState), pendingRequest);
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  @ApiOperation("Retrieve an active task by runId")
  public Optional<SingularityTaskId> getTaskByRunId(@Auth SingularityUser user, @PathParam("requestId") String requestId, @PathParam("runId") String runId) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);
    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.READ);
    return taskManager.getTaskByRunId(requestId, runId);
  }

  @POST
  @Path("/request/{requestId}/pause")
  public SingularityRequestParent pause(@Auth SingularityUser user,
                                        @PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext) {
    return pause(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/pause")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is already paused or being cleaned"),
  })
  public SingularityRequestParent pause(@Auth SingularityUser user,
                                        @ApiParam("The request ID to pause") @PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        @ApiParam("Pause Request Options") SingularityPauseRequest pauseRequest) {
    final Optional<SingularityPauseRequest> maybePauseRequest = Optional.fromNullable(pauseRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybePauseRequest.orNull(), () -> pause(requestId, maybePauseRequest, user));
  }

  public SingularityRequestParent pause(String requestId, Optional<SingularityPauseRequest> pauseRequest, SingularityUser user) {

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to pause (it must be manually unpaused first)", requestWithState.getRequest().getId());

    Optional<Boolean> killTasks = Optional.absent();
    Optional<String> message = Optional.absent();
    Optional<String> actionId = Optional.absent();
    Optional<SingularityShellCommand> runBeforeKill = Optional.absent();

    if (pauseRequest.isPresent()) {
      killTasks = pauseRequest.get().getKillTasks();
      message = pauseRequest.get().getMessage();
      if (pauseRequest.get().getRunShellCommandBeforeKill().isPresent()) {
        validator.checkValidShellCommand(pauseRequest.get().getRunShellCommandBeforeKill().get());
        runBeforeKill = pauseRequest.get().getRunShellCommandBeforeKill();
      }

      if (pauseRequest.get().getDurationMillis().isPresent() && !actionId.isPresent()) {
        actionId = Optional.of(UUID.randomUUID().toString());
      }
    }

    final long now = System.currentTimeMillis();
    Optional<Boolean> removeFromLoadBalancer = Optional.absent();

    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user.getEmail(),
        RequestCleanupType.PAUSING, now, killTasks, removeFromLoadBalancer, requestId, Optional.<String> absent(), Optional.<Boolean> absent(), message, actionId, runBeforeKill));

    checkConflict(result == SingularityCreateResult.CREATED, "%s is already pausing - try again soon", requestId, result);

    mailer.sendRequestPausedMail(requestWithState.getRequest(), pauseRequest, user.getEmail());

    requestManager.pause(requestWithState.getRequest(), now, user.getEmail(), message);

    if (pauseRequest.isPresent() && pauseRequest.get().getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringPause(requestId, user.getEmail(),
          System.currentTimeMillis(), pauseRequest.get(), actionId.get()));
    }

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.PAUSED, now));
  }

  @POST
  @Path("/request/{requestId}/unpause")
  public SingularityRequestParent unpauseNoBody(@Auth SingularityUser user,
                                                @PathParam("requestId") String requestId,
                                                @Context HttpServletRequest requestContext) {
    return unpause(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/unpause")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Unpause a Singularity Request, scheduling new tasks immediately", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is not paused"),
  })
  public SingularityRequestParent unpause(@Auth SingularityUser user,
                                          @ApiParam("The request ID to unpause") @PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          SingularityUnpauseRequest unpauseRequest) {
    final Optional<SingularityUnpauseRequest> maybeUnpauseRequest = Optional.fromNullable(unpauseRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybeUnpauseRequest.orNull(), () -> unpause(requestId, maybeUnpauseRequest, user));
  }

  public SingularityRequestParent unpause(String requestId, Optional<SingularityUnpauseRequest> unpauseRequest, SingularityUser user) {

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() == RequestState.PAUSED, "Request %s is not in PAUSED state, it is in %s", requestId, requestWithState.getState());

    Optional<String> message = Optional.absent();
    Optional<Boolean> skipHealthchecks = Optional.absent();

    if (unpauseRequest.isPresent()) {
      message = unpauseRequest.get().getMessage();
      skipHealthchecks = unpauseRequest.get().getSkipHealthchecks();
    }

    requestManager.deleteExpiringObject(SingularityExpiringPause.class, requestId);

    final long now = requestHelper.unpause(requestWithState.getRequest(), user.getEmail(), message, skipHealthchecks);

    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.ACTIVE, now));
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  public SingularityRequestParent exitCooldown(@Auth SingularityUser user,
                                               @PathParam("requestId") String requestId,
                                               @Context HttpServletRequest requestContext) {
    return exitCooldown(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Immediately exits cooldown, scheduling new tasks immediately", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=409, message="Request is not in cooldown"),
  })
  public SingularityRequestParent exitCooldown(@Auth SingularityUser user,
                                               @PathParam("requestId") String requestId,
                                               @Context HttpServletRequest requestContext,
                                               SingularityExitCooldownRequest exitCooldownRequest) {
    final Optional<SingularityExitCooldownRequest> maybeExitCooldownRequest = Optional.fromNullable(exitCooldownRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybeExitCooldownRequest.orNull(), () -> exitCooldown(requestId, maybeExitCooldownRequest, user));
  }

  public SingularityRequestParent exitCooldown(String requestId, Optional<SingularityExitCooldownRequest> exitCooldownRequest, SingularityUser user) {
    final SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

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

    requestManager.exitCooldown(requestWithState.getRequest(), now, Optional.of(user.getId()), message);

    if (maybeDeployId.isPresent() && !requestWithState.getRequest().isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), now, Optional.of(user.getId()),
          PendingType.IMMEDIATE, skipHealthchecks, message));
    }

    return fillEntireRequest(requestWithState);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @ApiOperation(value="Retrieve the list of active requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getActiveRequests(@Auth SingularityUser user,
                                                          @QueryParam("useWebCache") Boolean useWebCache,
                                                          @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
                                                          @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
                                                          @QueryParam("limit") Integer limit,
                                                          @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getActiveRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.fromNullable(limit), requestTypes);
  }


  @GET
  @PropertyFiltering
  @Path("/paused")
  @ApiOperation(value="Retrieve the list of paused requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getPausedRequests(@Auth SingularityUser user,
                                                          @QueryParam("useWebCache") Boolean useWebCache,
                                                          @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
                                                          @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
                                                          @QueryParam("limit") Integer limit,
                                                          @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getPausedRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.fromNullable(limit), requestTypes);
  }

  @GET
  @PropertyFiltering
  @Path("/cooldown")
  @ApiOperation(value="Retrieve the list of requests in system cooldown", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getCooldownRequests(@Auth SingularityUser user,
                                                            @QueryParam("useWebCache") Boolean useWebCache,
                                                            @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
                                                            @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
                                                            @QueryParam("limit") Integer limit,
                                                            @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getCooldownRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.fromNullable(limit), requestTypes);
  }

  @GET
  @PropertyFiltering
  @Path("/finished")
  @ApiOperation(value="Retreive the list of finished requests (Scheduled requests which have exhausted their schedules)", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getFinishedRequests(@Auth SingularityUser user,
                                                            @QueryParam("useWebCache") Boolean useWebCache,
                                                            @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
                                                            @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
                                                            @QueryParam("limit") Integer limit,
                                                            @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getFinishedRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.fromNullable(limit), requestTypes);
  }

  @GET
  @PropertyFiltering
  @ApiOperation(value="Retrieve the list of all requests", response=SingularityRequestParent.class, responseContainer="List")
  public List<SingularityRequestParent> getRequests(@Auth SingularityUser user,
                                                    @QueryParam("useWebCache") Boolean useWebCache,
                                                    @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
                                                    @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
                                                    @QueryParam("limit") Integer limit,
                                                    @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(requestManager.getRequests(useWebCache(useWebCache)), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.fromNullable(limit), requestTypes);
  }

  private boolean valueOrFalse(Boolean input) {
    return input == null ? false : input;
  }

  private List<SingularityRequestWithState> filterAutorized(List<SingularityRequestWithState> requests, final SingularityAuthorizationScope scope, SingularityUser user) {
    if (!authorizationHelper.hasAdminAuthorization(user)) {
      return requests.stream()
          .filter((parent) -> authorizationHelper.isAuthorizedForRequest(parent.getRequest(), user, scope))
          .collect(Collectors.toList());
    }
    return requests;
  }

  @GET
  @PropertyFiltering
  @Path("/queued/pending")
  @ApiOperation(value="Retrieve the list of pending requests", response=SingularityPendingRequest.class, responseContainer="List")
  public Iterable<SingularityPendingRequest> getPendingRequests(@Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, requestManager.getPendingRequests(), SingularityTransformHelpers.PENDING_REQUEST_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/queued/cleanup")
  @ApiOperation(value="Retrieve the list of requests being cleaned up", response=SingularityRequestCleanup.class, responseContainer="List")
  public Iterable<SingularityRequestCleanup> getCleanupRequests(@Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, requestManager.getCleanupRequests(), SingularityTransformHelpers.REQUEST_CLEANUP_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @Path("/request/{requestId}")
  @ApiOperation(value="Retrieve a specific Request by ID", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent getRequest(@Auth SingularityUser user,
                                             @ApiParam("Request ID") @PathParam("requestId") String requestId,
                                             @QueryParam("useWebCache") Boolean useWebCache) {
    return fillEntireRequest(fetchRequestWithState(requestId, useWebCache(useWebCache), user));
  }

  public SingularityRequestParent getRequest(String requestId, SingularityUser user) {
    return fillEntireRequest(fetchRequestWithState(requestId, false, user));
  }

  @DELETE
  @Path("/request/{requestId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Delete a specific Request by ID and return the deleted Request", response=SingularityRequest.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequest deleteRequest(@Auth SingularityUser user,
                                          @ApiParam("The request ID to delete.") @PathParam("requestId") String requestId,
                                          @Context HttpServletRequest requestContext,
                                          @ApiParam("Delete options") SingularityDeleteRequestRequest deleteRequest) {
    final Optional<SingularityDeleteRequestRequest> maybeDeleteRequest = Optional.fromNullable(deleteRequest);
    return maybeProxyToLeader(requestContext, SingularityRequest.class, maybeDeleteRequest.orNull(), () -> deleteRequest(requestId, maybeDeleteRequest, user));
  }

  public SingularityRequest deleteRequest(String requestId, Optional<SingularityDeleteRequestRequest> deleteRequest, SingularityUser user) {
    SingularityRequest request = fetchRequestWithState(requestId, user).getRequest();

    authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.REMOVE_REQUEST);

    Optional<String> message = Optional.absent();
    Optional<String> actionId = Optional.absent();
    Optional<Boolean> deleteFromLoadBalancer = Optional.absent();

    if (deleteRequest.isPresent()) {
      actionId = deleteRequest.get().getActionId();
      message = deleteRequest.get().getMessage();
      deleteFromLoadBalancer = deleteRequest.get().getDeleteFromLoadBalancer();
    }

    requestManager.startDeletingRequest(request, deleteFromLoadBalancer, user.getEmail(), actionId, message);

    mailer.sendRequestRemovedMail(request, user.getEmail(), message);

    return request;
  }

  @PUT
  @Path("/request/{requestId}/scale")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Scale the number of instances up or down for a specific Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent scale(@Auth SingularityUser user,
                                        @ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
                                        @Context HttpServletRequest requestContext,
                                        @ApiParam("Object to hold number of instances to request") SingularityScaleRequest scaleRequest) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, scaleRequest, () -> scale(requestId, scaleRequest, user));
  }

  public SingularityRequestParent scale(String requestId, SingularityScaleRequest scaleRequest, SingularityUser user) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId, user);

    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    authorizationHelper.checkForAuthorization(oldRequest, user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.SCALE_REQUEST);

    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(scaleRequest.getInstances()).build();
    validator.checkScale(newRequest, Optional.<Integer>absent());

    checkBadRequest(oldRequest.getInstancesSafe() != newRequest.getInstancesSafe(), "Scale request has no affect on the # of instances (%s)", newRequest.getInstancesSafe());
    String scaleMessage = String.format("Scaling from %d -> %d", oldRequest.getInstancesSafe(), newRequest.getInstancesSafe());
    if (scaleRequest.getMessage().isPresent()) {
      scaleMessage = String.format("%s -- %s", scaleRequest.getMessage().get(), scaleMessage);
    } else {
      scaleMessage = String.format("%s", scaleMessage);
    }

    if (scaleRequest.getBounce().or(newRequest.getBounceAfterScale().or(false))) {
      validator.checkActionEnabled(SingularityAction.BOUNCE_REQUEST);

      checkBadRequest(newRequest.isLongRunning(), "Can not bounce a %s request (%s)", newRequest.getRequestType(), newRequest);
      checkConflict(oldRequestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to bounce (it must be manually unpaused first)", newRequest.getId());
      checkConflict(!requestManager.cleanupRequestExists(newRequest.getId(), RequestCleanupType.BOUNCE), "Request %s is already bouncing cannot bounce again", newRequest.getId());

      final boolean isIncrementalBounce = scaleRequest.getIncremental().or(true);

      validator.checkResourcesForBounce(newRequest, isIncrementalBounce);
      validator.checkRequestForPriorityFreeze(newRequest);

      SingularityBounceRequest bounceRequest = new SingularityBounceRequest(Optional.of(isIncrementalBounce), scaleRequest.getSkipHealthchecks(), Optional.<Long>absent(), Optional.of(UUID.randomUUID().toString()), Optional.<String>absent(), Optional.<SingularityShellCommand>absent());

      submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.of(RequestHistoryType.SCALED), scaleRequest.getSkipHealthchecks(), Optional.of(scaleMessage), Optional.of(bounceRequest), user);
    } else {
      submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.of(RequestHistoryType.SCALED), scaleRequest.getSkipHealthchecks(), Optional.of(scaleMessage), Optional.absent(), user);
    }

    if (scaleRequest.getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringScale(requestId, user.getEmail(),
          System.currentTimeMillis(), scaleRequest, oldRequest.getInstances(), scaleRequest.getActionId().or(UUID.randomUUID().toString()), scaleRequest.getBounce()));
    }

    if (!scaleRequest.getSkipEmailNotification().isPresent() || !scaleRequest.getSkipEmailNotification().get()) {
      mailer.sendRequestScaledMail(newRequest, Optional.of(scaleRequest), oldRequest.getInstances(), user.getEmail());
    }

    return fillEntireRequest(fetchRequestWithState(requestId, user));
  }

  private <T extends SingularityExpiringRequestActionParent<?>> SingularityRequestParent deleteExpiringObject(Class<T> clazz, String requestId, SingularityUser user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

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
  public SingularityRequestParent deleteExpiringScale(@Auth SingularityUser user,
                                                      @ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringScale.class, requestId, user);
  }

  @Deprecated
  @DELETE
  @Path("/request/{requestId}/skipHealthchecks")
  @ApiOperation(value="Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
      @ApiResponse(code=404, message="No Request or expiring skipHealthchecks request for that ID"),
  })
  public SingularityRequestParent deleteExpiringSkipHealthchecksDeprecated(@Auth SingularityUser user,
                                                                           @ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringSkipHealthchecks(user, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/skip-healthchecks")
  @ApiOperation(value="Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring skipHealthchecks request for that ID"),
  })
  public SingularityRequestParent deleteExpiringSkipHealthchecks(@Auth SingularityUser user,
                                                                 @ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringSkipHealthchecks.class, requestId, user);
  }

  @DELETE
  @Path("/request/{requestId}/pause")
  @ApiOperation(value="Delete/cancel the expiring pause. This makes the pause request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring pause request for that ID"),
  })
  public SingularityRequestParent deleteExpiringPause(@Auth SingularityUser user,
                                                      @ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringPause.class, requestId, user);
  }

  @DELETE
  @Path("/request/{requestId}/bounce")
  @ApiOperation(value="Delete/cancel the expiring bounce. This makes the bounce request permanent.", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request or expiring bounce request for that ID"),
  })
  public SingularityRequestParent deleteExpiringBounce(@Auth SingularityUser user,
                                                       @ApiParam("The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringBounce.class, requestId, user);
  }

  @Deprecated
  @PUT
  @Path("/request/{requestId}/skipHealthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Update the skipHealthchecks field for the request, possibly temporarily", response=SingularityRequestParent.class)
  @ApiResponses({
      @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent skipHealthchecksDeprecated(@Auth SingularityUser user,
                                                             @ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
                                                             @Context HttpServletRequest requestContext,
                                                             @ApiParam("SkipHealtchecks options") SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return skipHealthchecks(user, requestId, requestContext, skipHealthchecksRequest);
  }

  @PUT
  @Path("/request/{requestId}/skip-healthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Update the skipHealthchecks field for the request, possibly temporarily", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=404, message="No Request with that ID"),
  })
  public SingularityRequestParent skipHealthchecks(@Auth SingularityUser user,
                                                   @ApiParam("The Request ID to scale") @PathParam("requestId") String requestId,
                                                   @Context HttpServletRequest requestContext,
                                                   @ApiParam("SkipHealtchecks options") SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, skipHealthchecksRequest, () -> skipHealthchecks(requestId, skipHealthchecksRequest, user));
  }

  public SingularityRequestParent skipHealthchecks(String requestId, SingularitySkipHealthchecksRequest skipHealthchecksRequest, SingularityUser user) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId, user);

    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    SingularityRequest newRequest = oldRequest.toBuilder().setSkipHealthchecks(skipHealthchecksRequest.getSkipHealthchecks()).build();

    submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.absent(), Optional.absent(), skipHealthchecksRequest.getMessage(), Optional.absent(), user);

    if (skipHealthchecksRequest.getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringSkipHealthchecks(requestId, user.getEmail(),
          System.currentTimeMillis(), skipHealthchecksRequest, oldRequest.getSkipHealthchecks(), skipHealthchecksRequest.getActionId().or(UUID.randomUUID().toString())));
    }

    return fillEntireRequest(fetchRequestWithState(requestId, user));
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @ApiOperation("Retrieve the list of tasks being cleaned from load balancers.")
  public Iterable<String> getLbCleanupRequests(@Auth SingularityUser user,
                                               @QueryParam("useWebCache") Boolean useWebCache) {
    return authorizationHelper.filterAuthorizedRequestIds(user, requestManager.getLbCleanupRequestIds(), SingularityAuthorizationScope.READ, useWebCache(useWebCache));
  }
}
