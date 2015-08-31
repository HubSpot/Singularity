package com.hubspot.singularity;

import javax.inject.Inject;

import com.ning.http.client.AsyncHttpClient;

import io.dropwizard.lifecycle.Managed;

public class SingularityHttpClient extends AsyncHttpClient implements Managed {

  @Inject
  public SingularityHttpClient() {}

  @Override
  public void start() {}

  @Override
  public void stop() {
    close();
  }

}
