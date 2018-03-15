package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import javax.inject.Inject;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.singularity.api.deploy.DeployState;
import com.hubspot.singularity.api.deploy.SingularityDeployFailure;
import com.hubspot.singularity.api.deploy.SingularityDeployHistory;
import com.hubspot.singularity.api.deploy.SingularityDeployMarker;
import com.hubspot.singularity.api.deploy.SingularityDeployResult;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityRequestHistory;
import com.hubspot.singularity.api.request.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.api.task.ExtendedTaskState;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskIdHistory;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class SingularityMappers {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMappers.class);

  static class SingularityBytesMapper implements ResultSetMapper<byte[]> {

    @Inject
    SingularityBytesMapper() {}

    @Override
    public byte[] map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return r.getBytes("bytes");
    }

  }

  static class DateMapper implements ResultSetMapper<Date> {

    @Inject
    DateMapper() {}

    @Override
    public Date map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new Date(r.getTimestamp(1).getTime());
    }

  }

  static class SingularityRequestIdMapper implements ResultSetMapper<String> {

    @Inject
    SingularityRequestIdMapper() {}

    @Override
    public String map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return r.getString("requestId");
    }

  }

  static class SingularityRequestHistoryMapper implements ResultSetMapper<SingularityRequestHistory> {
    private final Transcoder<SingularityRequest> singularityRequestTranscoder;

    @Inject
    SingularityRequestHistoryMapper(Transcoder<SingularityRequest> singularityRequestTranscoder) {
      this.singularityRequestTranscoder = singularityRequestTranscoder;
    }

    @Override
    public SingularityRequestHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      try {
        return new SingularityRequestHistory(r.getTimestamp("createdAt").getTime(), Optional.ofNullable(r.getString("user")), RequestHistoryType.valueOf(r.getString("requestState")),
            singularityRequestTranscoder.fromBytes(r.getBytes("request")), Optional.ofNullable(r.getString("message")));
      } catch (SingularityTranscoderException e) {
        throw new ResultSetException("Could not deserialize database result", e, ctx);
      }
    }
  }

  static class SingularityTaskIdHistoryMapper implements ResultSetMapper<SingularityTaskIdHistory> {

    private final IdTranscoder<SingularityTaskId> singularityTaskIdTranscoder;

    @Inject
    SingularityTaskIdHistoryMapper(final IdTranscoder<SingularityTaskId> singularityTaskIdTranscoder) {
      this.singularityTaskIdTranscoder = singularityTaskIdTranscoder;
    }

    @Override
    public SingularityTaskIdHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      try {
        final SingularityTaskId taskId = singularityTaskIdTranscoder.fromString(r.getString("taskId"));

        final String lastTaskStatus = r.getString("lastTaskStatus");

        Optional<ExtendedTaskState> lastTaskState = Optional.empty();

        if (lastTaskStatus != null) {
          try {
            lastTaskState = Optional.of(ExtendedTaskState.valueOf(lastTaskStatus));
          } catch (IllegalArgumentException e) {
            LOG.warn("Found invalid taskState {} in DB for task {}", lastTaskState, taskId, e);
          }
        }

        return new SingularityTaskIdHistory(taskId, r.getTimestamp("updatedAt").getTime(), lastTaskState, Optional.ofNullable(r.getString("runId")));
      } catch (SingularityTranscoderException e) {
        throw new ResultSetException("Could not deserialize database result", e, ctx);
      }
    }
  }

  static class SingularityDeployHistoryLiteMapper implements ResultSetMapper<SingularityDeployHistory> {

    @Inject
    SingularityDeployHistoryLiteMapper() {}

    @Override
    public SingularityDeployHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      SingularityDeployMarker marker =
          new SingularityDeployMarker(r.getString("requestId"), r.getString("deployId"), r.getTimestamp("createdAt").getTime(), Optional.ofNullable(r.getString("user")),
              Optional.ofNullable(r.getString("message")));
      SingularityDeployResult deployState =
          new SingularityDeployResult(DeployState.valueOf(r.getString("deployState")), Optional.empty(), Optional.empty(), Collections.<SingularityDeployFailure>emptyList(), r.getTimestamp("deployStateAt")
              .getTime());

      return new SingularityDeployHistory(Optional.of(deployState), marker, Optional.empty(), Optional.empty());
    }
  }

  static class SingularityRequestIdCountMapper implements ResultSetMapper<SingularityRequestIdCount> {

    @Inject
    SingularityRequestIdCountMapper() {}

    @Override
    public SingularityRequestIdCount map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new SingularityRequestIdCount(r.getString("requestId"), r.getInt("count"));
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
      return "SingularityRequestIdCount [count=" + count + ", requestId=" + requestId + "]";
    }

  }

}
