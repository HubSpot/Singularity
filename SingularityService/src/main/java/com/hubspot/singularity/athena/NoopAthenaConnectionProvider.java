package com.hubspot.singularity.athena;

import java.sql.SQLException;

import com.amazonaws.athena.jdbc.AthenaConnection;
import com.google.inject.Inject;

public class NoopAthenaConnectionProvider implements AthenaConnectionProvider {

  @Inject
  public NoopAthenaConnectionProvider() {}

  public AthenaConnection getAthenaConnection() throws SQLException {
    return null;
  }
}
