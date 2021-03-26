package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.DeployAcceptanceResult;
import com.hubspot.singularity.DeployAcceptanceState;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.hooks.DeployAcceptanceHook;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityDeployAcceptanceManager {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityDeployAcceptanceManager.class
  );

  private final Set<DeployAcceptanceHook> acceptanceHooks;

  @Inject
  public SingularityDeployAcceptanceManager(Set<DeployAcceptanceHook> acceptanceHooks) {
    this.acceptanceHooks = acceptanceHooks;
  }

  public Map<String, DeployAcceptanceResult> getAcceptanceResults(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> activeTasksForPendingDeploy,
    Collection<SingularityTaskId> inactiveTasksForPendingDeploy,
    Collection<SingularityTaskId> otherActiveTasksForRequest
  ) {
    Map<String, DeployAcceptanceResult> results = new HashMap<>();
    Map<String, DeployAcceptanceState> existing = pendingDeploy
      .getDeployProgress()
      .getStepAcceptanceResults();
    for (DeployAcceptanceHook hook : acceptanceHooks) {
      if (
        !existing.containsKey(hook.getName()) ||
        existing.get(hook.getName()) == DeployAcceptanceState.PENDING
      ) {
        try {
          results.put(
            hook.getName(),
            hook.getAcceptanceResult(
              request,
              deploy,
              pendingDeploy,
              activeTasksForPendingDeploy,
              inactiveTasksForPendingDeploy,
              otherActiveTasksForRequest
            )
          );
        } catch (Exception e) {
          LOG.error("Uncaught exception running hook {}", hook.getName(), e);
          if (hook.isFailOnUncaughtException()) {
            results.put(
              hook.getName(),
              new DeployAcceptanceResult(DeployAcceptanceState.FAILED, e.getMessage())
            );
          } else {
            results.put(
              hook.getName(),
              new DeployAcceptanceResult(
                DeployAcceptanceState.SUCCEEDED,
                String.format("Ignored err: %s", e.getMessage())
              )
            );
          }
        }
      }
    }
    return results;
  }

  public static DeployState resultsToDeployState(
    Map<String, DeployAcceptanceState> results
  ) {
    if (results.isEmpty()) {
      return DeployState.SUCCEEDED;
    }
    if (results.values().stream().anyMatch(d -> d == DeployAcceptanceState.FAILED)) {
      return DeployState.FAILED;
    }
    if (results.values().stream().anyMatch(d -> d == DeployAcceptanceState.PENDING)) {
      return DeployState.WAITING;
    }
    return DeployState.SUCCEEDED;
  }
}
