package com.hubspot.singularity.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.mesos.JavaUtils;

public class SingularityClientModule extends AbstractModule {

  public static final String HTTP_CLIENT_NAME = "singularity.http.client";

  // bind this name to not use the curator discovery, eg: http://localhost:5060,http://localhost:7000
  public static final String HOSTS_PROPERTY_NAME = "singularity.hosts";

  // bind this instead to provide a curator framework to discover singularity
  public static final String CURATOR_NAME = "singularity.curator";

  // bind this to provide the path for singularity eg: singularity/v2/api
  public static final String CONTEXT_PATH = "singularity.context.path";

  @Override
  protected void configure() {
    ObjectMapper objectMapper = JavaUtils.newObjectMapper();
    HttpClient httpClient = new NingHttpClient(HttpConfig.newBuilder().setObjectMapper(objectMapper).build());

    bind(HttpClient.class).annotatedWith(Names.named(HTTP_CLIENT_NAME)).toInstance(httpClient);
  }

}
