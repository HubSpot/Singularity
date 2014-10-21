package com.hubspot.singularity;

import javax.inject.Inject;

import io.dropwizard.lifecycle.Managed;

import com.ning.http.client.AsyncHttpClient;

public class SingularityHttpClient extends AsyncHttpClient implements Managed
{
  @Inject
  public SingularityHttpClient() {}

  @Override
  public void start() {}

  @Override
  public void stop() {
    close();
  }
}
