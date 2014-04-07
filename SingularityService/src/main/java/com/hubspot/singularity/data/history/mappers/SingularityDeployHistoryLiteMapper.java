package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployStatistics;

public class SingularityDeployHistoryLiteMapper implements ResultSetMapper<SingularityDeployHistory> {

  public SingularityDeployHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    SingularityDeployMarker marker = new SingularityDeployMarker(r.getString("requestId"), r.getString("deployId"), r.getTimestamp("createdAt").getTime(), Optional.fromNullable(r.getString("user")));
    
    return new SingularityDeployHistory(Optional.of(DeployState.valueOf(r.getString("deployState"))), marker, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeployStatistics> absent());
  }
  
}
