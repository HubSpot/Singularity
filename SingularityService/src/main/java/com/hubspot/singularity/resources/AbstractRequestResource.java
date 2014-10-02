package com.hubspot.singularity.resources;

import java.util.Optional;

import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.sun.jersey.api.NotFoundException;

public class AbstractRequestResource {

  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final SingularityValidator validator;

  public AbstractRequestResource(RequestManager requestManager, DeployManager deployManager, SingularityValidator validator) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.validator = validator;
  }

  protected SingularityRequestWithState fetchRequestWithState(String requestId) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(requestId);

    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }

    return request.get();
  }

  protected NotFoundException handleNoMatchingRequest(String requestId) {
    throw WebExceptions.notFound("Couldn't find request with id %s", requestId);
  }

  protected void checkRequestStateNotPaused(SingularityRequestWithState requestWithState, String action) {
    if (requestWithState.getState() == RequestState.PAUSED) {
      throw WebExceptions.conflict("Request %s is paused. Unable to %s (it must be manually unpaused first)", requestWithState.getRequest().getId(), action);
    }
  }

  protected SingularityRequestParent fillEntireRequest(SingularityRequestWithState requestWithState) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());

    Optional<SingularityDeploy> activeDeploy = Optional.empty();
    Optional<SingularityDeploy> pendingDeploy = Optional.empty();

    if (requestDeployState.isPresent()) {
      activeDeploy = fillDeploy(requestDeployState.get().getActiveDeploy());
      pendingDeploy = fillDeploy(requestDeployState.get().getPendingDeploy());
    }

    Optional<SingularityPendingDeploy> pendingDeployState = deployManager.getPendingDeploy(requestWithState.getRequest().getId());

    return new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), requestDeployState, activeDeploy, pendingDeploy, pendingDeployState);
  }

  protected Optional<SingularityDeploy> fillDeploy(Optional<SingularityDeployMarker> deployMarker) {
    if (!deployMarker.isPresent()) {
      return Optional.empty();
    }

    return deployManager.getDeploy(deployMarker.get().getRequestId(), deployMarker.get().getDeployId());
  }

  protected SingularityRequestParent deploy(SingularityDeploy pendingDeploy, @QueryParam("user") Optional<String> user) {
    final String requestId = pendingDeploy.getRequestId();

    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    SingularityRequest request = requestWithState.getRequest();

    checkRequestStateNotPaused(requestWithState, "deploy");

    validator.checkDeploy(request, pendingDeploy);

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(requestId, pendingDeploy.getId(), System.currentTimeMillis(), user);
    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(deployMarker, Optional.empty(), DeployState.WAITING);

    if (deployManager.createPendingDeploy(pendingDeployObj) == SingularityCreateResult.EXISTED) {
      throw WebExceptions.conflict("Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)", requestId, deployManager.getPendingDeploy(requestId).orElse(null));
    }

    deployManager.saveDeploy(request, deployMarker, pendingDeploy);

    if (request.isDeployable()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deployMarker.getDeployId(), System.currentTimeMillis(), Optional.empty(), user, PendingType.NEW_DEPLOY));
    }

    return fillEntireRequest(requestWithState);
  }

  protected SingularityRequestParent cancelDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());

    if (!deployState.isPresent() || !deployState.get().getPendingDeploy().isPresent() || !deployState.get().getPendingDeploy().get().getDeployId().equals(deployId)) {
      throw WebExceptions.badRequest("Request %s does not have a pending deploy %s", requestId, deployId);
    }

    deployManager.createCancelDeployRequest(new SingularityDeployMarker(requestId, deployId, System.currentTimeMillis(), user));

    return fillEntireRequest(requestWithState);
  }


}
