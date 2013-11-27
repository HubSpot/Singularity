package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;

public interface HistoryManager {

  void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user);
  
  void saveTaskHistory(SingularityTask task, String driverStatus);
  
  void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message, Date timestamp);
  
  void updateTaskHistory(String taskId, String statusUpdate, Date timestamp);
  
  void updateTaskDirectory(String taskId, String directory);
  
  List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount);
  
  List<SingularityTaskIdHistory> getTaskHistoryForRequestLike(String requestIdLike, Integer limitStart, Integer limitCount);
  
  Optional<SingularityTaskHistory> getTaskHistory(String taskId);
 
  List<SingularityRequestHistory> getRequestHistory(String requestId);
  
  List<SingularityRequestHistory> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount);
  
}
