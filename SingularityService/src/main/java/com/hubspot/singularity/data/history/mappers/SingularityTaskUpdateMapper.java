package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityTaskUpdateMapper implements ResultSetMapper<SingularityTaskHistoryUpdate> {
  
  // TODO set task id in the statment cntx.
  public SingularityTaskHistoryUpdate map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return new SingularityTaskHistoryUpdate(SingularityTaskId.fromString((String) ctx.getAttribute("taskd")), r.getTimestamp("createdAt").getTime(), r.getString("status"), Optional.fromNullable(r.getString("message")));
  }

}