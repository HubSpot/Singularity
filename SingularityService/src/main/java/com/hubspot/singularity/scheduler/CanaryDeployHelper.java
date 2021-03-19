package com.hubspot.singularity.scheduler;

import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.data.TaskManager;
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

  public static boolean canMoveToNextStep(SingularityDeployProgress deployProgress) {
    return (
      deployProgress.isAutoAdvanceDeploySteps() &&
      deployProgress.getTimestamp() +
      deployProgress.getDeployStepWaitTimeMs() <
      System.currentTimeMillis()
    );
  }

  public static int getNewTargetInstances(
    SingularityDeployProgress deployProgress,
    SingularityRequest request,
    Optional<SingularityUpdatePendingDeployRequest> updateRequest
  ) {
    return updateRequest
      .map(
        singularityUpdatePendingDeployRequest ->
          Math.min(
            singularityUpdatePendingDeployRequest.getTargetActiveInstances(),
            request.getInstancesSafe()
          )
      )
      .orElseGet(
        () ->
          Math.min(
            deployProgress.getTargetActiveInstances() +
            deployProgress.getDeployInstanceCountPerStep(),
            request.getInstancesSafe()
          )
      );
  }

  public static boolean canRetryTasks(
    Optional<SingularityDeploy> deploy,
    Collection<SingularityTaskId> inactiveDeployMatchingTasks,
    TaskManager taskManager
  ) {
    int maxRetries = getMaxRetries(deploy);
    long matchingInactiveTasks = inactiveDeployMatchingTasks
      .stream()
      .filter(
        t -> {
          // TODO - only tasks for most recent group somehow?
          // All TASK_LOSTs that are not resource limit related should be able to be retried
          for (SingularityTaskHistoryUpdate historyUpdate : taskManager.getTaskHistoryUpdates(
            t
          )) {
            if (
              historyUpdate.getTaskState() == ExtendedTaskState.TASK_LOST &&
              !historyUpdate.getStatusReason().orElse("").startsWith("REASON_CONTAINER")
            ) {
              return false;
            }
          }
          return true;
        }
      )
      .count();
    return maxRetries > 0 && matchingInactiveTasks <= maxRetries;
  }

  public static int getMaxRetries(Optional<SingularityDeploy> deploy) {
    return deploy
      .map(d -> d.getCanaryDeploySettings().getAllowedTasksFailuresPerGroup())
      .orElse(0);
  }
}
