package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityServiceModule;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;

public class SingularityMappers {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMappers.class);

  public static class SingularityBytesMapper implements ResultSetMapper<byte[]> {

    @Override
    public byte[] map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return r.getBytes("bytes");
    }

  }

  public static class SingularityRequestIdMapper implements ResultSetMapper<String> {

    @Override
    public String map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return r.getString("requestId");
    }

  }

  public static class SingularityRequestHistoryMapper implements ResultSetMapper<SingularityRequestHistory> {

    @Override
    public SingularityRequestHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new SingularityRequestHistory(r.getTimestamp("createdAt").getTime(), Optional.fromNullable(r.getString("user")), RequestHistoryType.valueOf(r.getString("requestState")), SingularityRequest.fromBytes(r.getBytes("request"),
          SingularityServiceModule.OBJECT_MAPPER));
    }

  }

  public static class SingularityTaskIdHistoryMapper implements ResultSetMapper<SingularityTaskIdHistory> {

    @Override
    public SingularityTaskIdHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      final SingularityTaskId taskId = SingularityTaskId.fromString(r.getString("taskId"));
      final String lastTaskStatus = r.getString("lastTaskStatus");

      Optional<ExtendedTaskState> lastTaskState = Optional.absent();

      if (lastTaskStatus != null) {
        try {
          lastTaskState = Optional.of(ExtendedTaskState.valueOf(lastTaskStatus));
        } catch (IllegalArgumentException e) {
          LOG.warn("Found invalid taskState {} in DB for task {}", lastTaskState, taskId, e);
        }
      }

      return new SingularityTaskIdHistory(taskId, r.getTimestamp("updatedAt").getTime(), lastTaskState);
    }

  }

  public static class SingularityDeployHistoryLiteMapper implements ResultSetMapper<SingularityDeployHistory> {

    @Override
    public SingularityDeployHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      SingularityDeployMarker marker = new SingularityDeployMarker(r.getString("requestId"), r.getString("deployId"), r.getTimestamp("createdAt").getTime(), Optional.fromNullable(r.getString("user")));
      SingularityDeployResult deployState = new SingularityDeployResult(DeployState.valueOf(r.getString("deployState")), Optional.<String> absent(), Optional.<SingularityLoadBalancerUpdate> absent(), r.getTimestamp("deployStateAt").getTime());

      return new SingularityDeployHistory(Optional.of(deployState), marker, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeployStatistics> absent());
    }

  }

}
