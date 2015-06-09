package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;

public abstract class AbstractHistoryResource {

  protected final HistoryManager historyManager;
  protected final TaskManager taskManager;
  protected final DeployManager deployManager;
  protected final SingularityValidator validator;
  protected final Optional<SingularityUser> user;

  public AbstractHistoryResource(HistoryManager historyManager, TaskManager taskManager, DeployManager deployManager, SingularityValidator validator, Optional<SingularityUser> user) {
    this.historyManager = historyManager;
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.validator = validator;
    this.user = user;
  }

  protected SingularityTaskId getTaskIdObject(String taskId) {
    try {
      return SingularityTaskId.valueOf(taskId);
    } catch (InvalidSingularityTaskIdException e) {
      throw badRequest("%s is not a valid task id: %s", taskId, e.getMessage());
    }
  }

  protected SingularityTaskHistory getTaskHistory(SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> history = taskManager.getTaskHistory(taskId);

    if (!history.isPresent()) {
      history = historyManager.getTaskHistory(taskId.getId());
    }

    checkNotFound(history.isPresent(), "No history for task %s", taskId);

    return history.get();
  }

  protected SingularityDeployHistory getDeployHistory(String requestId, String deployId) {
    Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(requestId, deployId, true);

    if (deployHistory.isPresent()) {
      return deployHistory.get();
    }

    deployHistory = historyManager.getDeployHistory(requestId, deployId);

    checkNotFound(deployHistory.isPresent(), "Deploy history for request %s and deploy %s not found", requestId, deployId);

    return deployHistory.get();
  }

}
