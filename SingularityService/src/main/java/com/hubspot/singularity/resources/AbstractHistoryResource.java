package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;

public abstract class AbstractHistoryResource {
  
  private final HistoryManager historyManager;
  private final TaskManager taskManager;
  
  public AbstractHistoryResource(HistoryManager historyManager, TaskManager taskManager) {
    this.historyManager = historyManager;
    this.taskManager = taskManager;
  }
  
  protected SingularityTaskId getTaskIdObject(String taskId) {
    try {
      return SingularityTaskId.fromString(taskId);
    } catch (InvalidSingularityTaskIdException e) {
      throw WebExceptions.badRequest("%s is not a valid task id: %s", taskId, e.getMessage());
    }
  }
  
  protected SingularityTaskHistory getTaskHistory(SingularityTaskId taskId) {
    Optional<SingularityTaskHistory> history = taskManager.getTaskHistory(taskId);
    
    if (!history.isPresent()) {
      history = historyManager.getTaskHistory(taskId.getId());
    }
    
    if (!history.isPresent()) {
      throw WebExceptions.notFound("No history for task %s", taskId);
    }
  
    return history.get();
  }
  
}
