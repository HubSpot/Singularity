package com.hubspot.singularity.s3uploader.config;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;
import com.hubspot.singularity.s3uploader.SingularityS3UploaderDriver;

public class SingularityS3UploaderModule extends AbstractModule {
  public static final String METRICS_OBJECT_MAPPER = "singularity.s3uploader.metrics.object.mapper";

  @Override
  protected void configure() {
    bind(SingularityDriver.class).to(SingularityS3UploaderDriver.class);
  }

  @Provides
  @Singleton
  @Named(METRICS_OBJECT_MAPPER)
  public ObjectMapper getObjectMapper(ObjectMapper mapper) {
    return mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
  }
}
