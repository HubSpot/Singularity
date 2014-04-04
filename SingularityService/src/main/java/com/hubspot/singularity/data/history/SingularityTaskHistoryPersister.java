package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.data.TaskManager;

public class SingularityTaskHistoryPersister {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityTaskHistoryPersister.class);
  
  private final TaskManager taskManager;
  private final HistoryManager historyManager;
  
  @Inject
  public SingularityTaskHistoryPersister(TaskManager taskManager, HistoryManager historyManager) {
    this.taskManager = taskManager;
    this.historyManager = historyManager;
  }
  
  public void checkInactiveTaskIds() {
    LOG.info("Checking inactive task ids for task history persistance");
    
    final long start = System.currentTimeMillis();
    
    final List<SingularityTaskId> allTaskIds = taskManager.getAllTaskIds();
     
    final Set<SingularityTaskId> activeTaskIds = Sets.newHashSet(taskManager.getActiveTaskIds());
    final Set<SingularityTaskId> lbCleaningTaskIds = Sets.newHashSet(taskManager.getLBCleanupTasks());
    
    int numTotal = 0;
    int numTransferred = 0;
    
    for (SingularityTaskId taskId : allTaskIds) {
      if (activeTaskIds.contains(taskId) || lbCleaningTaskIds.contains(taskId)) {
        continue;
      }
      if (transferToHistoryDB(taskId)) {
        numTransferred++;
      }
      numTotal++;
    }
    
    LOG.info("Transferred {} out of {} inactive task ids (total {}) in {}", numTransferred, numTotal, allTaskIds.size(), Utils.duration(start));
  }
  
  private boolean transferToHistoryDB(SingularityTaskId inactiveTaskId) {
    final long start = System.currentTimeMillis();
    
    final Optional<SingularityTaskHistory> taskHistory = taskManager.getTaskHistory(inactiveTaskId);
    
    if (taskHistory.isPresent()) {
      try {
        historyManager.saveTaskHistory(taskHistory.get());
      } catch (Throwable t) {
        LOG.warn("Failed to persist task into History for task {}", inactiveTaskId, t);
        return false;
      }
    } else {
      LOG.warn("Inactive task {} did not have a task to persist", inactiveTaskId);
    }
    
    taskManager.deleteTaskHistory(inactiveTaskId);
    
    LOG.debug("Moved task history for {} from ZK to History in {}", inactiveTaskId, Utils.duration(start));
  
    return true;
  }

}
