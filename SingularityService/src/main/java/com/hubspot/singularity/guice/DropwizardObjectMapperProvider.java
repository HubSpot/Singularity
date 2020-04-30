package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.setup.Environment;
import javax.inject.Inject;
import javax.inject.Provider;

public class DropwizardObjectMapperProvider implements Provider<ObjectMapper> {
  private final Environment environment;

  @Inject
  public DropwizardObjectMapperProvider(final Environment environment) {
    this.environment = checkNotNull(environment, "environment is null");
  }

  @Override
  public ObjectMapper get() {
    return environment.getObjectMapper().copy();
  }
}
