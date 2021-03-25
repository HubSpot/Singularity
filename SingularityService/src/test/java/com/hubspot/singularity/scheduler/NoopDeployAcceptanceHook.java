package com.hubspot.singularity.scheduler;

import com.google.inject.Singleton;
import com.hubspot.singularity.DeployAcceptanceResult;
import com.hubspot.singularity.DeployAcceptanceState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.hooks.DeployAcceptanceHook;
import java.util.Collection;

@Singleton
public class NoopDeployAcceptanceHook implements DeployAcceptanceHook {
  private DeployAcceptanceResult nextResult = new DeployAcceptanceResult(
    DeployAcceptanceState.SUCCEEDED,
    "no-op"
  );

  @Override
  public String getName() {
    return "no-op";
  }

  @Override
  public DeployAcceptanceResult getAcceptanceResult(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> activeTasksForPendingDeploy,
    Collection<SingularityTaskId> inactiveTasksForPendingDeploy,
    Collection<SingularityTaskId> otherActiveTasksForRequest
  ) {
    return nextResult;
  }

  public void setNextResult(DeployAcceptanceResult nextResult) {
    this.nextResult = nextResult;
  }
}
