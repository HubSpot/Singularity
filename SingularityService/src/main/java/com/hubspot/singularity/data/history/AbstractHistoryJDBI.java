package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityTaskIdHistory;

// Common code for DB queries
public abstract class AbstractHistoryJDBI implements HistoryJDBI {
    protected static final Logger LOG = LoggerFactory.getLogger(HistoryJDBI.class);

    private static final String GET_TASK_ID_HISTORY_QUERY = "SELECT taskId, requestId, updatedAt, lastTaskStatus, runId FROM taskHistory";
    private static final String GET_TASK_ID_HISTORY_COUNT_QUERY = "SELECT COUNT(*) FROM taskHistory";

    protected void addWhereOrAnd(StringBuilder sqlBuilder, boolean shouldUseWhere) {
        if (shouldUseWhere) {
            sqlBuilder.append(" WHERE ");
        } else {
            sqlBuilder.append(" AND ");
        }
    }

    protected void applyTaskIdHistoryBaseQuery(StringBuilder sqlBuilder, Map<String, Object> binds, Optional<String> requestId, Optional<String> deployId, Optional<String> runId, Optional<String> host,
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

    @Override
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

        // NOTE: PG, MySQL are both compatible with OFFSET LIMIT syntax, while only MySQL understands LIMIT offset, limit.
        if (limitCount != null ){
            sqlBuilder.append(" LIMIT :limitCount");
            binds.put("limitCount", limitCount);
        }

        if (limitStart.isPresent()) {
            sqlBuilder.append(" OFFSET :limitStart ");
            binds.put("limitStart", limitStart.get());
        }

        final String sql = sqlBuilder.toString();

        LOG.trace("Generated sql for task search: {}, binds: {}", sql, binds);

        final Query<SingularityTaskIdHistory> query = getHandle().createQuery(sql).mapTo(SingularityTaskIdHistory.class);
        for (Map.Entry<String, Object> entry : binds.entrySet()) {
            query.bind(entry.getKey(), entry.getValue());
        }

        return query.list();
    }

    @Override
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
