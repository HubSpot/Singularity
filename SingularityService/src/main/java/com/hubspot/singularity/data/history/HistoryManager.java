package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.data.history.SingularityRequestHistory.RequestState;

public interface HistoryManager {

  void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user);
  
  void saveTaskHistory(SingularityTask task, String driverStatus);
  
  void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message);
  
  List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId);
  
  List<SingularityTaskIdHistory> getTaskHistoryForRequestLike(String requestIdLike);
  
  SingularityTaskHistory getTaskHistory(String taskId);
 
  List<SingularityRequestHistory> getRequestHistory(String requestId);
  
  List<SingularityRequestHistory> getRequestHistoryLike(String requestIdLike);
  
}
