package com.hubspot.singularity.runner.base.config;

import java.lang.management.ManagementFactory;
import java.util.Properties;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.collect.ObjectArrays;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

public class SingularityRunnerBaseModule extends AbstractModule {

  public static final String PROCESS_NAME = "process.name";

  private final SingularityConfigurationLoader[] configurations;

  public SingularityRunnerBaseModule(SingularityConfigurationLoader... configurations) {
    this.configurations = ObjectArrays.concat(new SingularityRunnerBaseConfigurationLoader(), configurations);
  }

  @Override
  protected void configure() {
    Properties properties = new Properties();

    for (SingularityConfigurationLoader configurationLoader : configurations) {
      configurationLoader.bindAllDefaults(properties);
    }

    for (SingularityConfigurationLoader configurationLoader : configurations) {
      configurationLoader.bindPropertiesFile(properties);
    }

    Names.bindProperties(binder(), properties);

    bind(Properties.class).toInstance(properties);
    bind(SingularityRunnerBaseLogging.class).asEagerSingleton();
  }

  @Provides
  @Singleton
  @Named(PROCESS_NAME)
  public String getProcessName() {
    String name = ManagementFactory.getRuntimeMXBean().getName();
    if ((name != null) && name.contains("@")) {
      return name.substring(0, name.indexOf("@"));
    }
    return name;
  }

  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
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
