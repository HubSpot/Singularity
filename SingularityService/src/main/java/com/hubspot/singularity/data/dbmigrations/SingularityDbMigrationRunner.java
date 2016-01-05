package com.hubspot.singularity.data.dbmigrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.db.DatabaseConfiguration;
import io.dropwizard.migrations.CloseableLiquibase;

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

  public void checkMigrations() {
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

    try (final CloseableLiquibase liquibase = new CloseableLiquibase(dataSourceFactory.build(metricRegistry, "liquibase"))) {
      final long start = System.currentTimeMillis();
      LOG.info("Starting db migration...");
      liquibase.update("");
      LOG.info("Ran db migration in {}", JavaUtils.duration(start));
    } catch (Exception e) {
      LOG.error("Caught exception while running database migration", e);
    }
  }

  private static class DatabaseConfigurationHolder implements DatabaseConfiguration<SingularityConfiguration> {
    private final SingularityConfiguration configuration;

    public DatabaseConfigurationHolder(SingularityConfiguration configuration) {
      this.configuration = configuration;
    }

    @Override
    public DataSourceFactory getDataSourceFactory(SingularityConfiguration configuration) {
      return null;
    }
  }
}
