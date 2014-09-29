package com.hubspot.mesos.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.mesos.JavaUtils;

public class SingularityMesosClientModule extends AbstractModule {

  public static final String MESOS_CLIENT_OBJECT_MAPPER = "singularity.mesos.client.object.mapper";

  @Override
  protected void configure() {
    ObjectMapper objectMapper = JavaUtils.newObjectMapper();
    HttpConfig httpConfig = HttpConfig.newBuilder().setObjectMapper(objectMapper).build();
    HttpClient httpClient = new NingHttpClient(httpConfig);

    bind(ObjectMapper.class).annotatedWith(Names.named(MESOS_CLIENT_OBJECT_MAPPER)).toInstance(objectMapper);
    bind(HttpClient.class).annotatedWith(Names.named(MesosClient.HTTP_CLIENT_NAME)).toInstance(httpClient);
  }

}
