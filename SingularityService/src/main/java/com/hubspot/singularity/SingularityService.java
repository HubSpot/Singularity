package com.hubspot.singularity;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.hubspot.dropwizard.guice.GuiceBundle;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityService extends Application<SingularityConfiguration> {

  @Override
  public void initialize(Bootstrap<SingularityConfiguration> bootstrap) {
    GuiceBundle<SingularityConfiguration> guiceBundle = GuiceBundle.<SingularityConfiguration>newBuilder()
        .addModule(new SingularityModule())
        .enableAutoConfig(getClass().getPackage().getName())
        .setConfigClass(SingularityConfiguration.class)
        .build();
    bootstrap.addBundle(guiceBundle);
  }

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {}

  public static void main(String[] args) throws Exception {
    new SingularityService().run(args);
  }

}
