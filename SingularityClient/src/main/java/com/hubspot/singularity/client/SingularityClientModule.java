package com.hubspot.singularity.client;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.ning.http.client.AsyncHttpClient;

public class SingularityClientModule extends AbstractModule {
  
  @Override
  protected void configure() {
    bind(AsyncHttpClient.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return new ObjectMapper()
        .setSerializationInclusion(Include.NON_NULL)
        .registerModule(new ProtobufModule());
  }
  
  
}
