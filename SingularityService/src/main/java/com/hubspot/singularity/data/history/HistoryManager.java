package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public interface HistoryManager {

  void saveRequestHistory(SingularityRequest request);
  
  void saveRequestHistoryUpdate(SingularityRequest request);
  
  void saveTaskHistory(SingularityTask task, String driverStatus);
  
  void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message);
  
  List<SingularityTaskId> getTaskHistoryForRequest(String requestName);
  
  List<SingularityTaskId> getTaskHistoryForRequestLike(String requestNameLike);
  
  SingularityTaskHistory getTaskHistory(String taskId);
 
  List<SingularityRequestHistory> getRequestHistory(String requestName);
  
  List<SingularityRequestHistory> getRequestHistoryLike(String requestNameLike);
  
}
