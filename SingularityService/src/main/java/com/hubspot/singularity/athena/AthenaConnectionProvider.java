package com.hubspot.singularity.athena;

import java.sql.SQLException;

import com.amazonaws.athena.jdbc.AthenaConnection;

public interface AthenaConnectionProvider {
  public AthenaConnection getAthenaConnection() throws SQLException;
}
