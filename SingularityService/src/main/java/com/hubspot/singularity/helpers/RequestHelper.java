package com.hubspot.singularity.helpers;

import java.util.UUID;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestCleanupType;
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.smtp.SingularityMailer;

@Singleton
public class RequestHelper {

  private final RequestManager requestManager;
  private final SingularityMailer mailer;
  private final DeployManager deployManager;
  private final SingularityValidator validator;

  @Inject
  public RequestHelper(RequestManager requestManager, SingularityMailer mailer, DeployManager deployManager, SingularityValidator validator) {
    this.requestManager = requestManager;
    this.mailer = mailer;
    this.deployManager = deployManager;
    this.validator = validator;
  }

  public long unpause(SingularityRequest request, Optional<String> user, Optional<String> message, Optional<Boolean> skipHealthchecks) {
    mailer.sendRequestUnpausedMail(request, user, message);

    Optional<String> maybeDeployId = deployManager.getInUseDeployId(request.getId());

    final long now = System.currentTimeMillis();

    requestManager.unpause(request, now, user, message);

    if (maybeDeployId.isPresent() && !request.isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(request.getId(), maybeDeployId.get(), now, user, PendingType.UNPAUSED,
          skipHealthchecks, message));
    }

    return now;
  }

  private SingularityRequestDeployHolder getDeployHolder(String requestId) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestId);

    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();

    if (requestDeployState.isPresent()) {
      if (requestDeployState.get().getActiveDeploy().isPresent()) {
        activeDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getActiveDeploy().get().getDeployId());
      }
      if (requestDeployState.get().getPendingDeploy().isPresent()) {
        pendingDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getPendingDeploy().get().getDeployId());
      }
    }

    return new SingularityRequestDeployHolder(activeDeploy, pendingDeploy);
  }

  private boolean shouldReschedule(SingularityRequest newRequest, SingularityRequest oldRequest) {
    if (newRequest.getInstancesSafe() != oldRequest.getInstancesSafe()) {
      return true;
    }
    if (newRequest.isScheduled() && oldRequest.isScheduled()) {
      if (!newRequest.getQuartzScheduleSafe().equals(oldRequest.getQuartzScheduleSafe())) {
        return true;
      }
    }

    return false;
  }

  private void checkReschedule(SingularityRequest newRequest, Optional<SingularityRequest> maybeOldRequest, Optional<String> user, long timestamp, Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<SingularityBounceRequest> maybeBounceRequest) {
    if (!maybeOldRequest.isPresent()) {
      return;
    }

    if (shouldReschedule(newRequest, maybeOldRequest.get())) {
      Optional<String> maybeDeployId = deployManager.getInUseDeployId(newRequest.getId());

      if (maybeDeployId.isPresent()) {
        if (maybeBounceRequest.isPresent()) {
          Optional<String> actionId = maybeBounceRequest.get().getActionId().or(Optional.of(UUID.randomUUID().toString()));
          Optional<Boolean> removeFromLoadBalancer = Optional.absent();
          SingularityCreateResult createResult = requestManager.createCleanupRequest(
            new SingularityRequestCleanup(user, maybeBounceRequest.get().getIncremental().or(true) ? RequestCleanupType.INCREMENTAL_BOUNCE : RequestCleanupType.BOUNCE,
              System.currentTimeMillis(), Optional.<Boolean> absent(), removeFromLoadBalancer, newRequest.getId(), Optional.of(maybeDeployId.get()), skipHealthchecks, message, actionId, maybeBounceRequest.get().getRunShellCommandBeforeKill()));

          if (createResult != SingularityCreateResult.EXISTED) {
            requestManager.bounce(newRequest, System.currentTimeMillis(), user, Optional.of("Bouncing due to bounce after scale"));
            final SingularityBounceRequest validatedBounceRequest = validator.checkBounceRequest(maybeBounceRequest.get());
            requestManager.saveExpiringObject(new SingularityExpiringBounce(newRequest.getId(), maybeDeployId.get(), user, System.currentTimeMillis(), validatedBounceRequest, actionId.get()));
          } else {
            requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), timestamp, user, PendingType.UPDATED_REQUEST,
              skipHealthchecks, message));
          }
        } else {
          requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), timestamp, user, PendingType.UPDATED_REQUEST,
            skipHealthchecks, message));
        }
      }
    }
  }

  public void updateRequest(SingularityRequest request, Optional<SingularityRequest> maybeOldRequest, RequestState requestState, Optional<RequestHistoryType> historyType,
      Optional<String> user, Optional<Boolean> skipHealthchecks, Optional<String> message, Optional<SingularityBounceRequest> maybeBounceRequest) {
    SingularityRequestDeployHolder deployHolder = getDeployHolder(request.getId());

    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy());

    final long now = System.currentTimeMillis();

    if (requestState == RequestState.FINISHED && maybeOldRequest.isPresent() && shouldReschedule(newRequest, maybeOldRequest.get())) {
      requestState = RequestState.ACTIVE;
    }

    RequestHistoryType historyTypeToSet = null;

    if (historyType.isPresent()) {
      historyTypeToSet = historyType.get();
    } else if (maybeOldRequest.isPresent()) {
      historyTypeToSet = RequestHistoryType.UPDATED;
    } else {
      historyTypeToSet = RequestHistoryType.CREATED;
    }

    requestManager.save(newRequest, requestState, historyTypeToSet, now, user, message);

    checkReschedule(newRequest, maybeOldRequest, user, now, skipHealthchecks, message, maybeBounceRequest);
  }

}
