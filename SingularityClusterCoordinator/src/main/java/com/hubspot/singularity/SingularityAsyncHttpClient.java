package com.hubspot.singularity;

import com.google.inject.Inject;
import com.ning.http.client.AsyncHttpClient;

import io.dropwizard.lifecycle.Managed;

public class SingularityAsyncHttpClient extends AsyncHttpClient implements Managed {

  @Inject
  public SingularityAsyncHttpClient() {}

  @Override
  public void start() {}

  @Override
  public void stop() {
    close();
  }

}
