package com.hubspot.singularity.athena;

import java.util.concurrent.Executors;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.AthenaConfig;
import com.hubspot.singularity.config.SingularityConfiguration;

public class AthenaModule extends AbstractModule {

  private final Optional<AthenaConfig> config;
  public static final String ATHENA_QUERY_EXECUTOR = "athena.query.executor";

  public AthenaModule(Optional<AthenaConfig> config) {
    this.config = config;
  }

  @Override
  public void configure() {
    if (config.isPresent()) {
      try {
        Class.forName("com.amazonaws.athena.jdbc.AthenaDriver");
      } catch (ClassNotFoundException cnfe) {
        throw new RuntimeException("Could not locate Athena driver class");
      }
      bind(AthenaQueryRunner.class).to(JDBCAthenaQueryRunner.class).in(Scopes.SINGLETON);
    } else {
      bind(AthenaQueryRunner.class).to(NoopAthenaQueryRunner.class).in(Scopes.SINGLETON);
    }
  }

  @Provides
  @Singleton
  @Named(ATHENA_QUERY_EXECUTOR)
  public ListeningExecutorService providesQueryExecutor() {
    return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("athena-query-runner-%d").build()
    ));
  }

  @Provides
  @Singleton
  public Optional<AthenaConfig> providesAthenaConfig(SingularityConfiguration configuration) {
    return configuration.getAthenaConfig();
  }
}
