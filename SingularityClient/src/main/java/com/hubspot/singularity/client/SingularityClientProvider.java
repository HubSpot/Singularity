package com.hubspot.singularity.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.ning.http.client.AsyncHttpClient;

public class SingularityClientProvider {

  private final ObjectMapper objectMapper;
  private final AsyncHttpClient httpClient;
  
  @Inject
  public SingularityClientProvider(@Named(SingularityClientModule.HTTP_CLIENT_NAME) AsyncHttpClient httpClient, @Named(SingularityClientModule.OBJECT_MAPPER_NAME) ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }
    
  public SingularityClient buildClient(String contextPath, String hosts) {
    return new SingularityClient(contextPath, httpClient, objectMapper, hosts);
  }
  
}
