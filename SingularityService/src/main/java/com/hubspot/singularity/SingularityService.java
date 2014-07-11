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
import com.google.inject.Stage;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.jackson.jaxrs.PropertyFilteringMessageBodyWriter;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.smtp.SMTPAppenderBundle;

public class SingularityService extends Application<SingularityConfiguration> {

  public static final String API_BASE_PATH = "/api";

  @Override
  public void initialize(Bootstrap<SingularityConfiguration> bootstrap) {
    GuiceBundle<SingularityConfiguration> guiceBundle = GuiceBundle.<SingularityConfiguration>newBuilder()
        .addModule(new SingularityServiceModule())
        .enableAutoConfig(getClass().getPackage().getName())
        .setConfigClass(SingularityConfiguration.class)
        .build(Stage.DEVELOPMENT);
    bootstrap.addBundle(guiceBundle);

    bootstrap.addBundle(new ViewBundle());
    bootstrap.addBundle(new SMTPAppenderBundle());
    bootstrap.addBundle(new AssetsBundle("/static/static/", "/static/"));
    bootstrap.addBundle(new MigrationsBundle<SingularityConfiguration>() {
      @Override
      public DataSourceFactory getDataSourceFactory(SingularityConfiguration configuration) {
        return configuration.getDataSourceFactory();
      }
    });
    
    bootstrap.getObjectMapper().registerModule(new ProtobufModule());
    bootstrap.getObjectMapper().setSerializationInclusion(Include.NON_NULL);
    bootstrap.getObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(PropertyFilteringMessageBodyWriter.class);
  }

  public static void main(String[] args) {
    try {
      new SingularityService().run(args);
    } catch (Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }

}
