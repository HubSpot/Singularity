package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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

  @SqlUpdate("INSERT INTO requestHistory (requestId, requestJson, createdAt, requestState, f_user, message) VALUES (:requestId, :request, :createdAt, :requestState, :user, :message)")
  void insertRequestHistory(@Bind("requestId") String requestId, @Json SingularityRequest request, @Bind("createdAt") Date createdAt,
                                            @Bind("requestState") String requestState, @Bind("user") String user, @Bind("message") String message);

  @SqlUpdate("INSERT INTO deployHistory (requestId, deployId, createdAt, f_user, message, deployStateAt, deployState, deployJson) VALUES (:requestId, :deployId, :createdAt, :user, :message, :deployStateAt, :deployState, :deployHistory)")
  void insertDeployHistory(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("createdAt") Date createdAt, @Bind("user") String user, @Bind("message") String message, @Bind("deployStateAt") Date deployStateAt,
                                           @Bind("deployState") String deployState, @Json SingularityDeployHistory deployHistory);

  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, taskJson, updatedAt, lastTaskStatus, runId, deployId, host, startedAt, purged) VALUES (:requestId, :taskId, :taskHistory, :updatedAt, :lastTaskStatus, :runId, :deployId, :host, :startedAt, false)")
  void insertTaskHistory(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Json SingularityTaskHistory taskHistory, @Bind("updatedAt") Date updatedAt,
                                         @Bind("lastTaskStatus") String lastTaskStatus, @Bind("runId") String runId, @Bind("deployId") String deployId, @Bind("host") String host,
                                         @Bind("startedAt") Date startedAt);

  @SingleValue
  @Json
  @SqlQuery("SELECT taskJson FROM taskHistory WHERE taskId = :taskId")
  Optional<SingularityTaskHistory> getTaskHistoryForTask(@Bind("taskId") String taskId);

  @SingleValue
  @Json
  @SqlQuery("SELECT taskJson FROM taskHistory WHERE requestId = :requestId AND runId = :runId")
  Optional<SingularityTaskHistory> getTaskHistoryForTaskByRunId(@Bind("requestId") String requestId, @Bind("runId") String runId);

  @SingleValue
  @Json
  @SqlQuery("SELECT deployJson FROM deployHistory WHERE requestId = :requestId AND deployId = :deployId")
  Optional<SingularityDeployHistory> getDeployHistoryForDeploy(@Bind("requestId") String requestId, @Bind("deployId") String deployId);

  @SqlQuery("SELECT requestId, deployId, createdAt, f_user, message, deployStateAt, deployState FROM deployHistory WHERE requestId = :requestId ORDER BY createdAt DESC OFFSET :limitStart LIMIT :limitCount")
  List<SingularityDeployHistory> getDeployHistoryForRequest(@Bind("requestId") String requestId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM deployHistory WHERE requestId = :requestId")
  int getDeployHistoryForRequestCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT requestJson, createdAt, requestState, f_user, message FROM requestHistory WHERE requestId = :requestId ORDER BY createdAt <orderDirection> OFFSET :limitStart LIMIT :limitCount")
  List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM requestHistory WHERE requestId = :requestId")
  int getRequestHistoryCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT DISTINCT requestId as id FROM requestHistory WHERE requestId LIKE CONCAT(:requestIdLike, '%') OFFSET :limitStart LIMIT :limitCount")
  List<String> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId, COUNT(*) as count FROM taskHistory WHERE updatedAt \\< :updatedAt GROUP BY requestId")
  List<SingularityRequestIdCount> getRequestIdCounts(@Bind("updatedAt") Date updatedAt);

  @SqlQuery("SELECT MIN(updatedAt) from (SELECT updatedAt FROM taskHistory WHERE requestId = :requestId ORDER BY updatedAt DESC LIMIT :limit) as alias")
  Date getMinUpdatedAtWithLimitForRequest(@Bind("requestId") String requestId, @Bind("limit") Integer limit);

  @SqlUpdate("UPDATE taskHistory SET taskJson = NULL, purged = true WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  void updateTaskHistoryNullBytesForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlUpdate("DELETE FROM taskHistory WHERE requestId = :requestId AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  void deleteTaskHistoryForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlQuery("SELECT DISTINCT requestId as id FROM taskHistory")
  List<String> getRequestIdsInTaskHistory();

  @SqlQuery("SELECT COUNT(*) FROM taskHistory WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore")
  int getUnpurgedTaskHistoryCountByRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore);

  //Queries for history migration only
  @SqlQuery("SELECT bytes FROM taskHistory WHERE purged = false AND bytes != ''")
  List<byte[]> getTasksWithBytes(@Bind("limit") int limit);

  @SqlUpdate("UPDATE taskHistory SET taskJson = :taskHistory, bytes = '' WHERE taskId = :taskId")
  void setTaskJson(@Bind("taskId") String taskId, @Json SingularityTaskHistory taskHistory);

  default void close() {}

}
