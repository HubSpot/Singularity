package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.hubspot.singularity.SingularityTaskId;

public class SingularityTaskIdMapper implements ResultSetMapper<SingularityTaskId> {

  public SingularityTaskId map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return SingularityTaskId.fromString(r.getString("taskId"));
  }

}