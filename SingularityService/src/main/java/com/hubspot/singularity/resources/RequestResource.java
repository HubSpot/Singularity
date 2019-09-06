package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import javax.ws.rs.core.Response;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestParent;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBatch;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityShellCommand;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.TaskCleanupType;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityExitCooldownRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularitySkipHealthchecksRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;
import com.hubspot.singularity.api.SingularityUpdateGroupsRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringRequestActionParent;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.helpers.RebalancingHelper;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.ning.http.client.AsyncHttpClient;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.REQUEST_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manages Singularity Requests, the parent object for any deployed task")
@Tags({@Tag(name = "Requests")})
public class RequestResource extends AbstractRequestResource {
  private static final Logger LOG = LoggerFactory.getLogger(RequestResource.class);

  private final SingularityMailer mailer;
  private final TaskManager taskManager;
  private final RebalancingHelper rebalancingHelper;
  private final RequestHelper requestHelper;
  private final SlaveManager slaveManager;
  private final RackManager rackManager;
  private final SingularityConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, TaskManager taskManager, RebalancingHelper rebalancingHelper,
                         RequestManager requestManager, SingularityMailer mailer,
                         SingularityAuthorizationHelper authorizationHelper, RequestHelper requestHelper, LeaderLatch leaderLatch,
                         SlaveManager slaveManager, AsyncHttpClient httpClient, @Singularity ObjectMapper objectMapper,
                         RackManager rackManager, SingularityConfiguration configuration, SingularityExceptionNotifier exceptionNotifier) {
    super(requestManager, deployManager, validator, authorizationHelper, httpClient, leaderLatch, objectMapper, requestHelper);
    this.mailer = mailer;
    this.taskManager = taskManager;
    this.rebalancingHelper = rebalancingHelper;
    this.requestHelper = requestHelper;
    this.slaveManager = slaveManager;
    this.rackManager = rackManager;
    this.configuration = configuration;
    this.exceptionNotifier = exceptionNotifier;
  }

  private void submitRequest(SingularityRequest request, Optional<SingularityRequestWithState> oldRequestWithState, Optional<RequestHistoryType> historyType,
      Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<SingularityBounceRequest> maybeBounceRequest, SingularityUser user) {
    checkNotNullBadRequest(request.getId(), "Request must have an id");
    checkConflict(!requestManager.cleanupRequestExists(request.getId()), "Request %s is currently cleaning. Try again after a few moments", request.getId());

    Optional<SingularityPendingDeploy> maybePendingDeploy = deployManager.getPendingDeploy(request.getId());
    checkConflict(!(maybePendingDeploy.isPresent() && maybePendingDeploy.get().getUpdatedRequest().isPresent()), "Request %s has a pending deploy that may change the request data. Try again when the deploy has finished", request.getId());

    Optional<SingularityRequest> oldRequest = oldRequestWithState.isPresent() ? Optional.of(oldRequestWithState.get().getRequest()) : Optional.<SingularityRequest>empty();

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
      validator.checkScale(request, Optional.empty());
    }

    authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);

    RequestState requestState = RequestState.ACTIVE;

    if (oldRequestWithState.isPresent()) {
      requestState = oldRequestWithState.get().getState();
    }

    if (oldRequest.isPresent() && request.getInstancesSafe() < oldRequest.get().getInstancesSafe()) {
      // Trigger cleanups for scale down
      int newInstances = request.getInstancesSafe();
      Optional<SingularityRequestDeployState> maybeDeployState = deployManager.getRequestDeployState(request.getId());
      if (maybeDeployState.isPresent() && maybeDeployState.get().getActiveDeploy().isPresent()) {
        List<SingularityTaskId> remainingActiveTasks = new ArrayList<>();
        taskManager.getActiveTaskIdsForDeploy(request.getId(), maybeDeployState.get().getActiveDeploy().get().getDeployId()).forEach((taskId) -> {
          if (taskId.getInstanceNo() > newInstances) {
            taskManager.createTaskCleanup(new SingularityTaskCleanup(
                Optional.of(user.getId()),
                TaskCleanupType.SCALING_DOWN,
                System.currentTimeMillis(),
                taskId,
                message,
                Optional.of(UUID.randomUUID().toString()),
                Optional.empty()
            ));
          } else {
            remainingActiveTasks.add(taskId);
          }
        });


        if (oldRequest.get().getInstancesSafe() > rackManager.getNumActive()) {
          if (request.isRackSensitive() && configuration.isRebalanceRacksOnScaleDown()) {
            rebalancingHelper.rebalanceRacks(request, remainingActiveTasks, user.getEmail());
          }
        }
        if (request.getSlaveAttributeMinimums().isPresent()) {
          Set<SingularityTaskId> cleanedTasks = rebalancingHelper.rebalanceAttributeDistribution(request, user.getEmail(), remainingActiveTasks);
          remainingActiveTasks.removeAll(cleanedTasks);
        }
      }
    }

    requestHelper.updateRequest(request, oldRequest, requestState, historyType, user.getEmail(), skipHealthchecks, message, maybeBounceRequest);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Create or update a Singularity Request",
      responses = {
          @ApiResponse(responseCode = "400", description = "Request object is invalid"),
          @ApiResponse(responseCode = "409", description = "Request object is being cleaned. Try again shortly"),
      }
  )
  public SingularityRequestParent postRequest(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Context HttpServletRequest requestContext,
      @RequestBody(required = true, description = "The Singularity request to create or update") SingularityRequest request) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, request, () -> postRequest(request, user));
  }

  public SingularityRequestParent postRequest(SingularityRequest request, SingularityUser user) {
    submitRequest(request, requestManager.getRequest(request.getId()), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), user);
    return fillEntireRequest(fetchRequestWithState(request.getId(), user));
  }

  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    checkConflict(maybeDeployId.isPresent(), "Can not schedule/bounce a request (%s) with no deploy", requestId);

    return maybeDeployId.get();
  }

  @POST
  @Path("/request/{requestId}/groups")
  @Operation(
      summary = "Update the group, readOnlyGroups, and readWriteGroups for a SingularityRequest",
      responses = {
          @ApiResponse(responseCode = "400", description = "Request object is invalid"),
          @ApiResponse(responseCode = "401", description = "User is not authorized to make these updates"),
      }
  )
  public SingularityRequestParent updateAuthorizedGroups(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The id of the request to update") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(required = true, description = "Updated group settings") SingularityUpdateGroupsRequest updateGroupsRequest) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, updateGroupsRequest, () -> updateAuthorizedGroups(user, requestId, updateGroupsRequest));
  }

  private SingularityRequestParent updateAuthorizedGroups(SingularityUser user, String requestId, SingularityUpdateGroupsRequest updateGroupsRequest) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId, user);
    authorizationHelper.checkForAuthorization(oldRequestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    SingularityRequest newRequest = oldRequestWithState.getRequest().toBuilder()
        .setGroup(updateGroupsRequest.getGroup())
        .setReadWriteGroups(Optional.of(updateGroupsRequest.getReadWriteGroups()))
        .setReadOnlyGroups(Optional.of(updateGroupsRequest.getReadOnlyGroups()))
        .build();

    submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.of(RequestHistoryType.UPDATED), Optional.empty(), updateGroupsRequest.getMessage(), Optional.empty(), user);
    return fillEntireRequest(fetchRequestWithState(requestId, user));
  }

  @POST
  @Path("/request/{requestId}/groups/auth-check")
  @Operation(
      summary = "Check authorization for updating the group, readOnlyGroups, and readWriteGroups for a SingularityRequest, without committing the change",
      responses = {
          @ApiResponse(responseCode = "400", description = "Request object is invalid"),
          @ApiResponse(responseCode = "401", description = "User is not authorized to make these updates"),
      }
  )
  public Response checkAuthForGroupsUpdate(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The id of the request to update") @PathParam("requestId") String requestId,
      @RequestBody(required = true, description = "Updated group settings") SingularityUpdateGroupsRequest updateGroupsRequest) {
    Optional<SingularityRequestWithState> maybeOldRequestWithState = requestManager.getRequest(requestId, false);
    if (!maybeOldRequestWithState.isPresent()) {
      authorizationHelper.checkForAuthorization(
          user,
          Sets.union(updateGroupsRequest.getGroup().map(Collections::singleton).orElse(Collections.emptySet()), updateGroupsRequest.getReadWriteGroups()),
          updateGroupsRequest.getReadOnlyGroups(),
          SingularityAuthorizationScope.WRITE,
          Optional.empty());
      return Response.ok().build();
    }
    SingularityRequestWithState oldRequestWithState = maybeOldRequestWithState.get();
    authorizationHelper.checkForAuthorization(oldRequestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    SingularityRequest newRequest = oldRequestWithState.getRequest().toBuilder()
        .setGroup(updateGroupsRequest.getGroup())
        .setReadWriteGroups(Optional.of(updateGroupsRequest.getReadWriteGroups()))
        .setReadOnlyGroups(Optional.of(updateGroupsRequest.getReadOnlyGroups()))
        .build();
    authorizationHelper.checkForAuthorizedChanges(newRequest, oldRequestWithState.getRequest(), user);
    return Response.ok().build();
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @Operation(summary = "Trigger a bounce for a request")
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  public SingularityRequestParent bounce(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request to bounce") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext) {
    return bounce(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/bounce")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(summary = "Bounce a specific Singularity request. A bounce launches replacement task(s), and then kills the original task(s) if the replacement(s) are healthy")
  public SingularityRequestParent bounce(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request ID to bounce") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "Bounce request options") SingularityBounceRequest bounceRequest) {
    final Optional<SingularityBounceRequest> maybeBounceRequest = Optional.ofNullable(bounceRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybeBounceRequest.orElse(null), () -> bounce(requestId, maybeBounceRequest, user));
  }

  public SingularityRequestParent bounce(String requestId, Optional<SingularityBounceRequest> bounceRequest, SingularityUser user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.BOUNCE_REQUEST);

    checkBadRequest(requestWithState.getRequest().isLongRunning(), "Can not bounce a %s request (%s)", requestWithState.getRequest().getRequestType(), requestWithState);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to bounce (it must be manually unpaused first)", requestWithState.getRequest().getId());

    final boolean isIncrementalBounce = bounceRequest.isPresent() && bounceRequest.get().getIncremental().orElse(false);

    validator.checkResourcesForBounce(requestWithState.getRequest(), isIncrementalBounce);
    validator.checkRequestForPriorityFreeze(requestWithState.getRequest());

    final Optional<Boolean> skipHealthchecks = bounceRequest.isPresent() ? bounceRequest.get().getSkipHealthchecks() : Optional.<Boolean>empty();

    Optional<String> message = Optional.empty();
    Optional<String> actionId = Optional.empty();
    Optional<SingularityShellCommand> runBeforeKill = Optional.empty();

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
            System.currentTimeMillis(), Optional.empty(), Optional.empty(), requestId, Optional.of(deployId), skipHealthchecks, message, actionId, runBeforeKill));

    requestManager.bounce(requestWithState.getRequest(), System.currentTimeMillis(), Optional.of(user.getId()), message);

    final SingularityBounceRequest validatedBounceRequest = validator.checkBounceRequest(bounceRequest.orElse(SingularityBounceRequest.defaultRequest()));

    requestManager.saveExpiringObject(new SingularityExpiringBounce(requestId, deployId, Optional.of(user.getId()),
        System.currentTimeMillis(), validatedBounceRequest, actionId.get()));

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Path("/request/{requestId}/run")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Schedule a one-off or scheduled Singularity request for immediate or delayed execution",
      responses = {
          @ApiResponse(responseCode = "400", description = "Singularity Request is not scheduled or one-off"),
      }
  )
  public SingularityPendingRequestParent scheduleImmediately(
        @Parameter(hidden = true) @Auth SingularityUser user,
        @Parameter(required = true, description = "The request ID to run") @PathParam("requestId") String requestId,
        @Parameter(hidden = true) @Context HttpServletRequest requestContext,
        @QueryParam("minimal") Boolean minimalReturn,
        @RequestBody(description = "Settings specific to this run of the request") SingularityRunNowRequest runNowRequest) {
    if (runNowRequest != null) {
      runNowRequest.getEnvOverrides().forEach((k, v) -> {
        checkBadRequest(!k.equals("STARTED_BY_USER") && !v.contains("STARTED_BY_USER"), "Cannot override STARTED_BY_USER in env");
      });
      checkBadRequest(!runNowRequest.getCommandLineArgs().isPresent() || runNowRequest.getCommandLineArgs()
          .get()
          .stream()
          .noneMatch((arg) -> arg.contains("STARTED_BY_USER")), "Cannot override STARTED_BY_USER");
    }
    long start = System.currentTimeMillis();
    SingularityPendingRequestParent response;
    if (configuration.isProxyRunNowToLeader()) {
      response = maybeProxyToLeader(requestContext, SingularityPendingRequestParent.class, runNowRequest, () -> scheduleImmediately(user, requestId, runNowRequest, Optional.ofNullable(minimalReturn).orElse(false)));
    } else {
      response = scheduleImmediately(user, requestId, runNowRequest, Optional.ofNullable(minimalReturn).orElse(false));
    }
    long duration = System.currentTimeMillis() - start;
    LOG.trace("Enqueue for {} took {}ms", requestId, duration);
    if (duration > 15000) {
      exceptionNotifier.notify(
          String.format("Slow enqueue for %s", requestId),
          ImmutableMap.of("leader", Boolean.toString(isLeader()), "duration", Long.toString(duration))
      );
    }
    return response;
  }

  public SingularityPendingRequestParent scheduleImmediately(SingularityUser user, String requestId, SingularityRunNowRequest runNowRequest) {
    return scheduleImmediately(user, requestId, runNowRequest, false);
  }

  public SingularityPendingRequestParent scheduleImmediately(SingularityUser user, String requestId, SingularityRunNowRequest runNowRequest, boolean minimalReturn) {
    final Optional<SingularityRunNowRequest> maybeRunNowRequest = Optional.ofNullable(runNowRequest);
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to run now (it must be manually unpaused first)", requestWithState.getRequest().getId());

    // Check these to avoid unnecessary calls to taskManager
    int activeTasks = 0;
    int pendingTasks = 0;

    boolean isOneoffWithInstances = requestWithState.getRequest().isOneOff() && requestWithState.getRequest().getInstances().isPresent();
    if (requestWithState.getRequest().isScheduled() || isOneoffWithInstances) {
      activeTasks = taskManager.getActiveTaskIdsForRequest(requestId).size();
    }
    if (isOneoffWithInstances) {
      pendingTasks = taskManager.getPendingTaskIdsForRequest(requestId).size();
    }

    final SingularityPendingRequest pendingRequest = validator.checkRunNowRequest(
        getAndCheckDeployId(requestId),
        user.getEmail(),
        requestWithState.getRequest(),
        maybeRunNowRequest,
        activeTasks,
        pendingTasks);

    SingularityCreateResult result = requestManager.addToPendingQueue(pendingRequest);

    checkConflict(result != SingularityCreateResult.EXISTED, "%s is already pending, please try again soon", requestId);

    if (minimalReturn) {
      return SingularityPendingRequestParent.minimalFromRequestWithState(requestWithState, pendingRequest);
    } else {
      return SingularityPendingRequestParent.fromSingularityRequestParent(fillEntireRequest(requestWithState), pendingRequest);
    }
  }

  @GET
  @Path("/request/{requestId}/run/{runId}")
  @Operation(
      summary = "Retrieve an active task by runId",
      responses = {
          @ApiResponse(responseCode = "404", description = "A task with the specified runID was not found")
      }
  )
  public Optional<SingularityTaskId> getTaskByRunId(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the request") @PathParam("requestId") String requestId,
      @Parameter(required = true, description = "Run id to search for") @PathParam("runId") String runId) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);
    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.READ);
    return taskManager.getTaskByRunId(requestId, runId);
  }

  @POST
  @Path("/request/{requestId}/pause")
  @Operation(
      summary = "Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks",
      responses = {
          @ApiResponse(responseCode = "409", description = "Request is already paused or being cleaned"),
      }
  )
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  public SingularityRequestParent pause(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request ID to pause") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext) {
    return pause(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/pause")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Pause a Singularity request, future tasks will not run until it is manually unpaused. API can optionally choose to kill existing tasks",
      responses = {
          @ApiResponse(responseCode = "409", description = "Request is already paused or being cleaned"),
      }
  )
  public SingularityRequestParent pause(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request ID to pause") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "Pause Request Options") SingularityPauseRequest pauseRequest) {
    final Optional<SingularityPauseRequest> maybePauseRequest = Optional.ofNullable(pauseRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybePauseRequest.orElse(null), () -> pause(requestId, maybePauseRequest, user));
  }

  public SingularityRequestParent pause(String requestId, Optional<SingularityPauseRequest> pauseRequest, SingularityUser user) {

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to pause (it must be manually unpaused first)", requestWithState.getRequest().getId());

    Optional<Boolean> killTasks = Optional.empty();
    Optional<String> message = Optional.empty();
    Optional<String> actionId = Optional.empty();
    Optional<SingularityShellCommand> runBeforeKill = Optional.empty();

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
    Optional<Boolean> removeFromLoadBalancer = Optional.empty();

    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user.getEmail(),
        RequestCleanupType.PAUSING, now, killTasks, removeFromLoadBalancer, requestId, Optional.<String>empty(), Optional.<Boolean>empty(), message, actionId, runBeforeKill));

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
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  public SingularityRequestParent unpauseNoBody(@Parameter(hidden = true) @Auth SingularityUser user,
                                                @PathParam("requestId") String requestId,
                                                @Context HttpServletRequest requestContext) {
    return unpause(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/unpause")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Unpause a Singularity Request, scheduling new tasks immediately",
      responses = {
          @ApiResponse(responseCode = "409", description = "Request is not paused")
      }
  )
  public SingularityRequestParent unpause(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request ID to unpause") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "Settings for how the unpause should behave") SingularityUnpauseRequest unpauseRequest) {
    final Optional<SingularityUnpauseRequest> maybeUnpauseRequest = Optional.ofNullable(unpauseRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybeUnpauseRequest.orElse(null), () -> unpause(requestId, maybeUnpauseRequest, user));
  }

  public SingularityRequestParent unpause(String requestId, Optional<SingularityUnpauseRequest> unpauseRequest, SingularityUser user) {

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() == RequestState.PAUSED, "Request %s is not in PAUSED state, it is in %s", requestId, requestWithState.getState());

    Optional<String> message = Optional.empty();
    Optional<Boolean> skipHealthchecks = Optional.empty();

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
  @Operation(summary = "Immediately exits cooldown, scheduling new tasks immediately")
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
  public SingularityRequestParent exitCooldown(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request to operate on") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext) {
    return exitCooldown(user, requestId, requestContext, null);
  }

  @POST
  @Path("/request/{requestId}/exit-cooldown")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Immediately exits cooldown, scheduling new tasks immediately",
      responses = {
        @ApiResponse(responseCode = "409", description = "Request is not in cooldown")
      }
  )
  public SingularityRequestParent exitCooldown(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request to operate on") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "Settings related to how an exit cooldown should behave") SingularityExitCooldownRequest exitCooldownRequest) {
    final Optional<SingularityExitCooldownRequest> maybeExitCooldownRequest = Optional.ofNullable(exitCooldownRequest);
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, maybeExitCooldownRequest.orElse(null), () -> exitCooldown(requestId, maybeExitCooldownRequest, user));
  }

  public SingularityRequestParent exitCooldown(String requestId, Optional<SingularityExitCooldownRequest> exitCooldownRequest, SingularityUser user) {
    final SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    checkConflict(requestWithState.getState() == RequestState.SYSTEM_COOLDOWN, "Request %s is not in SYSTEM_COOLDOWN state, it is in %s", requestId, requestWithState.getState());

    final Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    final long now = System.currentTimeMillis();

    Optional<String> message = Optional.empty();
    Optional<Boolean> skipHealthchecks = Optional.empty();

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
  @Path("/batch")
  @Operation(summary = "Retrieve a specific batch of requests")
  public SingularityRequestBatch getRequestsBatch(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "List of request ids to fetch") @QueryParam("id") List<String> ids
  ) {
    List<SingularityRequestParent> found = filterAutorized(Lists.newArrayList(requestManager.getRequests(ids)), SingularityAuthorizationScope.READ, user)
        .stream()
        .map(this::fillEntireRequest)
        .collect(Collectors.toList());
    Set<String> notFound = new HashSet<>(ids);
    found.forEach((r) -> notFound.remove(r.getRequest().getId()));
    return new SingularityRequestBatch(found, notFound);
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  @Operation(summary = "Retrieve the list of active requests")
  public List<SingularityRequestParent> getActiveRequests(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache,
      @Parameter(description = "Only include requests that the user has operated on or is in a group for") @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
      @Parameter(description = "Return full data, including deploy data and active task ids") @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
      @Parameter(description = "The maximum number of results to return") @QueryParam("limit") Integer limit,
      @Parameter(description = "Only return requests of these types") @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getActiveRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.ofNullable(limit), requestTypes);
  }

  @GET
  @Path("/ids")
  @Operation(summary = "Retrieve the list of all request ids")
  public List<String> getAllRequestIds(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache,
      @Parameter(description = "Filter to request ids that match this string (case insensitive)") @QueryParam("requestIdLike") String requestIdLike,
      @Parameter(description = "Filter by request state") @QueryParam("state") Set<RequestState> states) {
    List<String> allIds = filterAutorized(Lists.newArrayList(requestManager.getRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user)
        .stream()
        .filter((r) -> states == null || states.isEmpty() || states.contains(r.getState()))
        .map((r) -> r.getRequest().getId())
        .collect(Collectors.toList());
    if (requestIdLike == null) {
      return allIds;
    } else {
      String lowerCase = requestIdLike.toLowerCase();
      return allIds.stream()
          .filter((id) -> id.toLowerCase().startsWith(lowerCase))
          .collect(Collectors.toList());
    }
  }

  @GET
  @Path("/ids/active")
  @Operation(summary = "Retrieve the list of active request ids")
  public List<String> getActiveRequestIds(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache) {
    return filterAutorized(Lists.newArrayList(requestManager.getActiveRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user)
        .stream()
        .map((r) -> r.getRequest().getId())
        .collect(Collectors.toList());
  }


  @GET
  @PropertyFiltering
  @Path("/paused")
  @Operation(summary = "Retrieve the list of paused requests")
  public List<SingularityRequestParent> getPausedRequests(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache,
      @Parameter(description = "Only include requests that the user has operated on or is in a group for") @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
      @Parameter(description = "Return full data, including deploy data and active task ids") @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
      @Parameter(description = "The maximum number of results to return") @QueryParam("limit") Integer limit,
      @Parameter(description = "Only return requests of these types") @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getPausedRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.ofNullable(limit), requestTypes);
  }

  @GET
  @PropertyFiltering
  @Path("/cooldown")
  @Operation(summary = "Retrieve the list of requests in system cooldown")
  public List<SingularityRequestParent> getCooldownRequests(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache,
      @Parameter(description = "Only include requests that the user has operated on or is in a group for") @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
      @Parameter(description = "Return full data, including deploy data and active task ids") @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
      @Parameter(description = "The maximum number of results to return") @QueryParam("limit") Integer limit,
      @Parameter(description = "Only return requests of these types") @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getCooldownRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.ofNullable(limit), requestTypes);
  }

  @GET
  @PropertyFiltering
  @Path("/finished")
  @Operation(summary = "Retreive the list of finished requests (Scheduled requests which have exhausted their schedules)")
  public List<SingularityRequestParent> getFinishedRequests(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache,
                                                            @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
                                                            @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
                                                            @QueryParam("limit") Integer limit,
                                                            @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(Lists.newArrayList(requestManager.getFinishedRequests(useWebCache(useWebCache))), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.ofNullable(limit), requestTypes);
  }

  @GET
  @PropertyFiltering
  @Operation(summary = "Retrieve the list of all requests")
  public List<SingularityRequestParent> getRequests(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache,
      @Parameter(description = "Only include requests that the user has operated on or is in a group for") @QueryParam("filterRelevantForUser") Boolean filterRelevantForUser,
      @Parameter(description = "Return full data, including deploy data and active task ids") @QueryParam("includeFullRequestData") Boolean includeFullRequestData,
      @Parameter(description = "The maximum number of results to return") @QueryParam("limit") Integer limit,
      @Parameter(description = "Only return requests of these types") @QueryParam("requestType") List<RequestType> requestTypes) {
    return requestHelper.fillDataForRequestsAndFilter(
        filterAutorized(requestManager.getRequests(useWebCache(useWebCache)), SingularityAuthorizationScope.READ, user),
        user, valueOrFalse(filterRelevantForUser), valueOrFalse(includeFullRequestData), Optional.ofNullable(limit), requestTypes);
  }

  private boolean valueOrFalse(Boolean input) {
    return input == null ? false : input;
  }

  private List<SingularityRequestWithState> filterAutorized(List<SingularityRequestWithState> requests, final SingularityAuthorizationScope scope, SingularityUser user) {
    authorizationHelper.checkUserInRequiredGroups(user);
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
  @Operation(summary = "Retrieve the list of pending requests")
  public List<SingularityPendingRequest> getPendingRequests(@Parameter(hidden = true) @Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, requestManager.getPendingRequests(), SingularityTransformHelpers.PENDING_REQUEST_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @PropertyFiltering
  @Path("/queued/cleanup")
  @Operation(summary = "Retrieve the list of requests being cleaned up")
  public List<SingularityRequestCleanup> getCleanupRequests(@Parameter(hidden = true) @Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, requestManager.getCleanupRequests(), SingularityTransformHelpers.REQUEST_CLEANUP_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @GET
  @Path("/request/{requestId}")
  @Operation(
      summary = "Retrieve a specific Request by ID",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request with that ID")
      }
  )
  public SingularityRequestParent getRequest(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Request ID") @PathParam("requestId") String requestId,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache) {
    return fillEntireRequest(fetchRequestWithState(requestId, useWebCache(useWebCache), user));
  }

  public SingularityRequestParent getRequest(String requestId, SingularityUser user) {
    return fillEntireRequest(fetchRequestWithState(requestId, false, user));
  }

  @GET
  @Path("/request/{requestId}/simple")
  @Operation(
      summary = "Retrieve a specific Request by ID without additional deploy/task information",
      responses = {
          @ApiResponse(responseCode = "404", description = "No Request with that ID")
      }
  )
  public SingularityRequestWithState getRequestSimple(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Request ID") @PathParam("requestId") String requestId,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache) {
    return fetchRequestWithState(requestId, useWebCache(useWebCache), user);
  }

  @DELETE
  @Path("/request/{requestId}")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Delete a specific Request by ID and return the deleted Request",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request with that ID")
      }
  )
  public SingularityRequest deleteRequest(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The request ID to delete") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "Delete options") SingularityDeleteRequestRequest deleteRequest) {
    final Optional<SingularityDeleteRequestRequest> maybeDeleteRequest = Optional.ofNullable(deleteRequest);
    return maybeProxyToLeader(requestContext, SingularityRequest.class, maybeDeleteRequest.orElse(null), () -> deleteRequest(requestId, maybeDeleteRequest, user));
  }

  public SingularityRequest deleteRequest(String requestId, Optional<SingularityDeleteRequestRequest> deleteRequest, SingularityUser user) {
    SingularityRequest request = fetchRequestWithState(requestId, user).getRequest();

    authorizationHelper.checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.REMOVE_REQUEST);

    Optional<String> message = Optional.empty();
    Optional<String> actionId = Optional.empty();
    Optional<Boolean> deleteFromLoadBalancer = Optional.empty();

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
  @Operation(summary = "Scale the number of instances up or down for a specific Request",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request with that ID")
      }
  )
  public SingularityRequestParent scale(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID to scale") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(required = true, description = "Object to hold number of instances to request") SingularityScaleRequest scaleRequest) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, scaleRequest, () -> scale(requestId, scaleRequest, user));
  }

  public SingularityRequestParent scale(String requestId, SingularityScaleRequest scaleRequest, SingularityUser user) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId, user);

    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    authorizationHelper.checkForAuthorization(oldRequest, user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.SCALE_REQUEST);

    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(scaleRequest.getInstances()).build();
    validator.checkScale(newRequest, Optional.<Integer>empty());

    checkBadRequest(oldRequest.getInstancesSafe() != newRequest.getInstancesSafe(), "Scale request has no affect on the # of instances (%s)", newRequest.getInstancesSafe());
    String scaleMessage = String.format("Scaling from %d -> %d", oldRequest.getInstancesSafe(), newRequest.getInstancesSafe());
    if (scaleRequest.getMessage().isPresent()) {
      scaleMessage = String.format("%s -- %s", scaleRequest.getMessage().get(), scaleMessage);
    } else {
      scaleMessage = String.format("%s", scaleMessage);
    }

    if (scaleRequest.getBounce().orElse(newRequest.getBounceAfterScale().orElse(false))) {
      validator.checkActionEnabled(SingularityAction.BOUNCE_REQUEST);

      checkBadRequest(newRequest.isLongRunning(), "Can not bounce a %s request (%s)", newRequest.getRequestType(), newRequest);
      checkConflict(oldRequestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to bounce (it must be manually unpaused first)", newRequest.getId());
      checkConflict(!requestManager.cleanupRequestExists(newRequest.getId(), RequestCleanupType.BOUNCE), "Request %s is already bouncing cannot bounce again", newRequest.getId());

      final boolean isIncrementalBounce = scaleRequest.getIncremental().orElse(true);

      validator.checkResourcesForBounce(newRequest, isIncrementalBounce);
      validator.checkRequestForPriorityFreeze(newRequest);

      SingularityBounceRequest bounceRequest = new SingularityBounceRequest(Optional.of(isIncrementalBounce), scaleRequest.getSkipHealthchecks(), Optional.<Long>empty(), Optional.of(UUID.randomUUID().toString()), Optional.<String>empty(), Optional.<SingularityShellCommand>empty());

      submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.of(RequestHistoryType.SCALED), scaleRequest.getSkipHealthchecks(), Optional.of(scaleMessage), Optional.of(bounceRequest), user);
    } else {
      submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.of(RequestHistoryType.SCALED), scaleRequest.getSkipHealthchecks(), Optional.of(scaleMessage), Optional.empty(), user);
    }

    if (scaleRequest.getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringScale(requestId, user.getEmail(),
          System.currentTimeMillis(), scaleRequest, oldRequest.getInstances(), scaleRequest.getActionId().orElse(UUID.randomUUID().toString()), scaleRequest.getBounce()));
    } else {
      requestManager.deleteExpiringObject(SingularityExpiringScale.class, requestId);
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
  @Operation(
      summary = "Delete/cancel the expiring scale. This makes the scale request permanent",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request or expiring scale request for that ID")
      }
  )
  public SingularityRequestParent deleteExpiringScale(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringScale.class, requestId, user);
  }

  @Deprecated
  @DELETE
  @Path("/request/{requestId}/skipHealthchecks")
  @Operation(
      summary = "Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent",
      responses = {
          @ApiResponse(responseCode = "404", description = "No Request or expiring skipHealthchecks request for that ID")
      }
  )
  public SingularityRequestParent deleteExpiringSkipHealthchecksDeprecated(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringSkipHealthchecks(user, requestId);
  }

  @DELETE
  @Path("/request/{requestId}/skip-healthchecks")
  @Operation(
      summary = "Delete/cancel the expiring skipHealthchecks. This makes the skipHealthchecks request permanent",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request or expiring skipHealthchecks request for that ID")
      }
  )
  public SingularityRequestParent deleteExpiringSkipHealthchecks(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringSkipHealthchecks.class, requestId, user);
  }

  @DELETE
  @Path("/request/{requestId}/pause")
  @Operation(summary = "Delete/cancel the expiring pause. This makes the pause request permanent",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request or expiring pause request for that ID"),
  })
  public SingularityRequestParent deleteExpiringPause(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringPause.class, requestId, user);
  }

  @DELETE
  @Path("/request/{requestId}/bounce")
  @Operation(summary = "Delete/cancel the expiring bounce. This makes the bounce request permanent",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request or expiring bounce request for that ID"),
  })
  public SingularityRequestParent deleteExpiringBounce(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID") @PathParam("requestId") String requestId) {
    return deleteExpiringObject(SingularityExpiringBounce.class, requestId, user);
  }

  @Deprecated
  @PUT
  @Path("/request/{requestId}/skipHealthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Update the skipHealthchecks field for the request, possibly temporarily",
      responses = {
          @ApiResponse(responseCode = "404", description = "No Request with that ID")
      }
  )
  public SingularityRequestParent skipHealthchecksDeprecated(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID to scale") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "SkipHealtchecks options") SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return skipHealthchecks(user, requestId, requestContext, skipHealthchecksRequest);
  }

  @PUT
  @Path("/request/{requestId}/skip-healthchecks")
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Update the skipHealthchecks field for the request, possibly temporarily",
      responses = {
        @ApiResponse(responseCode = "404", description = "No Request with that ID"),
  })
  public SingularityRequestParent skipHealthchecks(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The Request ID to skip healthchecks for") @PathParam("requestId") String requestId,
      @Context HttpServletRequest requestContext,
      @RequestBody(description = "SkipHealtchecks options") SingularitySkipHealthchecksRequest skipHealthchecksRequest) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, skipHealthchecksRequest, () -> skipHealthchecks(requestId, skipHealthchecksRequest, user));
  }

  public SingularityRequestParent skipHealthchecks(String requestId, SingularitySkipHealthchecksRequest skipHealthchecksRequest, SingularityUser user) {
    SingularityRequestWithState oldRequestWithState = fetchRequestWithState(requestId, user);

    SingularityRequest oldRequest = oldRequestWithState.getRequest();
    SingularityRequest newRequest = oldRequest.toBuilder().setSkipHealthchecks(skipHealthchecksRequest.getSkipHealthchecks()).build();

    submitRequest(newRequest, Optional.of(oldRequestWithState), Optional.empty(), Optional.empty(), skipHealthchecksRequest.getMessage(), Optional.empty(), user);

    if (skipHealthchecksRequest.getDurationMillis().isPresent()) {
      requestManager.saveExpiringObject(new SingularityExpiringSkipHealthchecks(requestId, user.getEmail(),
          System.currentTimeMillis(), skipHealthchecksRequest, oldRequest.getSkipHealthchecks(), skipHealthchecksRequest.getActionId().orElse(UUID.randomUUID().toString())));
    }

    return fillEntireRequest(fetchRequestWithState(requestId, user));
  }

  @GET
  @PropertyFiltering
  @Path("/lbcleanup")
  @Operation(summary = "Retrieve the list of tasks being cleaned from load balancers.")
  public Iterable<String> getLbCleanupRequests(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Fetched a cached version of this data to limit expensive operations") @QueryParam("useWebCache") Boolean useWebCache) {
    return authorizationHelper.filterAuthorizedRequestIds(user, requestManager.getLbCleanupRequestIds(), SingularityAuthorizationScope.READ, useWebCache(useWebCache));
  }
}
