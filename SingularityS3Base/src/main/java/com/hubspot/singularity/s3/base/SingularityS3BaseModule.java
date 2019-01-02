package com.hubspot.singularity.s3.base;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class SingularityS3BaseModule extends AbstractModule {
  public static final String METRICS_OBJECT_MAPPER = "singularity.s3base.metrics.object.mapper";

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  @Named(METRICS_OBJECT_MAPPER)
  public ObjectMapper getObjectMapper(ObjectMapper mapper) {
    return mapper.registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
  }
}
