package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = SingularityDeployFailure.class)
public abstract class AbstractSingularityDeployFailure {
  public static List<SingularityDeployFailure> lbUpdateFailed() {
    return Collections.singletonList(SingularityDeployFailure.builder()
        .setReason(SingularityDeployFailureReason.LOAD_BALANCER_UPDATE_FAILED)
        .build());
  }

  public static List<SingularityDeployFailure> failedToSave() {
    return Collections.singletonList(SingularityDeployFailure.builder()
        .setReason(SingularityDeployFailureReason.FAILED_TO_SAVE_DEPLOY_STATE)
        .build());
  }

  public static List<SingularityDeployFailure> deployRemoved() {
    return Collections.singletonList(SingularityDeployFailure.builder()
        .setReason(SingularityDeployFailureReason.PENDING_DEPLOY_REMOVED)
        .build());
  }

  public abstract SingularityDeployFailureReason getReason();

  public abstract Optional<SingularityTaskId> getTaskId();

  public abstract Optional<String> getMessage();
}
