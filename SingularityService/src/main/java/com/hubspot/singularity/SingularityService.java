package com.hubspot.singularity;


import static com.google.common.base.Preconditions.checkNotNull;

import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.singularity.bundles.AcceptLanguageFilterBundle;
import com.hubspot.singularity.bundles.CorsBundle;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.guice.GuiceBundle;

public class SingularityService<T extends SingularityConfiguration> extends Application<T> {

  public static final String API_BASE_PATH = "/api";

  @Override
  public void initialize(final Bootstrap<T> bootstrap) {

    final Iterable<? extends Module> additionalModules = checkNotNull(getGuiceModules(bootstrap), "getGuiceModules() returned null");
    final Iterable<? extends Bundle> additionalBundles = checkNotNull(getDropwizardBundles(bootstrap), "getDropwizardBundles() returned null");
    final Iterable<? extends ConfiguredBundle<T>> additionalConfiguredBundles = checkNotNull(getDropwizardConfiguredBundles(bootstrap), "getDropwizardConfiguredBundles() returned null");

    final GuiceBundle<SingularityConfiguration> guiceBundle = GuiceBundle.defaultBuilder(SingularityConfiguration.class)
        .modules(new SingularityServiceModule())
        .modules(additionalModules)
        .build();
    bootstrap.addBundle(guiceBundle);

    bootstrap.addBundle(new AcceptLanguageFilterBundle());
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

    for (Bundle bundle : additionalBundles) {
      bootstrap.addBundle(bundle);
    }

    for (ConfiguredBundle<T> configuredBundle : additionalConfiguredBundles) {
      bootstrap.addBundle(configuredBundle);
    }

    bootstrap.getObjectMapper().registerModule(new ProtobufModule());
    bootstrap.getObjectMapper().registerModule(new GuavaModule());
    bootstrap.getObjectMapper().setSerializationInclusion(Include.NON_NULL);
    bootstrap.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public void run(final T configuration, final Environment environment) throws Exception {}

  /**
   * Guice modules used in addition to the modules required by Singularity. This is an extension point when embedding
   * Singularity into a custom service.
   */
  public Iterable<? extends Module> getGuiceModules(Bootstrap<T> bootstrap) {
    return ImmutableList.of();
  }

  /**
   * Dropwizard bundles used in addition to the bundles required by Singularity. This is an extension point when embedding
   * Singularity into a custom service.
   */
  public Iterable<? extends Bundle> getDropwizardBundles(Bootstrap<T> bootstrap) {
    return ImmutableList.of();
  }

  public Iterable<? extends ConfiguredBundle<T>> getDropwizardConfiguredBundles(Bootstrap<T> bootstrap) {
    return ImmutableList.of();
  }


  public static void main(final String[] args) throws Exception {
    try {
      new SingularityService<SingularityConfiguration>().run(args);
    } catch (final Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }
}
