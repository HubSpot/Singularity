package com.hubspot.singularity.hooks;

import com.hubspot.singularity.DeployAcceptanceResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import java.util.Collection;

public interface DeployAcceptanceHook {
  String getName();

  default boolean isFailOnUncaughtException() {
    return true;
  }

  DeployAcceptanceResult getAcceptanceResult(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> activeTasksForPendingDeploy,
    Collection<SingularityTaskId> inactiveTasksForPendingDeploy,
    Collection<SingularityTaskId> otherActiveTasksForRequest
  );
}
