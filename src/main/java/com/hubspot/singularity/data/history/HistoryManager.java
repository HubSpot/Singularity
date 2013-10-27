package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.data.SingularityTask;
import com.hubspot.singularity.data.SingularityTaskId;

public interface HistoryManager {

  void saveTaskHistory(SingularityTask task, String driverStatus);
  
  void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message);
  
  List<SingularityTaskId> getTaskHistoryForRequest(String requestName);
  
  SingularityTaskHistory getTaskHistory(String taskId);
  
}
