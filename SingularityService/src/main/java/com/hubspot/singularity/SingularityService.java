package com.hubspot.singularity;


import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.guice.GuiceBundle;

public class SingularityService extends Application<SingularityConfiguration> {

  public static final String API_BASE_PATH = "/api";

  @Override
  public void initialize(final Bootstrap<SingularityConfiguration> bootstrap) {
    final GuiceBundle<SingularityConfiguration> guiceBundle = GuiceBundle.defaultBuilder(SingularityConfiguration.class)
        .modules(new SingularityServiceModule())
        .build();
    bootstrap.addBundle(guiceBundle);

    bootstrap.addBundle(new CorsBundle());
    bootstrap.addBundle(new ViewBundle());
    bootstrap.addBundle(new AssetsBundle("/static/static/", "/static/"));
    bootstrap.addBundle(new AssetsBundle("/static/api-docs/", "/api-docs/", "index.html", "api-docs"));
    bootstrap.addBundle(new MigrationsBundle<SingularityConfiguration>() {
      @Override
      public DataSourceFactory getDataSourceFactory(final SingularityConfiguration configuration) {
        return configuration.getDatabaseConfiguration().get();
      }
    });

    bootstrap.getObjectMapper().registerModule(new ProtobufModule());
    bootstrap.getObjectMapper().registerModule(new GuavaModule());
    bootstrap.getObjectMapper().setSerializationInclusion(Include.NON_NULL);
    bootstrap.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public void run(final SingularityConfiguration configuration, final Environment environment) throws Exception {}

  public static void main(final String[] args) {
    try {
      new SingularityService().run(args);
    } catch (final Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

}
