package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTransformHelpers;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(DeployResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity Deploys for existing requests", value=DeployResource.PATH, position=2)
public class DeployResource extends AbstractRequestResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/deploys";

  private final SingularityValidator validator;
  private final SingularityAuthorizationHelper authHelper;

  @Inject
  public DeployResource(RequestManager requestManager, DeployManager deployManager, SingularityValidator validator, SingularityAuthorizationHelper authHelper, Optional<SingularityUser> user) {
    super(requestManager, deployManager, user);

    this.validator = validator;
    this.authHelper = authHelper;
  }

  @GET
  @PropertyFiltering
  @Path("/pending")
  @ApiOperation(response=SingularityPendingDeploy.class, responseContainer="List", value="Retrieve the list of current pending deploys")
  public List<SingularityPendingDeploy> getPendingDeploys() {
    return authHelper.filterByAuthorizedRequests(user, deployManager.getPendingDeploys(), SingularityTransformHelpers.PENDING_DEPLOY_TO_REQUEST_ID);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation(value="Start a new deployment for a Request", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Deploy object is invalid"),
    @ApiResponse(code=409, message="A current deploy is in progress. It may be canceled by calling DELETE"),
  })
  public SingularityRequestParent deploy(@ApiParam(required=true) SingularityDeployRequest deployRequest) {
    final Optional<String> deployUser = deployRequest.getUser();

    SingularityDeploy deploy = deployRequest.getDeploy();
    checkNotNullBadRequest(deploy, "DeployRequest must have a deploy object");

    final String requestId = checkNotNullBadRequest(deploy.getRequestId(), "DeployRequest must have a non-null requestId");

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    SingularityRequest request = requestWithState.getRequest();

    validator.checkForAuthorization(requestWithState.getRequest(), Optional.<SingularityRequest>absent(), user);

    if (!deployRequest.isUnpauseOnSuccessfulDeploy()) {
      checkConflict(requestWithState.getState() != RequestState.PAUSED, "Request %s is paused. Unable to deploy (it must be manually unpaused first)", requestWithState.getRequest().getId());
    }

    deploy = validator.checkDeploy(request, deploy, user);

    final long now = System.currentTimeMillis();

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(requestId, deploy.getId(), now, deployUser);
    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING);

    checkConflict(deployManager.createPendingDeploy(pendingDeployObj) != SingularityCreateResult.EXISTED,
        "Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)", requestId, deployManager.getPendingDeploy(requestId).orNull());

    deployManager.saveDeploy(request, deployMarker, deploy);

    if (requestWithState.getState() == RequestState.PAUSED) {
      requestManager.deployToUnpause(request, now, deployUser);
    }

    if (request.isDeployable()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deployMarker.getDeployId(), now, deployUser, PendingType.NEW_DEPLOY, Collections.<String> emptyList()));
    }

    return fillEntireRequest(requestWithState);
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  @ApiOperation(value="Cancel a pending deployment (best effort - the deploy may still succeed or fail)", response=SingularityRequestParent.class)
  @ApiResponses({
    @ApiResponse(code=400, message="Deploy is not in the pending state pending or is not not present"),
  })
  public SingularityRequestParent cancelDeploy(
      @ApiParam(required=true,  value="The Singularity Request Id from which the deployment is removed.") @PathParam("requestId") String requestId,
      @ApiParam(required=true,  value="The Singularity Deploy Id that should be removed.") @PathParam("deployId") String deployId,
      @ApiParam(required=false, value="The user which executes the delete request.") @QueryParam("user") Optional<String> queryUser) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    validator.checkForAuthorization(requestWithState.getRequest(), Optional.<SingularityRequest>absent(), user);

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());

    if (!deployState.isPresent() || !deployState.get().getPendingDeploy().isPresent() || !deployState.get().getPendingDeploy().get().getDeployId().equals(deployId)) {
      throw badRequest("Request %s does not have a pending deploy %s", requestId, deployId);
    }

    deployManager.createCancelDeployRequest(new SingularityDeployMarker(requestId, deployId, System.currentTimeMillis(), queryUser));

    return fillEntireRequest(requestWithState);
  }
}
