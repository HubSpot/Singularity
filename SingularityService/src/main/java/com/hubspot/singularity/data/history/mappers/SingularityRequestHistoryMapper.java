package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;

public class SingularityRequestHistoryMapper implements ResultSetMapper<SingularityRequestHistory> {

  public SingularityRequestHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    try {
      return new SingularityRequestHistory(r.getTimestamp("createdAt").getTime(), Optional.fromNullable(r.getString("user")), RequestState.valueOf(r.getString("requestState")), SingularityRequest.fromBytes(r.getBytes("request"),
          SingularityModule.OBJECT_MAPPER));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
