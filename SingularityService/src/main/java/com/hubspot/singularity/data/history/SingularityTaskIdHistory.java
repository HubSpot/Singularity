package com.hubspot.singularity.data.history;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityTaskIdHistory {

  private final SingularityTaskId taskId;
  private final Optional<String> lastStatus;
  private final long createdAt;
  private final Optional<Long> updatedAt;

  public SingularityTaskIdHistory(@JsonProperty("taskId") SingularityTaskId taskId, @JsonProperty("lastStatus") Optional<String> lastStatus,
      @JsonProperty("createdAt") long createdAt, @JsonProperty("updatedAt") Optional<Long> updatedAt) {
    this.taskId = taskId;
    this.lastStatus = lastStatus;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public SingularityTaskId getTaskDetails() {
    return taskId;
  }

  public Optional<String> getLastStatus() {
    return lastStatus;
  }
  
  public Optional<Long> getUpdatedAt() {
    return updatedAt;
  }
  
  public long getCreatedAt() {
    return createdAt;
  }

  public String getTaskId() {
    return taskId.toString();
  }
  
  public static class SingularityTaskIdHistoryMapper implements ResultSetMapper<SingularityTaskIdHistory> {

    public SingularityTaskIdHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      Date updatedAt = r.getDate("updatedAt");
      Optional<Long> maybeUpdatedAt = null;
      if (updatedAt == null) {
        maybeUpdatedAt = Optional.absent();
      } else {
        maybeUpdatedAt = Optional.of(updatedAt.getTime());
      }
      return new SingularityTaskIdHistory(SingularityTaskId.fromString(r.getString("taskId")), Optional.fromNullable(r.getString("lastStatus")), r.getDate("createdAt").getTime(), maybeUpdatedAt);
    }
  }
  
}
