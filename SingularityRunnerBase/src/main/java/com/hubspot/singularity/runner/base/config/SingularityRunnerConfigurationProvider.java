package com.hubspot.singularity.runner.base.config;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;

import io.dropwizard.configuration.ConfigurationValidationException;

public class SingularityRunnerConfigurationProvider<T extends BaseRunnerConfiguration> implements Provider<T> {
  private final Class<T> clazz;
  private final Optional<String> filename;

  @Inject
  @Named(SingularityRunnerBaseModule.YAML)
  private ObjectMapper objectMapper;

  @Inject
  private Validator validator;

  public SingularityRunnerConfigurationProvider(Class<T> clazz, Optional<String> filename) {
    this.clazz = clazz;
    this.filename = filename;
  }

  @Override
  public T get() {
    final Configuration configuration = clazz.getAnnotation(Configuration.class);

    final String yamlPath = filename.or(configuration.filename());
    final String propsPath = yamlPath.replace(".yaml", ".properties");

    final File yamlFile = new File(yamlPath);
    final File propsFile = new File(propsPath);

    try {
      JsonNode node = objectMapper.readTree(yamlFile);

      if (filename.isPresent() && !Strings.isNullOrEmpty(configuration.consolidatedField())) {
        if (node.hasNonNull(configuration.consolidatedField())) {
          node = node.get(configuration.consolidatedField());
        } else {
          node = objectMapper.createObjectNode();
        }
      }

      final T config = yamlFile.exists() ? objectMapper.treeToValue(node, clazz) : clazz.newInstance();

      if (propsFile.exists()) {
        final Properties properties = new Properties();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(propsPath), Charset.defaultCharset())) {
          properties.load(br);
        }
        config.updateFromProperties(properties);
        config.updateLoggingFromProperties(properties);
      }

      final Set<ConstraintViolation<T>> violations = validator.validate(config);
      if (!violations.isEmpty()) {
        throw new ConfigurationValidationException(yamlPath, violations);
      }

      return config;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
