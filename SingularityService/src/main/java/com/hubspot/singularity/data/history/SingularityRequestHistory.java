package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.SingularityRequest;

public class SingularityRequestHistory {

  private final long createdAt;
  private final Optional<String> user;
  private final RequestState state;
  private final SingularityRequest request;
  
  public enum RequestState {
    CREATED, UPDATED, DELETED;
  }
  
  public SingularityRequestHistory(long createdAt, Optional<String> user, RequestState state, SingularityRequest request) {
    this.createdAt = createdAt;
    this.user = user;
    this.state = state;
    this.request = request;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public Optional<String> getUser() {
    return user;
  }

  public String getState() {
    return state.name();
  }
  
  public SingularityRequest getRequest() {
    return request;
  }

  public static class SingularityRequestHistoryMapper implements ResultSetMapper<SingularityRequestHistory> {
    
    public SingularityRequestHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      try {
        return new SingularityRequestHistory(r.getDate("createdAt").getTime(), Optional.fromNullable(r.getString("user")), RequestState.valueOf(r.getString("requestState")), SingularityRequest.getRequestFromData(r.getBytes("request"), SingularityModule.OBJECT_MAPPER));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    
  }
  
}
