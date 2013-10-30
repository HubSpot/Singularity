package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;

public class SingularityTaskHistory {

  private final List<SingularityTaskHistoryUpdate> taskUpdates;
  private final long timestamp;
  private final SingularityTask task;

  public SingularityTaskHistory(List<SingularityTaskHistoryUpdate> taskUpdates, long timestamp, SingularityTask task) {
    this.taskUpdates = taskUpdates;
    this.timestamp = timestamp;
    this.task = task;
  }

  public List<SingularityTaskHistoryUpdate> getTaskUpdates() {
    return taskUpdates;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public SingularityTask getTask() {
    return task;
  }
  
  public class SingularityTaskIdMapper implements ResultSetMapper<SingularityPendingTaskId> {
    public SingularityPendingTaskId map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return SingularityPendingTaskId.fromString(r.getString("taskId"));
    }
  }
  
  

}
