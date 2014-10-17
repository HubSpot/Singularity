package com.hubspot.singularity.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.ning.http.client.AsyncHttpClient;

import org.apache.curator.framework.CuratorFramework;

public class SingularityClientModule extends AbstractModule {

  public static final String HTTP_CLIENT_NAME = "singularity.http.client";
  public static final String OBJECT_MAPPER_NAME = "singularity.object.mapper";

  public static final String HOSTS_PROPERTY_NAME = "singularity.hosts"; // bind this name to not use the curator discovery
  public static final String CURATOR_NAME = "singularity.curator"; // bind this instead to provide a curator framework to discover singularity

  public static final String CONTEXT_PATH = "singularity.context.path";

  private final List<String> hosts;

  public SingularityClientModule() {
    this(null);
  }

  public SingularityClientModule(List<String> hosts) {
    this.hosts = hosts;
  }

  @Override
  protected void configure() {
    bind(SingularityClient.class).toProvider(SingularityClientProvider.class).in(Scopes.SINGLETON);
    bind(AsyncHttpClient.class).annotatedWith(Names.named(HTTP_CLIENT_NAME)).toInstance(new AsyncHttpClient());

    if (hosts != null) {
      bindHosts(binder()).toInstance(hosts);
    }
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

  public static LinkedBindingBuilder<List<String>> bindHosts(Binder binder) {
    return binder.bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(HOSTS_PROPERTY_NAME));
  }

  public static LinkedBindingBuilder<String> bindContextPath(Binder binder) {
    return binder.bind(String.class).annotatedWith(Names.named(CONTEXT_PATH));
  }

  public static LinkedBindingBuilder<CuratorFramework> bindCurator(Binder binder) {
    return binder.bind(CuratorFramework.class).annotatedWith(Names.named(CURATOR_NAME));
  }
}
