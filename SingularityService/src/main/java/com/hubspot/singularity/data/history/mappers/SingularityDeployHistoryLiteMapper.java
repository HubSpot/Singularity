package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeployHistoryLite;

public class SingularityDeployHistoryLiteMapper implements ResultSetMapper<SingularityDeployHistoryLite> {

  public SingularityDeployHistoryLite map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return new SingularityDeployHistoryLite(r.getString("requestId"), r.getString("deployId"), r.getTimestamp("createdAt").getTime(), Optional.fromNullable(r.getString("user")), Optional.of(DeployState.valueOf(r.getString("deployState"))));
  }
  
}
