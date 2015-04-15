package com.hubspot.singularity.runner.base.config;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

public class SingularityRunnerConfigurationProvider<T extends BaseRunnerConfiguration> implements Provider<T> {
  private final Class<T> clazz;

  @Inject
  @Named(SingularityRunnerBaseModule.YAML)
  private ObjectMapper objectMapper;

  public SingularityRunnerConfigurationProvider(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public T get() {
    final Configuration configuration = clazz.getAnnotation(Configuration.class);

    final String yamlPath = configuration.value();
    final String propsPath = yamlPath.replace(".yaml", ".properties");

    final File yamlFile = new File(yamlPath);
    final File propsFile = new File(propsPath);

    try {
      final T config = yamlFile.exists() ? objectMapper.readValue(yamlFile, clazz) : clazz.newInstance();

      if (propsFile.exists()) {
        final Properties properties = new Properties();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(propsPath), Charset.defaultCharset())) {
          properties.load(br);
        }
        config.updateFromProperties(properties);
        config.updateLoggingFromProperties(properties);
      }

      return config;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
