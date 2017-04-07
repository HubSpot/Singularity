package com.hubspot.singularity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;

import io.dropwizard.lifecycle.Managed;

public class SingularityHttpClient extends NingHttpClient implements Managed {

  @Inject
  public SingularityHttpClient(ObjectMapper objectMapper) {
    super(HttpConfig.newBuilder().setObjectMapper(objectMapper).build());
  }

  @Override
  public void start() {}

  @Override
  public void stop() throws Exception {
    close();
  }

}
