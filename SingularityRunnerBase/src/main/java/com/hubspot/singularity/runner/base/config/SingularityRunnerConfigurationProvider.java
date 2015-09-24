package com.hubspot.singularity.runner.base.config;

import java.io.File;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
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

  private JsonNode loadYamlField(String filename, String field) {
    final File yamlFile = new File(filename);

    if (!yamlFile.exists()) {
      return objectMapper.createObjectNode();
    }

    try {
      final JsonNode baseTree = objectMapper.readTree(yamlFile);

      return baseTree.hasNonNull(field) ? baseTree.get(field) : objectMapper.createObjectNode();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public T get() {
    final Configuration configuration = clazz.getAnnotation(Configuration.class);

    try {
      final File baseFile = new File(configuration.filename());
      final T baseConfig = baseFile.exists() ? objectMapper.readValue(baseFile, clazz) : clazz.newInstance();

      final JsonNode overrideNode = filename.isPresent() ? loadYamlField(filename.get(), configuration.consolidatedField()) : objectMapper.createObjectNode();

      final T config = objectMapper.readerForUpdating(baseConfig).readValue(overrideNode);

      final Set<ConstraintViolation<T>> violations = validator.validate(config);
      if (!violations.isEmpty()) {
        throw new ConfigurationValidationException(filename.or(configuration.filename()), violations);
      }

      return config;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
