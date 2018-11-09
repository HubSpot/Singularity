package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

@UseStringTemplate3StatementLocator
public abstract class MySQLHistoryJDBI extends AbstractHistoryJDBI {

  @SqlUpdate("INSERT INTO requestHistory (requestId, request, createdAt, requestState, user, message) VALUES (:requestId, :request, :createdAt, :requestState, :user, :message)")
  public abstract void insertRequestHistory(@Bind("requestId") String requestId, @Bind("request") byte[] request, @Bind("createdAt") Date createdAt, @Bind("requestState") String requestState, @Bind("user") String user, @Bind("message") String message);

  @SqlUpdate("INSERT INTO deployHistory (requestId, deployId, createdAt, user, message, deployStateAt, deployState, bytes) VALUES (:requestId, :deployId, :createdAt, :user, :message, :deployStateAt, :deployState, :bytes)")
  public abstract void insertDeployHistory(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("createdAt") Date createdAt, @Bind("user") String user, @Bind("message") String message, @Bind("deployStateAt") Date deployStateAt, @Bind("deployState") String deployState, @Bind("bytes") byte[] bytes);

  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, bytes, updatedAt, lastTaskStatus, runId, deployId, host, startedAt, purged) VALUES (:requestId, :taskId, :bytes, :updatedAt, :lastTaskStatus, :runId, :deployId, :host, :startedAt, false)")
  public abstract void insertTaskHistory(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("bytes") byte[] bytes, @Bind("updatedAt") Date updatedAt,
                                         @Bind("lastTaskStatus") String lastTaskStatus, @Bind("runId") String runId, @Bind("deployId") String deployId, @Bind("host") String host,
                                         @Bind("startedAt") Date startedAt);

  @SqlQuery("SELECT bytes FROM taskHistory WHERE taskId = :taskId")
  public abstract byte[] getTaskHistoryForTask(@Bind("taskId") String taskId);

  @SqlQuery("SELECT bytes FROM taskHistory WHERE requestId = :requestId AND runId = :runId")
  public abstract byte[] getTaskHistoryForTaskByRunId(@Bind("requestId") String requestId, @Bind("runId") String runId);

  @SqlQuery("SELECT bytes FROM deployHistory WHERE requestId = :requestId AND deployId = :deployId")
  public abstract byte[] getDeployHistoryForDeploy(@Bind("requestId") String requestId, @Bind("deployId") String deployId);

  @SqlQuery("SELECT requestId, deployId, createdAt, user, message, deployStateAt, deployState FROM deployHistory WHERE requestId = :requestId ORDER BY createdAt DESC LIMIT :limitStart,:limitCount")
  public abstract List<SingularityDeployHistory> getDeployHistoryForRequest(@Bind("requestId") String requestId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM deployHistory WHERE requestId = :requestId")
  public abstract int getDeployHistoryForRequestCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT request, createdAt, requestState, user, message FROM requestHistory WHERE requestId = :requestId ORDER BY createdAt <orderDirection> LIMIT :limitStart, :limitCount")
  public abstract List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM requestHistory WHERE requestId = :requestId")
  public abstract int getRequestHistoryCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT DISTINCT requestId FROM requestHistory WHERE requestId LIKE CONCAT(:requestIdLike, '%') LIMIT :limitStart, :limitCount")
  public abstract List<String> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId, COUNT(*) as count FROM taskHistory WHERE updatedAt \\< :updatedAt GROUP BY requestId")
  public abstract List<SingularityRequestIdCount> getRequestIdCounts(@Bind("updatedAt") Date updatedAt);

  @SqlQuery("SELECT MIN(updatedAt) from (SELECT updatedAt FROM taskHistory WHERE requestId = :requestId ORDER BY updatedAt DESC LIMIT :limit) as alias")
  public abstract Date getMinUpdatedAtWithLimitForRequest(@Bind("requestId") String requestId, @Bind("limit") Integer limit);

  @SqlUpdate("UPDATE taskHistory SET bytes = '', purged = true WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  public abstract void updateTaskHistoryNullBytesForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlUpdate("DELETE FROM taskHistory WHERE requestId = :requestId AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  public abstract void deleteTaskHistoryForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlQuery("SELECT DISTINCT requestId FROM taskHistory")
  public abstract List<String> getRequestIdsInTaskHistory();

  @SqlQuery("SELECT COUNT(*) FROM taskHistory WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore")
  public abstract int getUnpurgedTaskHistoryCountByRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore);


  public abstract void close();

}
