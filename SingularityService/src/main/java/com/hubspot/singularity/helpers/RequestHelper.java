package com.hubspot.singularity.helpers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class RequestHelper {

  private final RequestManager requestManager;
  private final SingularityMailer mailer;
  private final DeployManager deployManager;

  @Inject
  public RequestHelper(RequestManager requestManager, SingularityMailer mailer, DeployManager deployManager) {
    this.requestManager = requestManager;
    this.mailer = mailer;
    this.deployManager = deployManager;
  }

  public long unpause(SingularityRequest request, Optional<String> user) {
    mailer.sendRequestUnpausedMail(request, user);

    Optional<String> maybeDeployId = deployManager.getInUseDeployId(request.getId());

    final long now = System.currentTimeMillis();

    requestManager.unpause(request, now, user);

    if (maybeDeployId.isPresent() && !request.isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), maybeDeployId.get(), now, user, PendingType.UNPAUSED, Optional.<Boolean> absent()));
    }

    return now;
  }

}
