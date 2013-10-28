package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class SingularityRequestHistory {

  private final long timestamp;
  private final byte[] taskData;
  
  public SingularityRequestHistory(long timestamp, byte[] taskData) {
    this.timestamp = timestamp;
    this.taskData = taskData;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public byte[] getTaskData() {
    return taskData;
  }

  public class SingularityTaskHistoryHelperMapper implements ResultSetMapper<SingularityRequestHistory> {
    public SingularityRequestHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new SingularityRequestHistory(r.getDate("createdAt").getTime(), r.getBytes("task"));
    }
  }

}
