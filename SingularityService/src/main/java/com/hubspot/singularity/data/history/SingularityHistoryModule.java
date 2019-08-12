package com.hubspot.singularity.data.history;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.jackson2.Jackson2Config;
import org.jdbi.v3.jackson2.Jackson2Plugin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityIdMapper;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityJsonStringMapper;
import com.hubspot.singularity.data.usage.JDBITaskUsageManager;
import com.hubspot.singularity.data.usage.MySQLTaskUsageJDBI;
import com.hubspot.singularity.data.usage.PostgresTaskUsageJDBI;
import com.hubspot.singularity.data.usage.TaskUsageJDBI;
import com.hubspot.singularity.data.usage.TaskUsageManager;
import com.hubspot.singularity.data.usage.ZkTaskUsageManager;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;

public class SingularityHistoryModule extends AbstractModule {
  public static final String PERSISTER_LOCK = "history.persister.lock";

  private final Optional<DataSourceFactory> configuration;

  public SingularityHistoryModule(SingularityConfiguration configuration) {
    checkNotNull(configuration, "configuration is null");
    this.configuration = configuration.getDatabaseConfiguration();
  }

  @Override
  public void configure() {
    Multibinder<RowMapper<?>> rowMappers = Multibinder.newSetBinder(binder(), new TypeLiteral<RowMapper<?>>() {});
    rowMappers.addBinding().to(SingularityMappers.SingularityRequestHistoryMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityTaskIdHistoryMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityDeployHistoryLiteMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityRequestIdCountMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityTaskUsageMapper.class).in(Scopes.SINGLETON);
    rowMappers.addBinding().to(SingularityMappers.SingularityRequestWithTimeMapper.class).in(Scopes.SINGLETON);

    Multibinder<ColumnMapper<?>> columnMappers = Multibinder.newSetBinder(binder(), new TypeLiteral<ColumnMapper<?>>() {});
    columnMappers.addBinding().to(SingularityMappers.SingularityBytesMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityIdMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityJsonStringMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityMappers.DateMapper.class).in(Scopes.SINGLETON);
    columnMappers.addBinding().to(SingularityMappers.SingularityTimestampMapper.class).in(Scopes.SINGLETON);

    bind(TaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(RequestHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployTaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(SingularityRequestHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryPersister.class).in(Scopes.SINGLETON);

    // Setup database support
    if (configuration.isPresent()) {
      bind(Jdbi.class).toProvider(DBIProvider.class).in(Scopes.SINGLETON);
      bindSpecificDatabase();
      bind(HistoryManager.class).to(JDBIHistoryManager.class).in(Scopes.SINGLETON);
      bind(TaskUsageManager.class).to(JDBITaskUsageManager.class).in(Scopes.SINGLETON);
    } else {
      bind(HistoryManager.class).to(NoopHistoryManager.class).in(Scopes.SINGLETON);
      bind(TaskUsageManager.class).to(ZkTaskUsageManager.class).in(Scopes.SINGLETON);
    }
  }

  private void bindSpecificDatabase() {
    if (isPostgres(configuration)) {
      bind(HistoryJDBI.class).toProvider(PostgresHistoryJDBIProvider.class).in(Scopes.SINGLETON);
      bind(TaskUsageJDBI.class).toProvider(PostgresTaskUsageJDBIProvider.class).in(Scopes.SINGLETON);
      // Currently many unit tests use h2
    } else if (isMySQL(configuration) || isH2(configuration)) {
      bind(HistoryJDBI.class).toProvider(MySQLHistoryJDBIProvider.class).in(Scopes.SINGLETON);
      bind(TaskUsageJDBI.class).toProvider(MySQLTaskUsageJDBIProvider.class).in(Scopes.SINGLETON);
    } else {
      throw new IllegalStateException("Unknown driver class present " + configuration.get().getDriverClass());
    }
  }

  @Provides
  @Singleton
  @Named(PERSISTER_LOCK)
  public ReentrantLock providePersisterLock() {
    return new ReentrantLock();
  }

  static class DBIProvider implements Provider<Jdbi> {
    private final JdbiFactory factory = new JdbiFactory();
    private final Environment environment;
    private final DataSourceFactory dataSourceFactory;

    private Set<RowMapper<?>> rowMappers = ImmutableSet.of();
    private Set<ColumnMapper<?>> columnMappers = ImmutableSet.of();
    private ObjectMapper objectMapper;

    @Inject
    DBIProvider(final Environment environment, final SingularityConfiguration singularityConfiguration) throws ClassNotFoundException {
      this.environment = environment;
      this.dataSourceFactory = checkNotNull(singularityConfiguration, "singularityConfiguration is null").getDatabaseConfiguration().get();
    }

    @Inject(optional = true)
    void setMappers(Set<RowMapper<?>> rowMappers, Set<ColumnMapper<?>> columnMappers, ObjectMapper objectMapper) {
      checkNotNull(rowMappers, "resultSetMappers is null");
      this.rowMappers = ImmutableSet.copyOf(rowMappers);
      this.columnMappers = ImmutableSet.copyOf(columnMappers);
      this.objectMapper = objectMapper;
    }

    @Override
    public Jdbi get() {
      try {
        Jdbi jdbi = factory.build(environment, dataSourceFactory, "db");
        for (RowMapper<?> resultSetMapper : rowMappers) {
          jdbi.registerRowMapper(resultSetMapper);
        }
        for (ColumnMapper<?> resultSetMapper : columnMappers) {
          jdbi.registerColumnMapper(resultSetMapper);
        }
        jdbi.installPlugin(new Jackson2Plugin());
        jdbi.getConfig(Jackson2Config.class).setMapper(objectMapper);

        return jdbi;
      } catch (Exception e) {
        throw new ProvisionException("while instantiating DBI", e);
      }
    }
  }

  static class MySQLHistoryJDBIProvider implements Provider<HistoryJDBI> {
    private final Jdbi dbi;

    @Inject
    public MySQLHistoryJDBIProvider(Jdbi dbi) {
      this.dbi = dbi;
    }

    @Override
    public MySQLHistoryJDBI get() {
      return dbi.onDemand(MySQLHistoryJDBI.class);
    }

  }

  static class PostgresHistoryJDBIProvider implements Provider<HistoryJDBI> {
    private final Jdbi dbi;

    @Inject
    public PostgresHistoryJDBIProvider(Jdbi dbi) {
      this.dbi = dbi;
    }

    @Override
    public PostgresHistoryJDBI get() {
      return dbi.onDemand(PostgresHistoryJDBI.class);
    }

  }

  static class MySQLTaskUsageJDBIProvider implements Provider<TaskUsageJDBI> {
    private final Jdbi dbi;

    @Inject
    public MySQLTaskUsageJDBIProvider(Jdbi dbi) {
      this.dbi = dbi;
    }

    @Override
    public MySQLTaskUsageJDBI get() {
      return dbi.onDemand(MySQLTaskUsageJDBI.class);
    }

  }

  static class PostgresTaskUsageJDBIProvider implements Provider<TaskUsageJDBI> {
    private final Jdbi dbi;

    @Inject
    public PostgresTaskUsageJDBIProvider(Jdbi dbi) {
      this.dbi = dbi;
    }

    @Override
    public PostgresTaskUsageJDBI get() {
      return dbi.onDemand(PostgresTaskUsageJDBI.class);
    }

  }

  // Convenience methods for determining which database is configured
  static boolean isH2(Optional<DataSourceFactory> dataSourceFactoryOptional) {
    return driverConfigured(dataSourceFactoryOptional, "org.h2.Driver");
  }

  static boolean isMySQL(Optional<DataSourceFactory> dataSourceFactoryOptional) {
    return driverConfigured(dataSourceFactoryOptional, "com.mysql.jdbc.Driver");
  }

  static boolean isPostgres(Optional<DataSourceFactory> dataSourceFactoryOptional) {
          return driverConfigured(dataSourceFactoryOptional, "org.postgresql.Driver");
  }

  private static boolean driverConfigured(Optional<DataSourceFactory> dataSourceFactoryOptional, String jdbcDriverclass) {
    return dataSourceFactoryOptional != null && dataSourceFactoryOptional.isPresent() &&
            jdbcDriverclass.equals(dataSourceFactoryOptional.get().getDriverClass());
  }
}
