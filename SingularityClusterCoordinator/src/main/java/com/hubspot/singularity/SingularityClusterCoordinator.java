package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.hubspot.dropwizard.guicier.GuiceBundle;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class SingularityClusterCoordinator extends Application<ClusterCoordinatorConfiguration> {
  @Override
  public void initialize(final Bootstrap<ClusterCoordinatorConfiguration> bootstrap) {

    final GuiceBundle<ClusterCoordinatorConfiguration> guiceBundle = GuiceBundle.defaultBuilder(ClusterCoordinatorConfiguration.class)
        .modules(new SingularityClusterCoordinatorModule())
        .build();
    bootstrap.addBundle(guiceBundle);

    bootstrap.addBundle(new ViewBundle<>());
    bootstrap.addBundle(new AssetsBundle("/assets/static/", "/static/"));
    bootstrap.addBundle(new AssetsBundle("/assets/api-docs/", "/api-docs/", "index.html", "api-docs"));

    bootstrap.getObjectMapper().registerModule(new ProtobufModule());
    bootstrap.getObjectMapper().registerModule(new GuavaModule());
    bootstrap.getObjectMapper().setSerializationInclusion(Include.NON_NULL);
    bootstrap.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public void run(final ClusterCoordinatorConfiguration configuration, final Environment environment) throws Exception {}

  public static void main(final String[] args) throws Exception {
    try {
      new SingularityClusterCoordinator().run(args);
    } catch (final Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }
}
