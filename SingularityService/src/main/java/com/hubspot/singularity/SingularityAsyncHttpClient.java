package com.hubspot.singularity;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import javax.inject.Inject;

public class SingularityAsyncHttpClient extends AsyncHttpClient {

  public SingularityAsyncHttpClient(AsyncHttpClientConfig clientConfig) {
    super(clientConfig);
  }

  @Inject
  public SingularityAsyncHttpClient() {}
}
