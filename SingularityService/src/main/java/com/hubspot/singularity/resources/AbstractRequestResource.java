package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.sun.jersey.api.NotFoundException;

public class AbstractRequestResource {

  private final RequestManager requestManager;
  private final DeployManager deployManager;

  public AbstractRequestResource(RequestManager requestManager, DeployManager deployManager) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
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

    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();

    if (requestDeployState.isPresent()) {
      activeDeploy = fillDeploy(requestDeployState.get().getActiveDeploy());
      pendingDeploy = fillDeploy(requestDeployState.get().getPendingDeploy());
    }

    Optional<SingularityPendingDeploy> pendingDeployState = deployManager.getPendingDeploy(requestWithState.getRequest().getId());

    return new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), requestDeployState, activeDeploy, pendingDeploy, pendingDeployState);
  }

  protected Optional<SingularityDeploy> fillDeploy(Optional<SingularityDeployMarker> deployMarker) {
    if (!deployMarker.isPresent()) {
      return Optional.absent();
    }

    return deployManager.getDeploy(deployMarker.get().getRequestId(), deployMarker.get().getDeployId());
  }

}
