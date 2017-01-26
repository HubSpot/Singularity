package com.hubspot.singularity.athena;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.athena.jdbc.AthenaConnection;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class JDBCAthenaConnectionProvider implements AthenaConnectionProvider {
  private static final Logger LOG = LoggerFactory.getLogger(JDBCAthenaConnectionProvider.class);

  private final Properties athenaDriverProperties;
  private final String athenaUrl;

  @Inject
  public JDBCAthenaConnectionProvider(@Named(AthenaModule.ATHENA_DRIVER_PROPERTIES) Properties athenaDriverProperties,
                                      @Named(AthenaModule.ATHENA_CONNECTION_URL) String athenaUrl) {
    this.athenaDriverProperties = athenaDriverProperties;
    this.athenaUrl = athenaUrl;
  }

  public AthenaConnection getAthenaConnection() throws SQLException {
    return (AthenaConnection) DriverManager.getConnection(athenaUrl, athenaDriverProperties);
  }
}
