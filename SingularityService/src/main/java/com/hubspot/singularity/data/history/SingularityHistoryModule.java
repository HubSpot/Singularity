package com.hubspot.singularity.data.history;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Environment;

public class SingularityHistoryModule extends AbstractModule {

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

    if (configuration.isPresent()) {
      bind(DBI.class).toProvider(DBIProvider.class).in(Scopes.SINGLETON);
      bind(HistoryJDBI.class).toProvider(HistoryJDBIProvider.class).in(Scopes.SINGLETON);
      bind(HistoryManager.class).to(JDBIHistoryManager.class).in(Scopes.SINGLETON);

      bindMethodInterceptorForStringTemplateClassLoaderWorkaround();
    } else {
      bind(HistoryManager.class).to(NoopHistoryManager.class).in(Scopes.SINGLETON);
    }
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

  static class HistoryJDBIProvider implements Provider<HistoryJDBI> {
    private final DBI dbi;

    @Inject
    public HistoryJDBIProvider(DBI dbi) {
      this.dbi = dbi;
    }

    @Override
    public HistoryJDBI get() {
      return dbi.onDemand(HistoryJDBI.class);
    }

  }
}
