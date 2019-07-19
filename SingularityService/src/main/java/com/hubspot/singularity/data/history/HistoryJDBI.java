package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.jdbi.v3.sqlobject.SqlObject;

import java.util.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

public interface HistoryJDBI extends SqlObject {

  void insertRequestHistory(String requestId, SingularityRequest request, Date createdAt, String requestState, String user, String message);

  void insertDeployHistory(String requestId, String deployId, Date createdAt, String user, String message, Date deployStateAt, String deployState, SingularityDeployHistory deployHistory);

  void insertTaskHistory(String requestId, String taskId, SingularityTaskHistory taskHistory, Date updatedAt,
                         String lastTaskStatus, String runId, String deployId, String host, Date startedAt);

  SingularityTaskHistory getTaskHistoryForTask(String taskId);

  SingularityTaskHistory getTaskHistoryForTaskByRunId(String requestId, String runId);

  SingularityDeployHistory getDeployHistoryForDeploy(String requestId, String deployId);

  List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount);

  int getDeployHistoryForRequestCount(String requestId);

  List<SingularityRequestHistory> getRequestHistory(String requestId, String orderDirection, Integer limitStart, Integer limitCount);

  int getRequestHistoryCount(String requestId);

  List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount);

  List<SingularityRequestIdCount> getRequestIdCounts(Date updatedAt);

  Date getMinUpdatedAtWithLimitForRequest(String requestId, Integer limit);

  void updateTaskHistoryNullBytesForRequestBefore(String requestId, Date updatedAtBefore, Integer purgeLimitPerQuery);

  void deleteTaskHistoryForRequestBefore(String requestId, Date updatedAtBefore, Integer purgeLimitPerQuery);

  List<String> getRequestIdsInTaskHistory();

  int getUnpurgedTaskHistoryCountByRequestBefore(String requestId, Date updatedAtBefore);

  void close();

  List<SingularityTaskIdHistory> getTaskIdHistory(Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
                                                  Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
                                                  Optional<Long> updatedAfter, Optional<OrderDirection> orderDirection, Optional<Integer> limitStart, Integer limitCount);

  int getTaskIdHistoryCount(Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
                            Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
                            Optional<Long> updatedAfter);

  @Deprecated
  byte[] getTaskHistoryBytesForTask(String taskId);

  @Deprecated
  byte[] getTaskHistoryBytesForTaskByRunId(String requestId, String runId);

  @Deprecated
  byte[] getDeployHistoryBytesForDeploy(String requestId, String deployId);

  List<byte[]> getTasksWithBytes(String requestId, int limit);

  void setTaskJson(String taskId, SingularityTaskHistory taskHistory);

  List<SingularityRequestAndTime> getRequestsWithBytes(int limit);

  void setRequestJson(String requestId, Date createdAt, SingularityRequest request);

  List<byte[]> getDeploysWithBytes(String requestId, int limit);

  void setDeployJson(String requestId, String deployId, SingularityDeployHistory deployHistory);

  List<String> getRequestIdsWithHistory();

  int purgeRequestHistory(String requestId, Date threshold, int batchSize);

  List<String> getRequestIdsWithDeploys();

  int purgeDeployHistory(String requestId, Date threshold, int batchSize);
}
