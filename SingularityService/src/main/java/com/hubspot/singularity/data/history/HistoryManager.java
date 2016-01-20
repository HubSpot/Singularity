package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

public interface HistoryManager {

  void saveRequestHistoryUpdate(SingularityRequestHistory requestHistory);

  void saveTaskHistory(SingularityTaskHistory taskHistory);

  void saveDeployHistory(SingularityDeployHistory deployHistory);

  Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId);

  List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount);

  List<SingularityTaskIdHistory> getTaskIdHistory(String requestId, Optional<String> deployId, Optional<String> host,
      Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<OrderDirection> orderDirection,
      Optional<Integer> limitStart, Integer limitCount);

  Optional<SingularityTaskHistory> getTaskHistory(String taskId);

  Optional<SingularityTaskHistory> getTaskHistoryByRunId(String requestId, String runId);

  List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount);

  List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount);

  List<SingularityRequestIdCount> getRequestIdCounts(Date before);

  void purgeTaskHistory(String requestId, int count, Optional<Integer> limit, Optional<Date> purgeBefore, boolean deleteRowInsteadOfUpdate);

}
