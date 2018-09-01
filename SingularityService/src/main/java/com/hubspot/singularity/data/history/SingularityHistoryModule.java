package com.hubspot.singularity.data.history;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;

public class SingularityHistoryModule extends AbstractModule {
  public static final String PERSISTER_LOCK = "hsitory.persister.lock";

  private final Optional<DataSourceFactory> configuration;

  public SingularityHistoryModule(SingularityConfiguration configuration) {
    checkNotNull(configuration, "configuration is null");
    this.configuration = configuration.getDatabaseConfiguration();
  }

  @Override
  public void configure() {
    Multibinder<ResultSetMapper<?>> resultSetMappers = Multibinder.newSetBinder(binder(), new TypeLiteral<ResultSetMapper<?>>() {});

    resultSetMappers.addBinding().to(SingularityMappers.SingularityBytesMapper.class).in(Scopes.SINGLETON);
    resultSetMappers.addBinding().to(SingularityMappers.SingularityRequestIdMapper.class).in(Scopes.SINGLETON);
    resultSetMappers.addBinding().to(SingularityMappers.SingularityRequestHistoryMapper.class).in(Scopes.SINGLETON);
    resultSetMappers.addBinding().to(SingularityMappers.SingularityTaskIdHistoryMapper.class).in(Scopes.SINGLETON);
    resultSetMappers.addBinding().to(SingularityMappers.SingularityDeployHistoryLiteMapper.class).in(Scopes.SINGLETON);
    resultSetMappers.addBinding().to(SingularityMappers.SingularityRequestIdCountMapper.class).in(Scopes.SINGLETON);
    resultSetMappers.addBinding().to(SingularityMappers.DateMapper.class).in(Scopes.SINGLETON);

    bind(TaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(RequestHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployTaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(SingularityRequestHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryPersister.class).in(Scopes.SINGLETON);

    // Setup database support
    if (configuration.isPresent()) {
      bind(DBI.class).toProvider(DBIProvider.class).in(Scopes.SINGLETON);
      bindSpecificDatabase();
      bind(HistoryManager.class).to(JDBIHistoryManager.class).in(Scopes.SINGLETON);
      bindMethodInterceptorForStringTemplateClassLoaderWorkaround();
    } else {
      bind(HistoryManager.class).to(NoopHistoryManager.class).in(Scopes.SINGLETON);
    }
  }

  private void bindSpecificDatabase() {
    if (isPostgres(configuration)) {
      bind(HistoryJDBI.class).toProvider(PostgresHistoryJDBIProvider.class).in(Scopes.SINGLETON);
      // Currently many unit tests use h2
    } else if (isMySQL(configuration) || isH2(configuration)) {
      bind(HistoryJDBI.class).toProvider(MySQLHistoryJDBIProvider.class).in(Scopes.SINGLETON);
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

  private void bindMethodInterceptorForStringTemplateClassLoaderWorkaround() {
    bindInterceptor(Matchers.subclassesOf(JDBIHistoryManager.class), Matchers.any(), new MethodInterceptor() {

      @Override
      public Object invoke(MethodInvocation invocation) throws Throwable {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (cl == null) {
          Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }

        try {
          return invocation.proceed();
        } finally {
          Thread.currentThread().setContextClassLoader(cl);
        }
      }
    });
  }

  static class DBIProvider implements Provider<DBI> {
    private final DBIFactory dbiFactory = new DBIFactory();
    private final Environment environment;
    private final DataSourceFactory dataSourceFactory;

    private Set<ResultSetMapper<?>> resultSetMappers = ImmutableSet.of();

    @Inject
    DBIProvider(final Environment environment, final SingularityConfiguration singularityConfiguration) throws ClassNotFoundException {
      this.environment = environment;
      this.dataSourceFactory = checkNotNull(singularityConfiguration, "singularityConfiguration is null").getDatabaseConfiguration().get();
    }

    @Inject(optional = true)
    void setMappers(Set<ResultSetMapper<?>> resultSetMappers) {
      checkNotNull(resultSetMappers, "resultSetMappers is null");
      this.resultSetMappers = ImmutableSet.copyOf(resultSetMappers);
    }

    @Override
    public DBI get() {
      try {
        DBI dbi = dbiFactory.build(environment, dataSourceFactory, "db");
        for (ResultSetMapper<?> resultSetMapper : resultSetMappers) {
          dbi.registerMapper(resultSetMapper);
        }

        return dbi;
      } catch (Exception e) {
        throw new ProvisionException("while instantiating DBI", e);
      }
    }
  }

  static class MySQLHistoryJDBIProvider implements Provider<HistoryJDBI> {
    private final DBI dbi;

    @Inject
    public MySQLHistoryJDBIProvider(DBI dbi) {
      this.dbi = dbi;
    }

    @Override
    public MySQLHistoryJDBI get() {
      return dbi.onDemand(MySQLHistoryJDBI.class);
    }

  }

  static class PostgresHistoryJDBIProvider implements Provider<HistoryJDBI> {
    private final DBI dbi;

    @Inject
    public PostgresHistoryJDBIProvider(DBI dbi) {
      this.dbi = dbi;
    }

    @Override
    public PostgresHistoryJDBI get() {
      return dbi.onDemand(PostgresHistoryJDBI.class);
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
