package com.hubspot.singularity.runner.base.config;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.base.Throwables;
import com.google.inject.Binder;
import com.google.inject.name.Names;
import com.hubspot.mesos.JavaUtils;

public class SingularityRunnerBaseConfigurationLoader {

  public static final String LOGGING_PATTERN = "logging.pattern";
  public static final String ROOT_LOG_PATH = "root.log.path";
  
  protected void bindPropertiesFile(String configurationFilePath, Binder binder) {
    Properties properties = new Properties();
    bindDefaults(properties);
    try (BufferedReader br = Files.newBufferedReader(Paths.get(configurationFilePath), Charset.defaultCharset())) {
      properties.load(br);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    Names.bindProperties(binder, properties);
  }
  
  protected void bindDefaults(Properties properties) {
    properties.put(LOGGING_PATTERN, JavaUtils.LOGBACK_LOGGING_PATTERN);
  }

}
