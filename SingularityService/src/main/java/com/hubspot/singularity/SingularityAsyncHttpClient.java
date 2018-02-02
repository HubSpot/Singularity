package com.hubspot.singularity;

import javax.inject.Inject;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

import io.dropwizard.lifecycle.Managed;

public class SingularityAsyncHttpClient extends AsyncHttpClient implements Managed {

  public SingularityAsyncHttpClient(AsyncHttpClientConfig clientConfig) {
    super(clientConfig);
  }

  @Inject
  public SingularityAsyncHttpClient() {}

  @Override
  public void start() {}

  @Override
  public void stop() {
    close();
  }

}
