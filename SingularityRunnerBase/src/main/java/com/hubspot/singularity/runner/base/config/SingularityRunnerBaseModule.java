package com.hubspot.singularity.runner.base.config;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Set;

import javax.validation.Validation;
import javax.validation.Validator;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.jackson.ObfuscateModule;

public class SingularityRunnerBaseModule extends AbstractModule {
  public static final String PROCESS_NAME = "process.name";
  public static final String YAML = "yaml";
  public static final String OBFUSCATED_YAML = "obfuscated.yaml";

  private final Class<? extends BaseRunnerConfiguration> primaryConfigurationClass;
  private final Set<Class<? extends BaseRunnerConfiguration>> additionalConfigurationClasses;

  public SingularityRunnerBaseModule(Class<? extends BaseRunnerConfiguration> primaryConfigurationClass) {
    this(primaryConfigurationClass, Collections.<Class<? extends BaseRunnerConfiguration>>emptySet());
  }

  public SingularityRunnerBaseModule(Class<? extends BaseRunnerConfiguration> primaryConfigurationClass, Set<Class<? extends BaseRunnerConfiguration>> additionalConfigurationClasses) {
    this.primaryConfigurationClass = primaryConfigurationClass;
    this.additionalConfigurationClasses = additionalConfigurationClasses;
  }

  @Override
  protected void configure() {
    bind(ObjectMapper.class).toInstance(JavaUtils.newObjectMapper());
    bind(MetricRegistry.class).toInstance(new MetricRegistry());
    bind(Validator.class).toInstance(Validation.buildDefaultValidatorFactory().getValidator());

    final ConfigurationBinder configurationBinder = ConfigurationBinder.newBinder(binder());

    configurationBinder.bindPrimaryConfiguration(primaryConfigurationClass);
    for (Class<? extends BaseRunnerConfiguration> additionalConfigurationClass : additionalConfigurationClasses) {
      configurationBinder.bindConfiguration(additionalConfigurationClass);
    }

    if (!additionalConfigurationClasses.contains(SingularityRunnerBaseConfiguration.class)) {
      configurationBinder.bindConfiguration(SingularityRunnerBaseConfiguration.class);
    }

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
  @Named(YAML)
  public ObjectMapper providesYamlMapper() {
    final YAMLFactory yamlFactory = new YAMLFactory();
    yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);

    final ObjectMapper mapper = new ObjectMapper(yamlFactory);
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new ProtobufModule());

    return mapper;
  }

  @Provides
  @Singleton
  @Named(OBFUSCATED_YAML)
  public ObjectMapper providesObfuscatedYamlMapper(@Named(YAML) ObjectMapper yamlMapper) {
    return yamlMapper.copy()
            .setSerializationInclusion(JsonInclude.Include.ALWAYS)
            .registerModule(new ObfuscateModule());
  }
}
