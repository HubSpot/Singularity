package com.hubspot.singularity.runner.base.config;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;

import com.google.common.base.Throwables;

public abstract class SingularityConfigurationLoader {

  private final String propertyFile;
  private final Optional<String> defaultLogFileName;

  public SingularityConfigurationLoader(String propertyFile, Optional<String> defaultLogFileName) {
    this.propertyFile = propertyFile;
    this.defaultLogFileName = defaultLogFileName;
  }

  public void bindPropertiesFile(Properties properties) {
    try (BufferedReader br = Files.newBufferedReader(Paths.get(propertyFile), Charset.defaultCharset())) {
      properties.load(br);
    } catch (NoSuchFileException nsfe) {
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  public void bindAllDefaults(Properties properties) {
    if (defaultLogFileName.isPresent()) {
      properties.put(SingularityRunnerBaseConfigurationLoader.ROOT_LOG_FILENAME, defaultLogFileName.get());
    }

    bindDefaults(properties);
  }

  protected abstract void bindDefaults(Properties properties);

}
