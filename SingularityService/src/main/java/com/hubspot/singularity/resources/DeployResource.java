package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;
import static com.hubspot.singularity.WebExceptions.checkNotNullBadRequest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployProgress;
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
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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

@Path(ApiPaths.DEPLOY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manages Singularity Deploys for existing requests")
@Tags({ @Tag(name = "Deploys") })
public class DeployResource extends AbstractRequestResource {
  private final SingularityConfiguration configuration;
  private final TaskManager taskManager;
  private final SingularitySchedulerLock schedulerLock;

  @Inject
  public DeployResource(
    RequestManager requestManager,
    DeployManager deployManager,
    SingularityValidator validator,
    SingularityAuthorizer authorizationHelper,
    SingularityConfiguration configuration,
    TaskManager taskManager,
    LeaderLatch leaderLatch,
    AsyncHttpClient httpClient,
    @Singularity ObjectMapper objectMapper,
    RequestHelper requestHelper,
    SingularitySchedulerLock schedulerLock
  ) {
    super(
      requestManager,
      deployManager,
      validator,
      authorizationHelper,
      httpClient,
      leaderLatch,
      objectMapper,
      requestHelper
    );
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.schedulerLock = schedulerLock;
  }

  @GET
  @PropertyFiltering
  @Path("/pending")
  @Operation(description = "Retrieve the list of current pending deploys")
  public List<SingularityPendingDeploy> getPendingDeploys(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return authorizationHelper.filterByAuthorizedRequests(
      user,
      deployManager.getPendingDeploys(),
      SingularityTransformHelpers.PENDING_DEPLOY_TO_REQUEST_ID::apply,
      SingularityAuthorizationScope.READ
    );
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
    summary = "Start a new deployment for a Request",
    responses = {
      @ApiResponse(responseCode = "400", description = "Deploy object is invalid"),
      @ApiResponse(
        responseCode = "409",
        description = "Deploys are disabled or a current deploy is in progress. It may be canceled by calling DELETE"
      )
    }
  )
  public SingularityRequestParent deploy(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Context HttpServletRequest requestContext,
    @RequestBody(
      required = true,
      description = "Deploy data"
    ) SingularityDeployRequest deployRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      SingularityRequestParent.class,
      deployRequest,
      () -> deploy(deployRequest, user)
    );
  }

  public SingularityRequestParent deploy(
    SingularityDeployRequest deployRequest,
    SingularityUser user
  ) {
    validator.checkActionEnabled(SingularityAction.DEPLOY);
    SingularityDeploy deploy = deployRequest.getDeploy();
    checkNotNullBadRequest(deploy, "DeployRequest must have a deploy object");

    final Optional<String> deployUser = user.getEmail();
    final String requestId = checkNotNullBadRequest(
      deploy.getRequestId(),
      "DeployRequest must have a non-null requestId"
    );

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(
      requestWithState.getRequest(),
      user,
      SingularityAuthorizationScope.WRITE
    );

    SingularityRequest request = requestWithState.getRequest();
    final Optional<SingularityRequest> updatedValidatedRequest;
    if (deployRequest.getUpdatedRequest().isPresent()) {
      authorizationHelper.checkForAuthorizedChanges(
        deployRequest.getUpdatedRequest().get(),
        requestWithState.getRequest(),
        user
      );
      updatedValidatedRequest =
        Optional.of(
          validator.checkSingularityRequest(
            deployRequest.getUpdatedRequest().get(),
            Optional.of(requestWithState.getRequest()),
            Optional.<SingularityDeploy>empty(),
            Optional.of(deploy)
          )
        );
    } else {
      updatedValidatedRequest = Optional.empty();
    }

    if (updatedValidatedRequest.isPresent()) {
      request = updatedValidatedRequest.get();
    }

    validator.checkScale(
      request,
      Optional.of(taskManager.getActiveTaskIdsForRequest(request.getId()).size())
    );

    if (
      !deployRequest.isUnpauseOnSuccessfulDeploy() &&
      !configuration.isAllowDeployOfPausedRequests()
    ) {
      checkConflict(
        requestWithState.getState() != RequestState.PAUSED,
        "Request %s is paused. Unable to deploy (it must be manually unpaused first)",
        requestWithState.getRequest().getId()
      );
    }

    deploy = validator.checkDeploy(request, deploy);

    final long now = System.currentTimeMillis();

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(
      requestId,
      deploy.getId(),
      now,
      deployUser,
      deployRequest.getMessage()
    );

    SingularityDeployProgress deployProgress;
    if (request.isLongRunning()) {
      int firstTargetInstances = deploy.getCanaryDeploySettings().isEnableCanaryDeploy()
        ? Math.min(
          deploy.getCanaryDeploySettings().getInstanceGroupSize(),
          request.getInstancesSafe()
        )
        : request.getInstancesSafe();
      deployProgress =
        SingularityDeployProgress.forNewDeploy(
          firstTargetInstances,
          deploy.getCanaryDeploySettings().isEnableCanaryDeploy()
        );
    } else {
      deployProgress = SingularityDeployProgress.forNonLongRunning();
    }

    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(
      deployMarker,
      DeployState.WAITING,
      deployProgress,
      updatedValidatedRequest
    );

    boolean deployToUnpause = false;
    if (
      requestWithState.getState() == RequestState.PAUSED &&
      deployRequest.isUnpauseOnSuccessfulDeploy()
    ) {
      deployToUnpause = true;
      requestManager.deployToUnpause(
        request,
        now,
        deployUser,
        deployRequest.getMessage()
      );
    }

    AtomicBoolean deployAlreadyInProgress = new AtomicBoolean(
      deployManager.pendingDeployInProgress(requestId)
    );
    // Short circuit outside lock so we don't wait too long
    if (!deployAlreadyInProgress.get()) {
      SingularityRequest updatedRequest = request;
      SingularityDeploy validatedDeploy = deploy;
      // This can cause a conflict if run outside the lock, causing the pending deploy to be checked before deploy data is saved
      schedulerLock.runWithRequestLock(
        () -> {
          deployManager.createDeployIfNotExists(
            updatedRequest,
            deployMarker,
            validatedDeploy
          );
          deployAlreadyInProgress.set(
            deployManager.createPendingDeploy(pendingDeployObj) ==
            SingularityCreateResult.EXISTED
          );
          if (deployAlreadyInProgress.get()) {
            return;
          }
          deployManager.saveDeploy(updatedRequest, deployMarker, validatedDeploy);
        },
        requestId,
        "submitNewDeploy"
      );
    }

    if (deployAlreadyInProgress.get() && deployToUnpause) {
      requestManager.pause(request, now, deployUser, Optional.empty());
    }

    checkConflict(
      !deployAlreadyInProgress.get(),
      "Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)",
      requestId,
      deployManager.getPendingDeploy(requestId).orElse(null)
    );

    deployManager.saveDeploy(request, deployMarker, deploy);

    if (
      request.isDeployable() &&
      !(
        requestWithState.getState() == RequestState.PAUSED &&
        configuration.isAllowDeployOfPausedRequests()
      )
    ) {
      requestManager.addToPendingQueue(
        new SingularityPendingRequest(
          requestId,
          deployMarker.getDeployId(),
          now,
          deployUser,
          PendingType.NEW_DEPLOY,
          deployRequest.getDeploy().getSkipHealthchecksOnDeploy(),
          deployRequest.getMessage()
        )
      );
    }

    return fillEntireRequest(requestWithState, Optional.of(request));
  }

  @DELETE
  @Path("/deploy/{deployId}/request/{requestId}")
  @Operation(
    summary = "Cancel a pending deployment (best effort - the deploy may still succeed or fail)",
    responses = {
      @ApiResponse(
        responseCode = "400",
        description = "Deploy is not in the pending state pending or is not not present"
      )
    }
  )
  public SingularityRequestParent cancelDeploy(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Context HttpServletRequest requestContext,
    @Parameter(
      required = true,
      description = "The Singularity Request Id from which the deployment is removed."
    ) @PathParam("requestId") String requestId,
    @Parameter(
      required = true,
      description = "The Singularity Deploy Id that should be removed."
    ) @PathParam("deployId") String deployId
  ) {
    return maybeProxyToLeader(
      requestContext,
      SingularityRequestParent.class,
      null,
      () -> cancelDeploy(user, requestId, deployId)
    );
  }

  public SingularityRequestParent cancelDeploy(
    SingularityUser user,
    String requestId,
    String deployId
  ) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId, user);

    authorizationHelper.checkForAuthorization(
      requestWithState.getRequest(),
      user,
      SingularityAuthorizationScope.WRITE
    );
    validator.checkActionEnabled(SingularityAction.CANCEL_DEPLOY);

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(
      requestWithState.getRequest().getId()
    );

    checkBadRequest(
      deployState.isPresent() &&
      deployState.get().getPendingDeploy().isPresent() &&
      deployState.get().getPendingDeploy().get().getDeployId().equals(deployId),
      "Request %s does not have a pending deploy %s",
      requestId,
      deployId
    );

    deployManager.createCancelDeployRequest(
      new SingularityDeployMarker(
        requestId,
        deployId,
        System.currentTimeMillis(),
        user.getEmail(),
        Optional.<String>empty()
      )
    );

    return fillEntireRequest(requestWithState);
  }

  @POST
  @Path("/update")
  @Operation(
    summary = "Update the target active instance count for a pending deploy",
    responses = {
      @ApiResponse(
        responseCode = "400",
        description = "Deploy is not in the pending state pending or is not not present"
      )
    }
  )
  public SingularityRequestParent updatePendingDeploy(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Context HttpServletRequest requestContext,
    @RequestBody(required = true) SingularityUpdatePendingDeployRequest updateRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      SingularityRequestParent.class,
      updateRequest,
      () -> updatePendingDeploy(user, updateRequest)
    );
  }

  public SingularityRequestParent updatePendingDeploy(
    SingularityUser user,
    SingularityUpdatePendingDeployRequest updateRequest
  ) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(
      updateRequest.getRequestId(),
      user
    );

    authorizationHelper.checkForAuthorization(
      requestWithState.getRequest(),
      user,
      SingularityAuthorizationScope.WRITE
    );

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(
      requestWithState.getRequest().getId()
    );

    checkBadRequest(
      deployState.isPresent() &&
      deployState.get().getPendingDeploy().isPresent() &&
      deployState
        .get()
        .getPendingDeploy()
        .get()
        .getDeployId()
        .equals(updateRequest.getDeployId()),
      "Request %s does not have a pending deploy %s",
      updateRequest.getRequestId(),
      updateRequest.getDeployId()
    );

    checkBadRequest(
      updateRequest.getTargetActiveInstances() > 0 &&
      updateRequest.getTargetActiveInstances() <=
      requestWithState.getRequest().getInstancesSafe(),
      "Cannot update pending deploy to have more instances (%s) than instances set for request (%s), or less than 1 instance",
      updateRequest.getTargetActiveInstances(),
      requestWithState.getRequest().getInstancesSafe()
    );

    deployManager.createUpdatePendingDeployRequest(updateRequest);

    return fillEntireRequest(requestWithState);
  }
}
