package com.hubspot.singularity.client;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.horizon.HttpClient;

public class SingularityClientProvider {

  private final HttpClient httpClient;

  @Inject
  public SingularityClientProvider(@Named(SingularityClientModule.HTTP_CLIENT_NAME) HttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public SingularityClient buildClient(String contextPath, String hosts) {
    return new SingularityClient(contextPath, httpClient, hosts);
  }

}
