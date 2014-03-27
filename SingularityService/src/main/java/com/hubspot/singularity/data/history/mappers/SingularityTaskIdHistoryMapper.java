package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;

public class SingularityTaskIdHistoryMapper implements ResultSetMapper<SingularityTaskIdHistory> {

  public SingularityTaskIdHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return new SingularityTaskIdHistory(SingularityTaskId.fromString(r.getString("taskId")), r.getTimestamp("updatedAt").getTime(), Optional.fromNullable(r.getString("lastTaskStatus")));
  }
  
}
