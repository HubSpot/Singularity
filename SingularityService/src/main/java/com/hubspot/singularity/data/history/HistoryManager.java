package com.hubspot.singularity.data.history;

import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;

public interface HistoryManager {

  public enum OrderDirection {
    ASC, DESC;
  }

  void saveRequestHistoryUpdate(SingularityRequestHistory requestHistory);

  void saveTaskHistory(SingularityTaskHistory taskHistory);

  void saveDeployHistory(SingularityDeployHistory deployHistory);

  Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId);

  List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount);

  List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount);

  Optional<SingularityTaskHistory> getTaskHistory(String taskId);

  List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount);

  List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount);

}
