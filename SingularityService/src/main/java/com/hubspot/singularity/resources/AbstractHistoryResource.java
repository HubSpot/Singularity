package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;

public abstract class AbstractHistoryResource {

  protected final HistoryManager historyManager;
  protected final TaskManager taskManager;
  protected final DeployManager deployManager;
  protected final SingularityAuthorizationHelper authorizationHelper;

  public AbstractHistoryResource(HistoryManager historyManager, TaskManager taskManager, DeployManager deployManager, SingularityAuthorizationHelper authorizationHelper) {
    this.historyManager = historyManager;
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.authorizationHelper = authorizationHelper;
  }

  protected SingularityTaskId getTaskIdObject(String taskId) {
    try {
      return SingularityTaskId.valueOf(taskId);
    } catch (InvalidSingularityTaskIdException e) {
      throw badRequest("%s is not a valid task id: %s", taskId, e.getMessage());
    }
  }

  protected Optional<SingularityTaskHistory> getTaskHistory(SingularityTaskId taskId, SingularityUser user) {
    authorizationHelper.checkForAuthorizationByRequestId(taskId.getRequestId(), user, SingularityAuthorizationScope.READ);

    Optional<SingularityTaskHistory> history = taskManager.getTaskHistory(taskId);

    if (!history.isPresent()) {
      history = historyManager.getTaskHistory(taskId.getId());
    }

    return history;
  }

  protected SingularityTaskHistory getTaskHistoryRequired(SingularityTaskId taskId, SingularityUser user) {
    Optional<SingularityTaskHistory> history = getTaskHistory(taskId, user);

    checkNotFound(history.isPresent(), "No history for task %s", taskId);

    return history.get();
  }

  protected SingularityDeployHistory getDeployHistory(String requestId, String deployId, SingularityUser user) {
    authorizationHelper.checkForAuthorizationByRequestId(requestId, user, SingularityAuthorizationScope.READ);

    Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(requestId, deployId, true);

    if (deployHistory.isPresent()) {
      return deployHistory.get();
    }

    deployHistory = historyManager.getDeployHistory(requestId, deployId);

    checkNotFound(deployHistory.isPresent(), "Deploy history for request %s and deploy %s not found", requestId, deployId);

    return deployHistory.get();
  }

}
