package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;

public class AbstractRequestResource {

  private final RequestManager requestManager;
  private final DeployManager deployManager;

  public AbstractRequestResource(RequestManager requestManager, DeployManager deployManager) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
  }

  protected SingularityRequestWithState fetchRequestWithState(String requestId) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(requestId);

    checkNotFound(request.isPresent(), "Couldn't find request with id %s", requestId);

    return request.get();
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
