package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
import com.ning.http.client.AsyncHttpClient;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.DEPLOY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity Deploys for existing requests", value=ApiPaths.DEPLOY_RESOURCE_PATH, position=2)
public class DeployResource extends AbstractRequestResource {
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;

  @Inject
  public DeployResource(RequestManager requestManager, DeployManager deployManager, SingularityValidator validator, SingularityAuthorizationHelper authorizationHelper,
                        SingularityConfiguration configuration, TaskManager taskManager, LeaderLatch leaderLatch,
                        AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    super(requestManager, deployManager, validator, authorizationHelper, httpClient, leaderLatch, objectMapper);
    this.configuration = configuration;
    this.taskManager = taskManager;
  }

  @GET
  @PropertyFiltering
  @Path("/pending")
  @ApiOperation(response=SingularityPendingDeploy.class, responseContainer="List", value="Retrieve the list of current pending deploys")
  public Iterable<SingularityPendingDeploy> getPendingDeploys(@Auth SingularityUser user) {
    return authorizationHelper.filterByAuthorizedRequests(user, deployManager.getPendingDeploys(), SingularityTransformHelpers.PENDING_DEPLOY_TO_REQUEST_ID, SingularityAuthorizationScope.READ);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Start a new deployment for a Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Deploy object is invalid"),
    @ApiResponse(code=409, message="A current deploy is in progress. It may be canceled by calling DELETE"),
  })
  public SingularityRequestParent deploy(@Auth SingularityUser user, @Context HttpServletRequest requestContext, @ApiParam(required=true) SingularityDeployRequest deployRequest) {
    return maybeProxyToLeader(requestContext, SingularityRequestParent.class, deployRequest, () -> deploy(deployRequest, user));
  }

  public SingularityRequestParent deploy(SingularityDeployRequest deployRequest, SingularityUser user) {
    validator.checkActionEnabled(SingularityAction.DEPLOY);
    SingularityDeploy deploy = deployRequest.getDeploy();
    checkNotNullBadRequest(deploy, "DeployRequest must have a deploy object");

    final Optional<String> deployUser = user.getEmail();
    final String requestId = checkNotNullBadRequest(deploy.getRequestId(), "DeployRequest must have a non-null requestId");

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    SingularityRequest request = requestWithState.getRequest();
    final Optional<SingularityRequest> updatedValidatedRequest;
    if (deployRequest.getUpdatedRequest().isPresent()) {
      authorizationHelper.checkForAuthorizedChanges(deployRequest.getUpdatedRequest().get(), requestWithState.getRequest(), user);
      updatedValidatedRequest = Optional.of(validator.checkSingularityRequest(deployRequest.getUpdatedRequest().get(), Optional.of(requestWithState.getRequest()), Optional.<SingularityDeploy>absent(), Optional.of(deploy)));
    } else {
      updatedValidatedRequest = Optional.absent();
    }

    if (updatedValidatedRequest.isPresent()) {
      request = updatedValidatedRequest.get();
    }

    validator.checkScale(request, Optional.of(taskManager.getActiveTaskIdsForRequest(request.getId()).size()));

    if (!deployRequest.isUnpauseOnSuccessfulDeploy()) {
      checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to deploy (it must be manually unpaused first)", requestWithState.getRequest().getId());
    }

    deploy = validator.checkDeploy(request, deploy, taskManager.getActiveTaskIdsForRequest(requestId), taskManager.getPendingTaskIdsForRequest(requestId));

    final long now = System.currentTimeMillis();

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(requestId, deploy.getId(), now, deployUser, deployRequest.getMessage());

    Optional<SingularityDeployProgress> deployProgress = Optional.absent();
    if (request.isLongRunning()) {
      deployProgress = Optional.of(new SingularityDeployProgress(
          Math.min(deploy.getDeployInstanceCountPerStep().or(request.getInstancesSafe()), request.getInstancesSafe()),
          0,
          deploy.getDeployInstanceCountPerStep().or(request.getInstancesSafe()),
          deploy.getDeployStepWaitTimeMs().or(configuration.getDefaultDeployStepWaitTimeMs()),
          false,
          deploy.getAutoAdvanceDeploySteps().or(true),
          Collections.emptySet(),
          System.currentTimeMillis()));
    }

    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING, deployProgress, updatedValidatedRequest);

    boolean deployToUnpause = false;
    if (requestWithState.getState() == RequestState.PAUSED) {
      deployToUnpause = true;
      requestManager.deployToUnpause(request, now, deployUser, deployRequest.getMessage());
    }

    boolean deployAlreadyInProgress = deployManager.createPendingDeploy(pendingDeployObj) == SingularityCreateResult.EXISTED;
    if (deployAlreadyInProgress && deployToUnpause) {
      requestManager.pause(request, now, deployUser, Optional.absent());
    }

    checkConflict(!deployAlreadyInProgress,
        "Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)", requestId, deployManager.getPendingDeploy(requestId).orNull());

    deployManager.saveDeploy(request, deployMarker, deploy);

    if (request.isDeployable()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deployMarker.getDeployId(), now, deployUser, PendingType.NEW_DEPLOY,
          deployRequest.getDeploy().getSkipHealthchecksOnDeploy(), deployRequest.getMessage()));
    }

    return fillEntireRequest(requestWithState, Optional.of(request));
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  @ApiOperation(value="Cancel a pending deployment (best effort - the deploy may still succeed or fail)", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Deploy is not in the pending state pending or is not not present"),
  })
  public SingularityRequestParent cancelDeploy(
      @Auth SingularityUser user,
      @ApiParam(required=true, value="The Singularity Request Id from which the deployment is removed.") @PathParam("requestId") String requestId,
      @ApiParam(required=true, value="The Singularity Deploy Id that should be removed.") @PathParam("deployId") String deployId) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);
    validator.checkActionEnabled(SingularityAction.CANCEL_DEPLOY);

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());

    checkBadRequest(deployState.isPresent() && deployState.get().getPendingDeploy().isPresent() && deployState.get().getPendingDeploy().get().getDeployId().equals(deployId),
      "Request %s does not have a pending deploy %s", requestId, deployId);

    deployManager.createCancelDeployRequest(new SingularityDeployMarker(requestId, deployId, System.currentTimeMillis(), user.getEmail(), Optional.<String> absent()));

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Path("/update")
  @ApiOperation(value="Update the target active instance count for a pending deploy", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Deploy is not in the pending state pending or is not not present")
  })
  public SingularityRequestParent updatePendingDeploy(@Auth SingularityUser user,
                                                      @ApiParam(required=true) SingularityUpdatePendingDeployRequest updateRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(updateRequest.getRequestId(), user);

    authorizationHelper.checkForAuthorization(requestWithState.getRequest(), user, SingularityAuthorizationScope.WRITE);

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());

    checkBadRequest(deployState.isPresent() && deployState.get().getPendingDeploy().isPresent() && deployState.get().getPendingDeploy().get().getDeployId().equals(updateRequest.getDeployId()),
      "Request %s does not have a pending deploy %s", updateRequest.getRequestId(), updateRequest.getDeployId());

    checkBadRequest(updateRequest.getTargetActiveInstances() > 0 && updateRequest.getTargetActiveInstances() <= requestWithState.getRequest().getInstancesSafe(),
      "Cannot update pending deploy to have more instances (%s) than instances set for request (%s), or less than 1 instance", updateRequest.getTargetActiveInstances(), requestWithState.getRequest().getInstancesSafe());

    deployManager.createUpdatePendingDeployRequest(updateRequest);

    return fillEntireRequest(requestWithState);
  }
}
