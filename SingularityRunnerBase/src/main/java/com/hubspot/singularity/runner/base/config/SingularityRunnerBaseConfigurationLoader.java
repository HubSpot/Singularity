package com.hubspot.singularity.runner.base.config;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.inject.Binder;
import com.google.inject.name.Names;
import com.hubspot.mesos.JavaUtils;

public class SingularityRunnerBaseConfigurationLoader {

  public static final String LOGGING_PATTERN = "logging.pattern";
  public static final String ROOT_LOG_PATH = "root.log.path";
  
  public static final String ROOT_LOG_LEVEL = "root.log.level";

  public static final String LOG_METADATA_DIRECTORY = "logwatcher.metadata.directory";
  public static final String LOG_METADATA_SUFFIX = "logwatcher.metadata.suffix";
  
  public static final String S3_METADATA_SUFFIX = "s3uploader.metadata.suffix";
  public static final String S3_METADATA_DIRECTORY = "s3uploader.metadata.directory";
  
  protected Properties bindPropertiesFile(String configurationFilePath, Binder binder) {
    Properties properties = new Properties();
    bindDefaults(properties);
    try (BufferedReader br = Files.newBufferedReader(Paths.get(configurationFilePath), Charset.defaultCharset())) {
      properties.load(br);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    Names.bindProperties(binder, properties);
    return properties;
  }
  
  public static Path getValidDirectory(String directoryPath, String name) {
    Preconditions.checkState(!directoryPath.isEmpty(), "Path for %s can't be empty", name);
    
    Path path = Paths.get(directoryPath);
    
    Preconditions.checkState(Files.isDirectory(path), "Path %s for %s wasn't a directory", path, name);
    
    return path;
  }
  
  protected void bindDefaults(Properties properties) {
    properties.put(LOGGING_PATTERN, JavaUtils.LOGBACK_LOGGING_PATTERN);
    properties.put(LOG_METADATA_SUFFIX, ".tail.json");
    properties.put(ROOT_LOG_LEVEL, "INFO");
    properties.put(S3_METADATA_SUFFIX, ".s3.json");
    properties.put(S3_METADATA_DIRECTORY, "");
    properties.put(LOG_METADATA_DIRECTORY, "");
  }

}
