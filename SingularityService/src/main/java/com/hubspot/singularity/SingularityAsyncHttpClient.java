package com.hubspot.singularity;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

// Closed in SingularityLifecycleManaged
public class SingularityAsyncHttpClient extends AsyncHttpClient {
  public SingularityAsyncHttpClient(AsyncHttpClientConfig clientConfig) {
    super(clientConfig);
  }
}
