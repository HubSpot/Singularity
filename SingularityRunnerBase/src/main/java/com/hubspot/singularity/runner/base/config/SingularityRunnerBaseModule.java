package com.hubspot.singularity.runner.base.config;

import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

public class SingularityRunnerBaseModule extends AbstractModule {

  public static final String JSON_MAPPER = "object.mapper.json";
  
  private final String rootPath;
  private final SingularityRunnerBaseConfigurationLoader configuration;
  
  public SingularityRunnerBaseModule(String rootPath, SingularityRunnerBaseConfigurationLoader configuration) {
    this.rootPath = rootPath;
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    Properties properties = configuration.bindPropertiesFile(rootPath, binder());
    
    bind(Properties.class).toInstance(properties);
    bind(SingularityRunnerBaseLogging.class).asEagerSingleton();
  }
  
  @Provides
  @Singleton
  @Named(JSON_MAPPER)
  public ObjectMapper getJsonObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new ProtobufModule());
    return mapper;
  }

  @Provides
  @Singleton
  public MustacheFactory providesMustacheFactory() {
    return new DefaultMustacheFactory();
  }

}
