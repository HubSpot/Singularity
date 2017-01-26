package com.hubspot.singularity.athena;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.singularity.config.AthenaConfig;
import com.hubspot.singularity.config.SingularityConfiguration;

public class AthenaModule extends AbstractModule {

  private final Optional<AthenaConfig> config;
  public static final String ATHENA_DRIVER_PROPERTIES = "athena.driver.properties";
  public static final String ATHENA_CONNECTION_URL = "athena.connection.url";
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
      bind(AthenaConnectionProvider.class).to(JDBCAthenaConnectionProvider.class).in(Scopes.SINGLETON);
      bind(Properties.class).annotatedWith(Names.named(ATHENA_DRIVER_PROPERTIES)).toProvider(AthenaQueryDriverProvider.class).in(Scopes.SINGLETON);
      bind(String.class).annotatedWith(Names.named(ATHENA_CONNECTION_URL)).toInstance(config.get().getAthenaUrl());
    } else {
      bind(AthenaConnectionProvider.class).to(NoopAthenaConnectionProvider.class).in(Scopes.SINGLETON);
    }
  }

  @Provides
  @Singleton
  @Named(ATHENA_QUERY_EXECUTOR)
  public ExecutorService providesQueryExecutor() {
    return Executors.newFixedThreadPool(1);
  }

  @Provides
  @Singleton
  public Optional<AthenaConfig> providesAthenaConfig(SingularityConfiguration configuration) {
    return configuration.getAthenaConfig();
  }

  // TODO - Athena S3 client

  static class AthenaQueryDriverProvider implements Provider<Properties> {
    private final Optional<AthenaConfig> athenaConfig;

    @Inject
    public AthenaQueryDriverProvider(Optional<AthenaConfig> athenaConfig) {
      this.athenaConfig = athenaConfig;
    }

    @Override
    public Properties get() {
      Properties info = new Properties();
      info.put("user", athenaConfig.get().getS3AccessKey());
      info.put("password", athenaConfig.get().getS3SecretKey());
      info.put("s3_staging_dir", String.format("s3://%s/%s", athenaConfig.get().getS3StagingBucket(), athenaConfig.get().getS3StagingPrefix()));
      return info;
    }
  }
}
