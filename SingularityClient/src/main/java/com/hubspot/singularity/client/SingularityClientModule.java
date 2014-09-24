package com.hubspot.singularity.client;

import com.google.inject.AbstractModule;

public class SingularityClientModule extends AbstractModule {

  public static final String HTTP_CLIENT_NAME = "singularity.http.client";

  public static final String HOSTS_PROPERTY_NAME = "singularity.hosts"; // bind this name to not use the curator discovery
  public static final String CURATOR_NAME = "singularity.curator"; // bind this instead to provide a curator framework to discover singularity

  public static final String CONTEXT_PATH = "singularity.context.path";
  //
  //  @Override
  @Override
  protected void configure() {
    //    bind(HttpClient.class).annotatedWith(Names.named(HTTP_CLIENT_NAME)).toInstance(new ApacheHttpClient());
  }
  //
  //  @Provides
  //  @Singleton
  //  @Named(OBJECT_MAPPER_NAME)
  //  public ObjectMapper getObjectMapper() {
  //    return new ObjectMapper()
  //    .setSerializationInclusion(Include.NON_NULL)
  //    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  //    .registerModule(new GuavaModule())
  //    .registerModule(new ProtobufModule());
  //  }

}
