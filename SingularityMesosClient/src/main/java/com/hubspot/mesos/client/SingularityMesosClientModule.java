package com.hubspot.mesos.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.HttpConfig.Builder;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.mesos.JavaUtils;

public class SingularityMesosClientModule extends AbstractModule {

  public static final String MESOS_CLIENT_OBJECT_MAPPER = "singularity.mesos.client.object.mapper";
  private static final int MESOS_CLIENT_HTTP_SHORT_TIMEOUT_SECONDS = 5;

  @Override
  protected void configure() {
    ObjectMapper objectMapper = JavaUtils.newObjectMapper();
    Builder httpConfigBuilder = HttpConfig.newBuilder().setObjectMapper(objectMapper);

    bind(ObjectMapper.class).annotatedWith(Names.named(MESOS_CLIENT_OBJECT_MAPPER)).toInstance(objectMapper);
    bind(HttpClient.class).annotatedWith(Names.named(SingularityMesosClient.DEFAULT_HTTP_CLIENT_NAME))
        .toInstance(new NingHttpClient(httpConfigBuilder.build()));

    bind(HttpClient.class).annotatedWith(Names.named(SingularityMesosClient.SHORT_TIMEOUT_HTTP_CLIENT_NAME))
        .toInstance(new NingHttpClient(httpConfigBuilder.setRequestTimeoutSeconds(MESOS_CLIENT_HTTP_SHORT_TIMEOUT_SECONDS).build()));

    bind(MesosClient.class).to(SingularityMesosClient.class).in(Scopes.SINGLETON);
  }

}
