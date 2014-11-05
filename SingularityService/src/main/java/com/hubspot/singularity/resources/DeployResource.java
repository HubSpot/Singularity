package com.hubspot.singularity.resources;

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
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(DeployResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity deploys for existing requests.", value=DeployResource.PATH)
public class DeployResource extends AbstractRequestResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/deploys";

  private final DeployManager deployManager;
  private final RequestManager requestManager;
  private final SingularityValidator validator;

  @Inject
  public DeployResource(RequestManager requestManager, DeployManager deployManager, SingularityValidator validator) {
    super(requestManager, deployManager);

    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.validator = validator;
  }

  @GET
  @PropertyFiltering
  @Path("/pending")
  @ApiOperation("Retrieve the list of pending deploys.")
  public List<SingularityPendingDeploy> getPendingDeploys() {
    return deployManager.getPendingDeploys();
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @ApiOperation("Create a new deployment.")
  @ApiResponses({
    @ApiResponse(code=200, message="Deploy successfully scheduled."),
    @ApiResponse(code=400, message="Deploy object is invalid.", response=Void.class),
    @ApiResponse(code=409, message="A current deploy is in progress.", response=Void.class),
  })
  public SingularityRequestParent deploy(
      @ApiParam(required=true) SingularityDeployRequest deployRequest) {
    if (deployRequest.getDeploy() == null || deployRequest.getDeploy().getRequestId() == null) {
      throw WebExceptions.badRequest("DeployRequest must have a deploy object with a non-null requestId");
    }

    final String requestId = deployRequest.getDeploy().getRequestId();

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    SingularityRequest request = requestWithState.getRequest();

    if (!deployRequest.getUnpauseOnSuccessfulDeploy().isPresent() || !deployRequest.getUnpauseOnSuccessfulDeploy().get().booleanValue()) {
      checkRequestStateNotPaused(requestWithState, "deploy");
    }

    validator.checkDeploy(request, deployRequest.getDeploy());

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(requestId, deployRequest.getDeploy().getId(), System.currentTimeMillis(), deployRequest.getUser());
    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING);

    if (deployManager.createPendingDeploy(pendingDeployObj) == SingularityCreateResult.EXISTED) {
      throw WebExceptions.conflict("Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)", requestId, deployManager.getPendingDeploy(requestId).orNull());
    }

    deployManager.saveDeploy(request, deployMarker, deployRequest.getDeploy());

    if (requestWithState.getState() == RequestState.PAUSED) {
      requestManager.deployToUnpause(request, deployRequest.getUser());
    }

    if (request.isDeployable()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deployMarker.getDeployId(), System.currentTimeMillis(), Optional.<String> absent(), deployRequest.getUser(), PendingType.NEW_DEPLOY));
    }

    return fillEntireRequest(requestWithState);
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  @ApiOperation(value="Delete a pending deployment from a request.")
  @ApiResponses({
    @ApiResponse(code=400, message="Deploy is not pending or not present.", response=Void.class),
    @ApiResponse(code=200, message="Pending deploy successfully removed.")
  })
  public SingularityRequestParent cancelDeploy(
      @ApiParam(required=true,  value="The Singularity Request Id from which the deployment is removed.") @PathParam("requestId") String requestId,
      @ApiParam(required=true,  value="The Singularity Deploy Id that should be removed.") @PathParam("deployId") String deployId,
      @ApiParam(required=false, value="The user which executes the delete request.") @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());

    if (!deployState.isPresent() || !deployState.get().getPendingDeploy().isPresent() || !deployState.get().getPendingDeploy().get().getDeployId().equals(deployId)) {
      throw WebExceptions.badRequest("Request %s does not have a pending deploy %s", requestId, deployId);
    }

    deployManager.createCancelDeployRequest(new SingularityDeployMarker(requestId, deployId, System.currentTimeMillis(), user));

    return fillEntireRequest(requestWithState);
  }
}
