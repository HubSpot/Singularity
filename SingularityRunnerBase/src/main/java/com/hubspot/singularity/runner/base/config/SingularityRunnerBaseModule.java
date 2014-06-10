package com.hubspot.singularity.runner.base.config;

import java.lang.management.ManagementFactory;
import java.util.Properties;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

public class SingularityRunnerBaseModule extends AbstractModule {

  public static final String JSON_MAPPER = "object.mapper.json";
  public static final String PROCESS_NAME = "process.name";
  
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
  @Named(PROCESS_NAME)
  public String getProcessName() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    if (name != null && name.contains("@")) {
      return name.substring(0, name.indexOf("@"));
    }
    return name;
  }
  
  @Provides
  @Singleton
  @Named(JSON_MAPPER)
  public ObjectMapper getJsonObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new ProtobufModule());
    return mapper;
  }
  
  @Provides
  @Singleton
  public MetricRegistry getMetricRegistry() {
    return new MetricRegistry();
  }
  
}
