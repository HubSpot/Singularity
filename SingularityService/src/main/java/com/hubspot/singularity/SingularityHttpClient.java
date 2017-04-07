package com.hubspot.singularity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.horizon.ning.NingHttpClient;

import io.dropwizard.lifecycle.Managed;

public class SingularityHttpClient extends NingHttpClient implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityHttpClient.class);

  @Inject
  public SingularityHttpClient() {}

  @Override
  public void start() {}

  @Override
  public void stop() throws Exception {
    close();
  }

}
