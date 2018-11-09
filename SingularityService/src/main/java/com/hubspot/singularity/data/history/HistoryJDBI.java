package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;

public interface HistoryJDBI extends GetHandle {

  void insertRequestHistory(String requestId,byte[] request, Date createdAt, String requestState, String user, String message);
  void insertDeployHistory(String requestId,String deployId, Date createdAt, String user, String message, Date deployStateAt, String deployState, byte[] bytes);
  void insertTaskHistory(String requestId,String taskId, byte[] bytes,Date updatedAt,
                         String lastTaskStatus, String runId,String deployId, String host,
                         Date startedAt);

  byte[] getTaskHistoryForTask(String taskId);
  byte[] getTaskHistoryForTaskByRunId(String requestId, String runId);

  byte[] getDeployHistoryForDeploy(String requestId, String deployId);
  List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount);
  int getDeployHistoryForRequestCount(String requestId);

  List<SingularityRequestHistory> getRequestHistory(String requestId, String orderDirection, Integer limitStart, Integer limitCount);
  int getRequestHistoryCount(String requestId);
  List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart,Integer limitCount);
  List<SingularityRequestIdCount> getRequestIdCounts(Date updatedAt);

  Date getMinUpdatedAtWithLimitForRequest(String requestId, Integer limit);

  void updateTaskHistoryNullBytesForRequestBefore(String requestId, Date updatedAtBefore, Integer purgeLimitPerQuery);
  void deleteTaskHistoryForRequestBefore(String requestId,Date updatedAtBefore, Integer purgeLimitPerQuery);
  List<String> getRequestIdsInTaskHistory();
  int getUnpurgedTaskHistoryCountByRequestBefore(String requestId, Date updatedAtBefore);

  void close();

  List<SingularityTaskIdHistory> getTaskIdHistory(Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
                                                  Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
                                                  Optional<Long> updatedAfter, Optional<OrderDirection> orderDirection, Optional<Integer> limitStart, Integer limitCount);
  int getTaskIdHistoryCount(Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
                            Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
                            Optional<Long> updatedAfter);
}
