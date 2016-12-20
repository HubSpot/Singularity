package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

@UseStringTemplate3StatementLocator
public abstract class HistoryJDBI implements GetHandle {
  private static final Logger LOG = LoggerFactory.getLogger(HistoryJDBI.class);

  @SqlUpdate("INSERT INTO requestHistory (requestId, request, createdAt, requestState, user, message) VALUES (:requestId, :request, :createdAt, :requestState, :user, :message)")
  abstract void insertRequestHistory(@Bind("requestId") String requestId, @Bind("request") byte[] request, @Bind("createdAt") Date createdAt, @Bind("requestState") String requestState, @Bind("user") String user, @Bind("message") String message);

  @SqlUpdate("INSERT INTO deployHistory (requestId, deployId, createdAt, user, message, deployStateAt, deployState, bytes) VALUES (:requestId, :deployId, :createdAt, :user, :message, :deployStateAt, :deployState, :bytes)")
  abstract void insertDeployHistory(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("createdAt") Date createdAt, @Bind("user") String user, @Bind("message") String message, @Bind("deployStateAt") Date deployStateAt, @Bind("deployState") String deployState, @Bind("bytes") byte[] bytes);

  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, bytes, updatedAt, lastTaskStatus, runId, deployId, host, startedAt, purged) VALUES (:requestId, :taskId, :bytes, :updatedAt, :lastTaskStatus, :runId, :deployId, :host, :startedAt, false)")
  abstract void insertTaskHistory(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("bytes") byte[] bytes, @Bind("updatedAt") Date updatedAt,
      @Bind("lastTaskStatus") String lastTaskStatus, @Bind("runId") String runId, @Bind("deployId") String deployId, @Bind("host") String host,
      @Bind("startedAt") Date startedAt);

  @SqlQuery("SELECT bytes FROM taskHistory WHERE taskId = :taskId")
  abstract byte[] getTaskHistoryForTask(@Bind("taskId") String taskId);

  @SqlQuery("SELECT bytes FROM taskHistory WHERE requestId = :requestId AND runId = :runId")
  abstract byte[] getTaskHistoryForTaskByRunId(@Bind("requestId") String requestId, @Bind("runId") String runId);

  @SqlQuery("SELECT bytes FROM deployHistory WHERE requestId = :requestId AND deployId = :deployId")
  abstract byte[] getDeployHistoryForDeploy(@Bind("requestId") String requestId, @Bind("deployId") String deployId);

  @SqlQuery("SELECT requestId, deployId, createdAt, user, message, deployStateAt, deployState FROM deployHistory WHERE requestId = :requestId ORDER BY createdAt DESC LIMIT :limitStart, :limitCount")
  abstract List<SingularityDeployHistory> getDeployHistoryForRequest(@Bind("requestId") String requestId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM deployHistory WHERE requestId = :requestId")
  abstract int getDeployHistoryForRequestCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT request, createdAt, requestState, user, message FROM requestHistory WHERE requestId = :requestId ORDER BY createdAt <orderDirection> LIMIT :limitStart, :limitCount")
  abstract List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT COUNT(*) FROM requestHistory WHERE requestId = :requestId")
  abstract int getRequestHistoryCount(@Bind("requestId") String requestId);

  @SqlQuery("SELECT DISTINCT requestId FROM requestHistory WHERE requestId LIKE CONCAT(:requestIdLike, '%') LIMIT :limitStart, :limitCount")
  abstract List<String> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId, COUNT(*) as count FROM taskHistory WHERE updatedAt \\< :updatedAt GROUP BY requestId")
  abstract List<SingularityRequestIdCount> getRequestIdCounts(@Bind("updatedAt") Date updatedAt);

  @SqlQuery("SELECT MIN(updatedAt) from (SELECT updatedAt FROM taskHistory WHERE requestId = :requestId ORDER BY updatedAt DESC LIMIT :limit) as alias")
  abstract Date getMinUpdatedAtWithLimitForRequest(@Bind("requestId") String requestId, @Bind("limit") Integer limit);

  @SqlUpdate("UPDATE taskHistory SET bytes = '', purged = true WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  abstract void updateTaskHistoryNullBytesForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlUpdate("DELETE FROM taskHistory WHERE requestId = :requestId AND updatedAt \\< :updatedAtBefore LIMIT :purgeLimitPerQuery")
  abstract void deleteTaskHistoryForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore, @Bind("purgeLimitPerQuery") Integer purgeLimitPerQuery);

  @SqlQuery("SELECT DISTINCT requestId FROM taskHistory")
  abstract List<String> getRequestIdsInTaskHistory();

  @SqlQuery("SELECT COUNT(*) FROM taskHistory WHERE requestId = :requestId AND purged = false AND updatedAt \\< :updatedAtBefore")
  abstract int getUnpurgedTaskHistoryCountByRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore);


  abstract void close();

  private static final String GET_TASK_ID_HISTORY_QUERY = "SELECT taskId, requestId, updatedAt, lastTaskStatus, runId FROM taskHistory";
  private static final String GET_TASK_ID_HISTORY_COUNT_QUERY = "SELECT COUNT(*) FROM taskHistory";


  private void addWhereOrAnd(StringBuilder sqlBuilder, boolean shouldUseWhere) {
    if (shouldUseWhere) {
      sqlBuilder.append(" WHERE ");
    } else {
      sqlBuilder.append(" AND ");
    }
  }

  private void applyTaskIdHistoryBaseQuery(StringBuilder sqlBuilder, Map<String, Object> binds, Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
      Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
      Optional<Long> updatedAfter) {
    if (requestId.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("requestId = :requestId");
      binds.put("requestId", requestId.get());
    }

    if (deployId.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("deployId = :deployId");
      binds.put("deployId", deployId.get());
    }

    if (runId.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("runId = :runId");
      binds.put("runId", runId.get());
    }

    if (host.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("host = :host");
      binds.put("host", host.get());
    }

    if (lastTaskStatus.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("lastTaskStatus = :lastTaskStatus");
      binds.put("lastTaskStatus", lastTaskStatus.get().name());
    }

    if (startedBefore.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("startedAt < :startedBefore");
      binds.put("startedBefore", new Date(startedBefore.get()));
    }

    if (startedAfter.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("startedAt > :startedAfter");
      binds.put("startedAfter", new Date(startedAfter.get()));
    }

    if (updatedBefore.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("updatedAt < :updatedBefore");
      binds.put("updatedBefore", new Date(updatedBefore.get()));
    }

    if (updatedAfter.isPresent()) {
      addWhereOrAnd(sqlBuilder, binds.isEmpty());
      sqlBuilder.append("updatedAt > :updatedAfter");
      binds.put("updatedAfter", new Date(updatedAfter.get()));
    }
  }

    public List<SingularityTaskIdHistory> getTaskIdHistory(Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
      Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
      Optional<Long> updatedAfter, Optional<OrderDirection> orderDirection, Optional<Integer> limitStart, Integer limitCount) {

    final Map<String, Object> binds = new HashMap<>();
    final StringBuilder sqlBuilder = new StringBuilder(GET_TASK_ID_HISTORY_QUERY);

    applyTaskIdHistoryBaseQuery(sqlBuilder, binds, requestId, deployId, runId, host, lastTaskStatus, startedBefore, startedAfter, updatedBefore, updatedAfter);

    sqlBuilder.append(" ORDER BY startedAt ");
    sqlBuilder.append(orderDirection.or(OrderDirection.DESC).name());

    if (!requestId.isPresent()) {
      sqlBuilder.append(", requestId ");
      sqlBuilder.append(orderDirection.or(OrderDirection.DESC).name());
    }

    if (limitStart.isPresent()) {
      sqlBuilder.append(" LIMIT :limitStart, ");
      binds.put("limitStart", limitStart.get());
    } else {
      sqlBuilder.append(" LIMIT ");
    }

    sqlBuilder.append(":limitCount");
    binds.put("limitCount", limitCount);

    final String sql = sqlBuilder.toString();

    LOG.trace("Generated sql for task search: {}, binds: {}", sql, binds);

    final Query<SingularityTaskIdHistory> query = getHandle().createQuery(sql).mapTo(SingularityTaskIdHistory.class);
    for (Map.Entry<String, Object> entry : binds.entrySet()) {
      query.bind(entry.getKey(), entry.getValue());
    }

    return query.list();
  }

  public int getTaskIdHistoryCount(Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
      Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<Long> updatedBefore,
      Optional<Long> updatedAfter) {

    final Map<String, Object> binds = new HashMap<>();
    final StringBuilder sqlBuilder = new StringBuilder(GET_TASK_ID_HISTORY_COUNT_QUERY);

    applyTaskIdHistoryBaseQuery(sqlBuilder, binds, requestId, deployId, runId, host, lastTaskStatus, startedBefore, startedAfter, updatedBefore, updatedAfter);

    final String sql = sqlBuilder.toString();

    LOG.trace("Generated sql for task search count: {}, binds: {}", sql, binds);

    final Query<Integer> query = getHandle().createQuery(sql).mapTo(Integer.class);
    for (Map.Entry<String, Object> entry : binds.entrySet()) {
      query.bind(entry.getKey(), entry.getValue());
    }

    return query.first();
  }

}
