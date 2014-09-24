package com.hubspot.mesos.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.mesos.JavaUtils;

public class SingularityMesosClientModule extends AbstractModule {

  @Override
  protected void configure() {
    ObjectMapper objectMapper = JavaUtils.newObjectMapper();
    HttpConfig httpConfig = HttpConfig.newBuilder().setObjectMapper(objectMapper).build();
    HttpClient httpClient = new NingHttpClient(httpConfig);

    bind(HttpClient.class).annotatedWith(Names.named(MesosClient.HTTP_CLIENT_NAME)).toInstance(httpClient);
  }

}
