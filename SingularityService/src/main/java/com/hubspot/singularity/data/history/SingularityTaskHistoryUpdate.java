package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;

public class SingularityTaskHistoryUpdate {

  private final long timestamp;
  private final String statusUpdate;
  private final Optional<String> statusMessage;

  public SingularityTaskHistoryUpdate(long timestamp, String statusUpdate, Optional<String> statusMessage) {
    this.timestamp = timestamp;
    this.statusUpdate = statusUpdate;
    this.statusMessage = statusMessage;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String getStatusUpdate() {
    return statusUpdate;
  }

  public Optional<String> getStatusMessage() {
    return statusMessage;
  }

  @Override
  public String toString() {
    return "SingularityTaskUpdate [timestamp=" + timestamp + ", statusUpdate=" + statusUpdate + ", statusMessage=" + statusMessage + "]";
  }

  public static class SingularityTaskUpdateMapper implements ResultSetMapper<SingularityTaskHistoryUpdate> {
    
    public SingularityTaskHistoryUpdate map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      return new SingularityTaskHistoryUpdate(r.getDate("createdAt").getTime(), r.getString("status"), Optional.fromNullable(r.getString("message")));
    }

  }

}
