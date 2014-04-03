package com.hubspot.singularity.data.history.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public class SingularityTaskHistoryMapper implements ResultSetMapper<byte[]> {
  
  public byte[] map(int index, ResultSet r, StatementContext ctx) throws SQLException {
    return r.getBytes("taskHistory");
  }

}