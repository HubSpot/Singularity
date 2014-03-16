package com.hubspot.singularity;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import com.google.inject.Stage;
import com.hubspot.dropwizard.guice.GuiceBundle;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.jackson.jaxrs.PropertyFilteringMessageBodyWriter;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.smtp.SMTPAppenderBundle;

public class SingularityService extends Application<SingularityConfiguration> {

  @Override
  public void initialize(Bootstrap<SingularityConfiguration> bootstrap) {
    GuiceBundle<SingularityConfiguration> guiceBundle = GuiceBundle.<SingularityConfiguration>newBuilder()
        .addModule(new SingularityModule())
        .enableAutoConfig(getClass().getPackage().getName())
        .setConfigClass(SingularityConfiguration.class)
        .build(Stage.DEVELOPMENT);
    bootstrap.addBundle(guiceBundle);

    bootstrap.addBundle(new SMTPAppenderBundle());
    bootstrap.addBundle(new AssetsBundle("/static/static/", "/static/"));
    bootstrap.addBundle(new MigrationsBundle<SingularityConfiguration>() {
      @Override
      public DataSourceFactory getDataSourceFactory(SingularityConfiguration configuration) {
        return configuration.getDataSourceFactory();
      }
    });
    
    bootstrap.getObjectMapper().registerModule(new ProtobufModule());
  }

  @Override
  public void run(SingularityConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(PropertyFilteringMessageBodyWriter.class);
    environment.jersey().setUrlPattern("/v1/*");
    environment.servlets().addServlet("brunch", new SingularityBrunchServlet("/static/", "/", "index.html")).addMapping("/*");
  }

  public static void main(String[] args) throws Exception {
    new SingularityService().run(args);
  }

}
