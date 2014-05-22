package com.hubspot.singularity.client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.ning.http.client.AsyncHttpClient;

public class SingularityClientModule extends AbstractModule {
  
  public static final String HTTP_CLIENT_NAME = "singularity.http.client";
  public static final String OBJECT_MAPPER_NAME = "singularity.object.mapper";

  public static final String HOSTS_PROPERTY_NAME = "singularity.hosts"; // bind this name to not use the curator discovery
  public static final String CURATOR_NAME = "singularity.curator"; // bind this instead to provide a curator framework to discover singularity
  
  public static final String CONTEXT_PATH = "singularity.context.path";
  
  @Override
  protected void configure() {
    bind(AsyncHttpClient.class).annotatedWith(Names.named(HTTP_CLIENT_NAME)).toInstance(new AsyncHttpClient());
  }

  @Provides
  @Singleton
  @Named(OBJECT_MAPPER_NAME)
  public ObjectMapper getObjectMapper() {
    return new ObjectMapper()
        .setSerializationInclusion(Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new GuavaModule())
        .registerModule(new ProtobufModule());
  }
  
}
