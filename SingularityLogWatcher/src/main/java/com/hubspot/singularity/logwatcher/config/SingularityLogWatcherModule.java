package com.hubspot.singularity.logwatcher.config;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

public class SingularityLogWatcherModule extends AbstractModule {

  public static final String JSON_MAPPER = "object.mapper.json";
  
  @Override
  protected void configure() {
    bindPropertiesFile("/etc/singularity.logwatcher.properties");
  }
  
  private void bindDefaults(Properties properties) {
  }
  
  private void bindPropertiesFile(String file) {
    Properties properties = new Properties();
    bindDefaults(properties);
    try (BufferedReader br = Files.newBufferedReader(Paths.get(file), Charset.defaultCharset())) {
      properties.load(br);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    Names.bindProperties(binder(), properties);
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
  
}
