package com.hubspot.singularity.hooks;

import com.hubspot.singularity.DeployAcceptanceResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import java.util.Collection;

public interface DeployAcceptanceHook {
  // Unique per hook. Used to track results in SingularityDeployChecker
  String getName();

  /*
   * If `true` any uncaught exception will cause a DeployAcceptanceState of FAILED.
   * If `false` an uncaught exception will be ignored and a DeployAcceptanceState of SUCCESS used. Useful for testing
   */
  default boolean isFailOnUncaughtException() {
    return true;
  }

  /*
   * The `canarySettings` object will change the state/time during which `getAcceptanceResult` is called:
   *  - If `enableCanaryDeploy` is set to `false`, the state of tasks will be:
   *    - All new tasks in `activeTasksForPendingDeploy` are launched and health checked
   *    - If the deploy is load balanced, tasks in `otherActiveTasksForRequest` are no longer in the load balancer. Only the new deploy tasks in `activeTasksForPendingDeploy` are active in the load balancer
   *      - *Note* - Singularity will re-add the old tasks back to the load balancer if deploy acceptance checks fail
   *  - If `enableCanaryDeploy` is set to `true`, `getAcceptanceResult` is called after each deploy step
   *    - `activeTasksForPendingDeploy` contains _all_ active tasks launched so far, not just those for the current canary step. These tasks are in running state and have passed initial health checks
   *    - If load balanced, all tasks in `activeTasksForPendingDeploy` as well as all in `otherActiveTasksForRequest` are active in the load balancer at once
   */
  DeployAcceptanceResult getAcceptanceResult(
    SingularityRequest request,
    SingularityDeploy deploy,
    SingularityPendingDeploy pendingDeploy,
    Collection<SingularityTaskId> activeTasksForPendingDeploy,
    Collection<SingularityTaskId> inactiveTasksForPendingDeploy,
    Collection<SingularityTaskId> otherActiveTasksForRequest
  );
}
