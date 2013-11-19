package com.hubspot.singularity.data.history.mappers;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;

public class SingularityTaskIdHistoryMapper implements ResultSetMapper<SingularityTaskIdHistory> {

  public SingularityTaskIdHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    Date updatedAt = r.getDate("updatedAt");
    
    Optional<Long> maybeUpdatedAt = null;
    if (updatedAt == null) {
      maybeUpdatedAt = Optional.absent();
    } else {
      maybeUpdatedAt = Optional.of(updatedAt.getTime());
    }
    
    return new SingularityTaskIdHistory(SingularityTaskId.fromString(r.getString("taskId")), Optional.fromNullable(r.getString("lastTaskStatus")), r.getDate("createdAt").getTime(), maybeUpdatedAt);
  }
  
}