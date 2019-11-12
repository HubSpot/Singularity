package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.jdbi.v3.json.Json;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.Define;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

public interface PostgresHistoryJDBI extends AbstractHistoryJDBI {

  @SqlUpdate("INSERT INTO requestHistory (requestId, json, createdAt, requestState, f_user, message) VALUES (:requestId, :json, :createdAt, :requestState, :user, :message)" +
      "ON DUPLICATE KEY UPDATE json = :json, requestState = :requestState, user = :user, message = :message")
  void insertRequestHistory(@Bind("requestId") String requestId, @Bind("json") @Json SingularityRequest request, @Bind("createdAt") Date createdAt,
                            @Bind("requestState") String requestState, @Bind("user") String user, @Bind("message") String message);

  @SqlUpdate("INSERT INTO deployHistory (requestId, deployId, createdAt, f_user, message, deployStateAt, deployState, json) VALUES (:requestId, :deployId, :createdAt, :user, :message, :deployStateAt, :deployState, :json)" +
      "ON DUPLICATE KEY UPDATE createdAt = :createdAt, user = :user, message = :message, deployStateAt = :deployStateAt, deployState = :deployState, json = :json")
  void insertDeployHistory(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("createdAt") Date createdAt, @Bind("user") String user,
                           @Bind("message") String message, @Bind("deployStateAt") Date deployStateAt, @Bind("deployState") String deployState, @Bind("json") @Json SingularityDeployHistory deployHistory);

  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, json, updatedAt, lastTaskStatus, runId, deployId, host, startedAt, purged) VALUES (:requestId, :taskId, :json, :updatedAt, :lastTaskStatus, :runId, :deployId, :host, :startedAt, false)")
  void insertTaskHistory(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("json") @Json SingularityTaskHistory taskHistory, @Bind("updatedAt") Date updatedAt,
                         @Bind("lastTaskStatus") String lastTaskStatus, @Bind("runId") String runId, @Bind("deployId") String deployId, @Bind("host") String host,
                         @Bind("startedAt") Date startedAt);

  @SingleValue
  @SqlQuery("SELECT json FROM taskHistory WHERE taskId = :taskId")
  @Json
  SingularityTaskHistory getTaskHistoryForTask(@Bind("taskId") String taskId);

  @SingleValue
  @SqlQuery("SELECT json FROM taskHistory WHERE requestId = :requestId AND runId = :runId")
  @Json
  SingularityTaskHistory getTaskHistoryForTaskByRunId(@Bind("requestId") String requestId, @Bind("runId") String runId);

  @SingleValue
  @SqlQuery("SELECT json FROM deployHistory WHERE requestId = :requestId AND deployId = :deployId")
  @Json
  SingularityDeployHistory getDeployHistoryForDeploy(@Bind("requestId") String requestId, @Bind("deployId") String deployId);

  @SqlQuery("SELECT requestId, deployId, createdAt, f_user, message, deployStateAt, deployState FROM deployHistory WHERE requestId = :requestId ORDER BY createdAt DESC OFFSET :limitStart LIMIT :limitCount")
  List<SingularityDeployHistory> getDeployHistoryForRequest(@Bind("requestId") String requestId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM deployHistory WHERE requestId = :requestId")
  int getDeployHistoryForRequestCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT json, request, createdAt, requestState, f_user, message FROM requestHistory WHERE requestId = :requestId ORDER BY createdAt <orderDirection> OFFSET :limitStart LIMIT :limitCount")
  List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM requestHistory WHERE requestId = :requestId")
  int getRequestHistoryCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT DISTINCT requestId as id FROM requestHistory WHERE requestId LIKE CONCAT(:requestIdLike, '%') OFFSET :limitStart LIMIT :limitCount")
  List<String> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId, COUNT(*) as count FROM taskHistory WHERE updatedAt \\< :updatedAt GROUP BY requestId")
  List<SingularityRequestIdCount> getRequestIdCounts(@Bind("updatedAt") Date updatedAt);

  @SqlQuery("SELECT MIN(updatedAt) from (SELECT updatedAt FROM taskHistory WHERE requestId = :requestId ORDER BY updatedAt DESC LIMIT :limit) as alias")
  Date getMinUpdatedAtWithLimitForRequest(@Bind("requestId") String requestId, @Bind("limit") Integer limit);

  @SqlUpdate("UPDATE taskHistory SET json = NULL, purged = true WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  void updateTaskHistoryNullBytesForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlUpdate("DELETE FROM taskHistory WHERE requestId = :requestId AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  void deleteTaskHistoryForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlQuery("SELECT DISTINCT requestId as id FROM taskHistory")
  List<String> getRequestIdsInTaskHistory();

  @SqlQuery("SELECT COUNT(*) FROM taskHistory WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore")
  int getUnpurgedTaskHistoryCountByRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore);


  @SqlQuery("SELECT DISTINCT requestId AS id FROM requestHistory")
  List<String> getRequestIdsWithHistory();

  @SqlUpdate("DELETE FROM requestHistory WHERE requestId = :requestId AND createdAt \\< :threshold LIMIT :batchSize")
  int purgeRequestHistory(@Bind("requestId") String requestId, @Bind("threshold") Date threshold, @Bind("batchSize") int batchSize);

  @SqlQuery("SELECT DISTINCT requestId AS id FROM deployHistory")
  List<String> getRequestIdsWithDeploys();

  @SqlUpdate("DELETE FROM deployHistory WHERE requestId = :requestId AND createdAt \\< :threshold LIMIT :batchSize")
  int purgeDeployHistory(@Bind("requestId") String requestId, @Bind("threshold") Date threshold, @Bind("batchSize") int batchSize);

  // Deprecated queries for before json backfill is finished
  @Deprecated
  @SingleValue
  @SqlQuery("SELECT bytes FROM taskHistory WHERE taskId = :taskId")
  byte[] getTaskHistoryBytesForTask(@Bind("taskId") String taskId);

  @Deprecated
  @SingleValue
  @SqlQuery("SELECT bytes FROM taskHistory WHERE requestId = :requestId AND runId = :runId")
  byte[] getTaskHistoryBytesForTaskByRunId(@Bind("requestId") String requestId, @Bind("runId") String runId);

  @Deprecated
  @SingleValue
  @SqlQuery("SELECT bytes FROM deployHistory WHERE requestId = :requestId AND deployId = :deployId")
  byte[] getDeployHistoryBytesForDeploy(@Bind("requestId") String requestId, @Bind("deployId") String deployId);

  // Queries for history migration
  @SqlQuery("SELECT bytes FROM taskHistory WHERE requestId = :requestId AND purged = false AND bytes != '' AND bytes IS NOT NULL LIMIT :limit")
  List<byte[]> getTasksWithBytes(@Bind("requestId") String requestId, @Bind("limit") int limit);

  @SqlUpdate("UPDATE taskHistory SET json = :json, bytes = '' WHERE taskId = :taskId")
  void setTaskJson(@Bind("taskId") String taskId, @Bind("json") @Json SingularityTaskHistory taskHistory);

  @SqlQuery("SELECT request, createdAt FROM requestHistory WHERE request != '' AND request IS NOT NULL LIMIT :limit")
  List<SingularityRequestAndTime> getRequestsWithBytes(@Bind("limit") int limit);

  @SqlUpdate("UPDATE requestHistory SET json = :json, request = '' WHERE requestId = :requestId AND createdAt = :createdAt")
  void setRequestJson(@Bind("requestId") String requestId, @Bind("createdAt") Date createdAt, @Bind("json") @Json SingularityRequest request);

  @SqlQuery("SELECT bytes FROM deployHistory WHERE requestId = :requestId AND bytes != '' AND bytes IS NOT NULL LIMIT :limit")
  List<byte[]> getDeploysWithBytes(@Bind("requestId") String requestId, @Bind("limit") int limit);

  @SqlUpdate("UPDATE deployHistory SET json = :json, bytes = '' WHERE requestId = :requestId AND deployId = :deployId")
  void setDeployJson(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("json") @Json SingularityDeployHistory deployHistory);

  default void close() {
  }

}
