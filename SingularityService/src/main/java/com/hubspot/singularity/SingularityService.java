package com.hubspot.singularity;

import com.google.inject.Stage;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.hubspot.dropwizard.guice.GuiceBundle;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityService extends Application<SingularityConfiguration> {

  @Override
  public void initialize(Bootstrap<SingularityConfiguration> bootstrap) {
    GuiceBundle<SingularityConfiguration> guiceBundle = GuiceBundle.<SingularityConfiguration>newBuilder()
        .addModule(new SingularityModule())
        .enableAutoConfig(getClass().getPackage().getName())
        .setConfigClass(SingularityConfiguration.class)
        .build(Stage.DEVELOPMENT);
    bootstrap.addBundle(guiceBundle);
    
    bootstrap.getObjectMapper().registerModule(new ProtobufModule());
  }

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {}

  public static void main(String[] args) throws Exception {
    new SingularityService().run(args);
  }

}
