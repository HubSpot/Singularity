package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;

public class AbstractRequestResource {

  protected final RequestManager requestManager;
  protected final DeployManager deployManager;
  protected final Optional<SingularityUser> user;
  protected final SingularityValidator validator;
  protected final SingularityAuthorizationHelper authorizationHelper;

  public AbstractRequestResource(RequestManager requestManager, DeployManager deployManager, Optional<SingularityUser> user, SingularityValidator validator, SingularityAuthorizationHelper authorizationHelper) {
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.user = user;
    this.validator = validator;
    this.authorizationHelper = authorizationHelper;
  }

  protected SingularityRequestWithState fetchRequestWithState(String requestId) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(requestId);

    checkNotFound(request.isPresent(), "Couldn't find request with id %s", requestId);

    authorizationHelper.checkForAuthorization(request.get().getRequest(), user, SingularityAuthorizationScope.READ);

    return request.get();
  }

  protected SingularityRequestParent fillEntireRequest(SingularityRequestWithState requestWithState) {
    final String requestId = requestWithState.getRequest().getId();

    final Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestId);

    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();

    if (requestDeployState.isPresent()) {
      activeDeploy = fillDeploy(requestDeployState.get().getActiveDeploy());
      pendingDeploy = fillDeploy(requestDeployState.get().getPendingDeploy());
    }

    Optional<SingularityPendingDeploy> pendingDeployState = deployManager.getPendingDeploy(requestId);

    return new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), requestDeployState, activeDeploy, pendingDeploy, pendingDeployState,
        requestManager.getExpiringBounce(requestId), requestManager.getExpiringPause(requestId), requestManager.getExpiringScale(requestId),
        requestManager.getExpiringSkipHealthchecks(requestId));
  }

  protected Optional<SingularityDeploy> fillDeploy(Optional<SingularityDeployMarker> deployMarker) {
    if (!deployMarker.isPresent()) {
      return Optional.absent();
    }

    return deployManager.getDeploy(deployMarker.get().getRequestId(), deployMarker.get().getDeployId());
  }

}
