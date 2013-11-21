package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class SingularityTaskHistoryHelper {

  private final long timestamp;
  private final byte[] taskData;
  
  public SingularityTaskHistoryHelper(long timestamp, byte[] taskData) {
    this.timestamp = timestamp;
    this.taskData = taskData;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public byte[] getTaskData() {
    return taskData;
  }

  public static class SingularityTaskHistoryHelperMapper implements ResultSetMapper<SingularityTaskHistoryHelper> {
    
    public SingularityTaskHistoryHelper map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new SingularityTaskHistoryHelper(r.getTimestamp("createdAt").getTime(), r.getBytes("task"));
    }
    
  }

}
