package com.hubspot.singularity;

import javax.inject.Inject;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class SingularityAsyncHttpClient extends AsyncHttpClient {

  public SingularityAsyncHttpClient(AsyncHttpClientConfig clientConfig) {
    super(clientConfig);
  }

  @Inject
  public SingularityAsyncHttpClient() {}
}
