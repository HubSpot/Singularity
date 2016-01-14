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

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

@UseStringTemplate3StatementLocator
public abstract class HistoryJDBI implements GetHandle {

  @SqlUpdate("INSERT INTO requestHistory (requestId, request, createdAt, requestState, user, message) VALUES (:requestId, :request, :createdAt, :requestState, :user, :message)")
  abstract void insertRequestHistory(@Bind("requestId") String requestId, @Bind("request") byte[] request, @Bind("createdAt") Date createdAt, @Bind("requestState") String requestState, @Bind("user") String user, @Bind("message") String message);

  @SqlUpdate("INSERT INTO deployHistory (requestId, deployId, createdAt, user, message, deployStateAt, deployState, bytes) VALUES (:requestId, :deployId, :createdAt, :user, :message, :deployStateAt, :deployState, :bytes)")
  abstract void insertDeployHistory(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("createdAt") Date createdAt, @Bind("user") String user, @Bind("message") String message, @Bind("deployStateAt") Date deployStateAt, @Bind("deployState") String deployState, @Bind("bytes") byte[] bytes);

  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, bytes, updatedAt, lastTaskStatus, runId, deployId, host, startedAt) VALUES (:requestId, :taskId, :bytes, :updatedAt, :lastTaskStatus, :runId, :deployId, :host, :startedAt)")
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

  @SqlQuery("SELECT request, createdAt, requestState, user, message FROM requestHistory WHERE requestId = :requestId ORDER BY createdAt <orderDirection> LIMIT :limitStart, :limitCount")
  abstract List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT DISTINCT requestId FROM requestHistory WHERE requestId LIKE CONCAT(:requestIdLike, '%') LIMIT :limitStart, :limitCount")
  abstract List<String> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId, COUNT(*) as count FROM taskHistory WHERE updatedAt \\< :updatedAt GROUP BY requestId")
  abstract List<SingularityRequestIdCount> getRequestIdCounts(@Bind("updatedAt") Date updatedAt);

  @SqlQuery("SELECT MIN(updatedAt) from (SELECT updatedAt FROM taskHistory WHERE requestId = :requestId ORDER BY updatedAt DESC LIMIT :limit) as alias")
  abstract Date getMinUpdatedAtWithLimitForRequest(@Bind("requestId") String requestId, @Bind("limit") Integer limit);

  @SqlUpdate("UPDATE taskHistory SET bytes = '' WHERE requestId = :requestId AND updatedAt \\< :updatedAtBefore")
  abstract void updateTaskHistoryNullBytesForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore);

  @SqlUpdate("DELETE FROM taskHistory WHERE requestId = :requestId AND updatedAt \\< :updatedAtBefore")
  abstract void deleteTaskHistoryForRequestBefore(@Bind("requestId") String requestId, @Bind("updatedAtBefore") Date updatedAtBefore);

  abstract void close();

  private static final String GET_TASK_ID_HISTORY_QUERY = "SELECT taskId, requestId, updatedAt, lastTaskStatus, runId FROM taskHistory WHERE requestId = :requestId";

  public List<SingularityTaskIdHistory> getTaskIdHistory(String requestId, Optional<String> deployId, Optional<String> host,
      Optional<ExtendedTaskState> lastTaskStatus, Optional<Long> startedBefore, Optional<Long> startedAfter, Optional<OrderDirection> orderDirection,
      Optional<Integer> limitStart, Integer limitCount) {

    Map<String, Object> binds = new HashMap<>();
    binds.put("requestId", requestId);

    StringBuilder sqlBuilder = new StringBuilder(GET_TASK_ID_HISTORY_QUERY);

    if (deployId.isPresent()) {
      sqlBuilder.append(" AND deployId = :deployId");
      binds.put("deployId", deployId.get());
    }

    if (host.isPresent()) {
      sqlBuilder.append(" AND host = :host");
      binds.put("host", host.get());
    }

    if (lastTaskStatus.isPresent()) {
      sqlBuilder.append(" AND lastTaskStatus = :lastTaskStatus");
      binds.put("lastTaskStatus", lastTaskStatus.get().name());
    }

    if (startedBefore.isPresent()) {
      sqlBuilder.append(" AND startedAt \\< :startedBefore");
      binds.put("startedBefore", startedBefore.get());
    }

    if (startedAfter.isPresent()) {
      sqlBuilder.append(" AND startedAt \\> :startedAfter");
      binds.put("startedAfter", startedAfter.get());
    }

    sqlBuilder.append(" ORDER BY startedAt ");
    sqlBuilder.append(orderDirection.or(OrderDirection.DESC).name());

    if (limitStart.isPresent()) {
      sqlBuilder.append(" LIMIT :limitStart, ");
      binds.put("limitStart", limitStart.get());
    } else {
      sqlBuilder.append("LIMIT ");
    }

    sqlBuilder.append(":limitCount");
    binds.put("limitCount", limitCount);

    String sql = sqlBuilder.toString();

    Query<SingularityTaskIdHistory> query = getHandle().createQuery(sql).mapTo(SingularityTaskIdHistory.class);
    for (Map.Entry<String, Object> entry : binds.entrySet()) {
      query.bind(entry.getKey(), entry.getValue());
    }

    return query.list();
  }

}
