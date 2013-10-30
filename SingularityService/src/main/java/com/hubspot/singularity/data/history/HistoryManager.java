package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;

public interface HistoryManager {

  void saveTaskHistory(SingularityTask task, String driverStatus);
  
  void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message);
  
  List<SingularityPendingTaskId> getTaskHistoryForRequest(String requestName);
  
  SingularityTaskHistory getTaskHistory(String taskId);
  
}
