package com.hubspot.singularity.data.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;
import javax.inject.Inject;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.result.ResultSetException;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityMappers {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityMappers.class);

  static class SingularityBytesMapper implements ColumnMapper<byte[]> {

    @Inject
    SingularityBytesMapper() {}

    @Override
    public byte[] map(ResultSet r, int index, StatementContext ctx) throws SQLException {
      return r.getBytes("bytes");
    }
  }

  static class DateMapper implements ColumnMapper<Date> {

    @Inject
    DateMapper() {}

    @Override
    public Date map(ResultSet r, int index, StatementContext ctx) throws SQLException {
      return new Date(r.getTimestamp(1).getTime());
    }
  }

  @Json
  static class SingularityJsonStringMapper implements ColumnMapper<String> {

    @Inject
    SingularityJsonStringMapper() {}

    @Override
    public String map(ResultSet r, int index, StatementContext ctx) throws SQLException {
      return r.getString("json");
    }
  }

  static class SingularityIdMapper implements ColumnMapper<String> {

    @Inject
    SingularityIdMapper() {}

    @Override
    public String map(ResultSet r, int index, StatementContext ctx) throws SQLException {
      return r.getString("id");
    }
  }

  static class SingularityTimestampMapper implements ColumnMapper<Long> {

    @Inject
    SingularityTimestampMapper() {}

    @Override
    public Long map(ResultSet r, int index, StatementContext ctx) throws SQLException {
      return r.getLong("timestamp");
    }
  }

  static class SingularityRequestHistoryMapper
    implements RowMapper<SingularityRequestHistory> {
    private final String userColumn;
    private final ObjectMapper objectMapper;

    @Inject
    SingularityRequestHistoryMapper(
      SingularityConfiguration singularityConfiguration,
      @Singularity ObjectMapper objectMapper
    ) {
      this.userColumn = getUserColumn(singularityConfiguration);
      this.objectMapper = objectMapper;
    }

    @Override
    public SingularityRequestHistory map(ResultSet r, StatementContext ctx)
      throws SQLException {
      try {
        SingularityRequest request;
        String json = r.getString("json");
        if (json != null) {
          request = objectMapper.readValue(json, SingularityRequest.class);
        } else {
          request =
            objectMapper.readValue(r.getBytes("request"), SingularityRequest.class);
        }
        return new SingularityRequestHistory(
          r.getTimestamp("createdAt").getTime(),
          Optional.ofNullable(r.getString(userColumn)),
          RequestHistoryType.valueOf(r.getString("requestState")),
          request,
          Optional.ofNullable(r.getString("message"))
        );
      } catch (IOException e) {
        throw new ResultSetException("Could not deserialize database result", e, ctx);
      }
    }
  }

  static class SingularityRequestWithTimeMapper
    implements RowMapper<SingularityRequestAndTime> {
    private final ObjectMapper objectMapper;

    @Inject
    SingularityRequestWithTimeMapper(@Singularity ObjectMapper objectMapper) {
      this.objectMapper = objectMapper;
    }

    @Override
    public SingularityRequestAndTime map(ResultSet r, StatementContext ctx)
      throws SQLException {
      try {
        return new SingularityRequestAndTime(
          objectMapper.readValue(r.getBytes("request"), SingularityRequest.class),
          r.getTimestamp("createdAt").getTime()
        );
      } catch (IOException e) {
        throw new ResultSetException("Could not deserialize database result", e, ctx);
      }
    }
  }

  static class SingularityTaskIdHistoryMapper
    implements RowMapper<SingularityTaskIdHistory> {
    private final IdTranscoder<SingularityTaskId> singularityTaskIdTranscoder;

    @Inject
    SingularityTaskIdHistoryMapper(
      final IdTranscoder<SingularityTaskId> singularityTaskIdTranscoder
    ) {
      this.singularityTaskIdTranscoder = singularityTaskIdTranscoder;
    }

    @Override
    public SingularityTaskIdHistory map(ResultSet r, StatementContext ctx)
      throws SQLException {
      try {
        final SingularityTaskId taskId = singularityTaskIdTranscoder.fromString(
          r.getString("taskId")
        );

        final String lastTaskStatus = r.getString("lastTaskStatus");

        Optional<ExtendedTaskState> lastTaskState = Optional.empty();

        if (lastTaskStatus != null) {
          try {
            lastTaskState = Optional.of(ExtendedTaskState.valueOf(lastTaskStatus));
          } catch (IllegalArgumentException e) {
            LOG.warn(
              "Found invalid taskState {} in DB for task {}",
              lastTaskState,
              taskId,
              e
            );
          }
        }

        return new SingularityTaskIdHistory(
          taskId,
          r.getTimestamp("updatedAt").getTime(),
          lastTaskState,
          Optional.ofNullable(r.getString("runId"))
        );
      } catch (SingularityTranscoderException e) {
        throw new ResultSetException("Could not deserialize database result", e, ctx);
      }
    }
  }

  static class SingularityDeployHistoryLiteMapper
    implements RowMapper<SingularityDeployHistory> {
    private final String userColumn;

    @Inject
    SingularityDeployHistoryLiteMapper(
      SingularityConfiguration singularityConfiguration
    ) {
      this.userColumn = getUserColumn(singularityConfiguration);
    }

    @Override
    public SingularityDeployHistory map(ResultSet r, StatementContext ctx)
      throws SQLException {
      SingularityDeployMarker marker = new SingularityDeployMarker(
        r.getString("requestId"),
        r.getString("deployId"),
        r.getTimestamp("createdAt").getTime(),
        Optional.ofNullable(r.getString(userColumn)),
        Optional.ofNullable(r.getString("message"))
      );
      SingularityDeployResult deployState = new SingularityDeployResult(
        DeployState.valueOf(r.getString("deployState")),
        Optional.empty(),
        Collections.emptyList(),
        r.getTimestamp("deployStateAt").getTime()
      );

      return new SingularityDeployHistory(
        Optional.of(deployState),
        marker,
        Optional.empty(),
        Optional.empty()
      );
    }
  }

  static class SingularityRequestIdCountMapper
    implements RowMapper<SingularityRequestIdCount> {

    @Inject
    SingularityRequestIdCountMapper() {}

    @Override
    public SingularityRequestIdCount map(ResultSet r, StatementContext ctx)
      throws SQLException {
      return new SingularityRequestIdCount(r.getString("requestId"), r.getInt("count"));
    }
  }

  static class SingularityTaskUsageMapper implements RowMapper<SingularityTaskUsage> {

    @Inject
    SingularityTaskUsageMapper() {}

    @Override
    public SingularityTaskUsage map(ResultSet r, StatementContext ctx)
      throws SQLException {
      return new SingularityTaskUsage(
        r.getLong("memoryTotalBytes"),
        r.getLong("timestamp"),
        r.getDouble("cpuSeconds"),
        r.getLong("diskTotalBytes"),
        r.getLong("cpusNrPeriods"),
        r.getLong("cpusNrThrottled"),
        r.getDouble("cpusThrottledTimeSecs")
      );
    }
  }

  public static class SingularityRequestIdCount {
    private final int count;
    private final String requestId;

    public SingularityRequestIdCount(String requestId, int count) {
      this.requestId = requestId;
      this.count = count;
    }

    public int getCount() {
      return count;
    }

    public String getRequestId() {
      return requestId;
    }

    @Override
    public String toString() {
      return (
        "SingularityRequestIdCount [count=" + count + ", requestId=" + requestId + "]"
      );
    }
  }

  // In Postgres "user" is a reserved word - hence we cannot use it.
  static String getUserColumn(SingularityConfiguration singularityConfiguration) {
    return SingularityDbModule.isPostgres(
        singularityConfiguration.getDatabaseConfiguration()
      )
      ? "f_user"
      : "user";
  }
}
