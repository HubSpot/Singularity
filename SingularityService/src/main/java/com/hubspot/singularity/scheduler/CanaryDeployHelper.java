package com.hubspot.singularity.scheduler;

import com.hubspot.singularity.CanaryDeploySettings;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class CanaryDeployHelper {

  public static List<SingularityTaskId> tasksToShutDown(
    SingularityDeployProgress deployProgress,
    Collection<SingularityTaskId> otherActiveTasks,
    SingularityRequest request
  ) {
    int numTasksToShutDown = Math.max(
      otherActiveTasks.size() -
      (request.getInstancesSafe() - deployProgress.getTargetActiveInstances()),
      0
    );
    List<SingularityTaskId> sortedOtherTasks = new ArrayList<>(otherActiveTasks);
    sortedOtherTasks.sort(SingularityTaskId.INSTANCE_NO_COMPARATOR);
    return sortedOtherTasks.isEmpty()
      ? sortedOtherTasks
      : sortedOtherTasks.subList(
        0,
        Math.min(numTasksToShutDown, sortedOtherTasks.size())
      );
  }

  public static int getNewTargetInstances(
    SingularityDeployProgress deployProgress,
    SingularityRequest request,
    Optional<SingularityUpdatePendingDeployRequest> updateRequest,
    CanaryDeploySettings canaryDeploySettings
  ) {
    return updateRequest
      .map(singularityUpdatePendingDeployRequest ->
        Math.min(
          singularityUpdatePendingDeployRequest.getTargetActiveInstances(),
          request.getInstancesSafe()
        )
      )
      .orElseGet(() -> {
        int cycleCount =
          deployProgress.getTargetActiveInstances() /
          canaryDeploySettings.getInstanceGroupSize();
        if (cycleCount >= canaryDeploySettings.getCanaryCycleCount()) {
          // We have run enough canary cycles, launch everything remaining
          return request.getInstancesSafe();
        } else {
          return Math.min(
            deployProgress.getTargetActiveInstances() +
            canaryDeploySettings.getInstanceGroupSize(),
            request.getInstancesSafe()
          );
        }
      });
  }

  public static boolean canRetryTasks(
    SingularityDeploy deploy,
    Collection<SingularityTaskId> inactiveDeployMatchingTasksForInstanceGroup
  ) {
    int maxFailures = deploy.getCanaryDeploySettings().getAllowedTasksFailuresPerGroup();
    return (
      maxFailures > 0 && inactiveDeployMatchingTasksForInstanceGroup.size() <= maxFailures
    );
  }
}
