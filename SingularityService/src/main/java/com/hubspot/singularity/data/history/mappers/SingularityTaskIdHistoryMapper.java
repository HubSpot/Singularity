package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.mesos.Protos.TaskState;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;

public class SingularityTaskIdHistoryMapper implements ResultSetMapper<SingularityTaskIdHistory> {

  public SingularityTaskIdHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    String lastTaskStatus = r.getString("lastTaskStatus");
    
    Optional<TaskState> lastTaskState = Optional.absent();
    
    if (lastTaskStatus != null) {
      lastTaskState = Optional.of(TaskState.valueOf(lastTaskStatus));
    }
    
    return new SingularityTaskIdHistory(SingularityTaskId.fromString(r.getString("taskId")), r.getTimestamp("updatedAt").getTime(), lastTaskState);
  }
  
}
