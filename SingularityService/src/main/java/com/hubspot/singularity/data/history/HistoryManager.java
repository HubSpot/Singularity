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

  public enum OrderDirection {
    ASC, DESC;
  }
  
  public enum TaskHistoryOrderBy {
    requestId, taskId, lastTaskStatus, createdAt, updatedAt,
  }
  
  public enum RequestHistoryOrderBy {
    requestId, createdAt
  }
  
  void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user);
  
  void saveTaskHistory(SingularityTask task, String driverStatus);
  
  void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message, Date timestamp);
  
  void updateTaskHistory(String taskId, String statusUpdate, Date timestamp);
  
  void updateTaskDirectory(String taskId, String directory);
  
  List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Optional<TaskHistoryOrderBy> orderBy, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount);
  
  List<SingularityTaskIdHistory> getActiveTaskHistoryForRequest(String requestId);
  
  List<SingularityTaskIdHistory> getTaskHistoryForRequestLike(String requestIdLike, Optional<TaskHistoryOrderBy> orderBy, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount);
  
  Optional<SingularityTaskHistory> getTaskHistory(String taskId, boolean fetchUpdates);
 
  boolean hasTaskUpdate(String taskId, String status);
  
  List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<RequestHistoryOrderBy> orderBy, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount);
  
  List<SingularityRequestHistory> getRequestHistoryLike(String requestIdLike, Optional<RequestHistoryOrderBy> orderBy, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount);
  
  Optional<SingularityTaskIdHistory> getLastTaskForRequest(String requestId);
   
}
