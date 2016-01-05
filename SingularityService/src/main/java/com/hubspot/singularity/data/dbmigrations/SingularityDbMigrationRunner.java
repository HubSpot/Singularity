package com.hubspot.singularity.data.dbmigrations;

import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.ManagedDataSource;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

@Singleton
public class SingularityDbMigrationRunner {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityDbMigrationRunner.class);

  private final SingularityConfiguration configuration;
  private final MetricRegistry metricRegistry;

  @Inject
  public SingularityDbMigrationRunner(SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    this.configuration = configuration;
    this.metricRegistry = metricRegistry;
  }

  public void checkMigrations() throws Exception {
    final DataSourceFactory dataSourceFactory;

    if (configuration.getDatabaseMigrationConfiguration().isPresent()) {
      LOG.info("Using databaseMigration configuration for migration.");
      dataSourceFactory = configuration.getDatabaseMigrationConfiguration().get();
    } else if (configuration.getDatabaseConfiguration().isPresent()) {
      LOG.info("Using database configuration for migration.");
      dataSourceFactory = configuration.getDatabaseConfiguration().get();
    } else {
      LOG.info("Database not configured, skipping migration.");
      return;
    }

    final ManagedDataSource managedDataSource = dataSourceFactory.build(metricRegistry, "liquibase");

    try (final ClosableLiquibaseFromResource liquibase = new ClosableLiquibaseFromResource(managedDataSource, "pre-migrations.xml")) {
      final long start = System.currentTimeMillis();
      LOG.info("Starting db pre-migration...");
      liquibase.update("");
      LOG.info("Ran db pre-migration in {}", JavaUtils.duration(start));
    }

    try (final ClosableLiquibaseFromResource liquibase = new ClosableLiquibaseFromResource(managedDataSource, "migrations.xml")) {
      final long start = System.currentTimeMillis();
      LOG.info("Starting db migration...");
      liquibase.update("");
      LOG.info("Ran db migration in {}", JavaUtils.duration(start));
    }
  }

  private static class ClosableLiquibaseFromResource extends Liquibase implements AutoCloseable {
    private final ManagedDataSource dataSource;

    public ClosableLiquibaseFromResource(ManagedDataSource dataSource, String filename) throws LiquibaseException, ClassNotFoundException, SQLException {
      super(filename,
          new ClassLoaderResourceAccessor(),
          new JdbcConnection(dataSource.getConnection()));
      this.dataSource = dataSource;
    }

    @Override
    public void close() throws Exception {
      dataSource.stop();
    }
  }
}
