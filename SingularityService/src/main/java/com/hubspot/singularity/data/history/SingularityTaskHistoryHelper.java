package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;

public class SingularityTaskHistoryHelper {

  private final long timestamp;
  private final Optional<String> directory;
  private final byte[] taskData;
  
  public SingularityTaskHistoryHelper(long timestamp, byte[] taskData, Optional<String> directory) {
    this.timestamp = timestamp;
    this.taskData = taskData;
    this.directory = directory;
  }
  
  public Optional<String> getDirectory() {
    return directory;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public byte[] getTaskData() {
    return taskData;
  }

  public static class SingularityTaskHistoryHelperMapper implements ResultSetMapper<SingularityTaskHistoryHelper> {
    
    public SingularityTaskHistoryHelper map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new SingularityTaskHistoryHelper(r.getTimestamp("createdAt").getTime(), r.getBytes("task"), Optional.fromNullable(r.getString("directory")));
    }
    
  }

}
