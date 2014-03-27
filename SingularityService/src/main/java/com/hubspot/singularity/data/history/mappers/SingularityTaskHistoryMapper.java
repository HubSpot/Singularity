package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Throwables;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.SingularityTaskHistory;

public class SingularityTaskHistoryMapper implements ResultSetMapper<SingularityTaskHistory> {
  
  public SingularityTaskHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    try {
      return SingularityTaskHistory.fromBytes(r.getBytes("taskHistory"), SingularityModule.OBJECT_MAPPER);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}