package com.hubspot.singularity.data.history;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Throwables;
import com.hubspot.singularity.SingularityModule;
import com.hubspot.singularity.SingularityRequest;

public class SingularityRequestHistory {

  private final long updatedAt;
  private final long createdAt;
  private final SingularityRequest request;

  public SingularityRequestHistory(long updatedAt, long createdAt, SingularityRequest request) {
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
    this.request = request;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public SingularityRequest getRequest() {
    return request;
  }
  
  public static class SingularityRequestHistoryMapper implements ResultSetMapper<SingularityRequestHistory> {
    
    public SingularityRequestHistory map(int index, ResultSet r, StatementContext ctx) throws SQLException {
      try {
        return new SingularityRequestHistory(r.getDate("updatedAt").getTime(), r.getDate("createdAt").getTime(), SingularityRequest.getRequestFromData(r.getBytes("request"), SingularityModule.OBJECT_MAPPER));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    
  }
  
}
