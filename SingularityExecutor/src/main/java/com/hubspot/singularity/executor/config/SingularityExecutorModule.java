package com.hubspot.singularity.executor.config;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;

public class SingularityExecutorModule extends AbstractModule {

  public static final String ARTIFACT_CACHE_DIRECTORY = "hubspot.mesos.executor.artifact.cache.directory";
  public static final String DEPLOY_ENV = "deploy.env";

  public static final String RUNNER_TEMPLATE = "runner.sh";
  public static final String ENVIRONMENT_TEMPLATE = "deploy.env";

  public static final String YAML_MAPPER = "object.mapper.yaml";
  public static final String JSON_MAPPER = "object.mapper.json";
    
  @Override
  protected void configure() {
    configureLogger();

    bindConstant().annotatedWith(Names.named(DEPLOY_ENV)).to("qa");
    bindConstant().annotatedWith(Names.named(ARTIFACT_CACHE_DIRECTORY)).to("/Users/wsorenson/dev/mesos/cache");
  }
  
  private void configureLogger() {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger logger = context.getLogger(Logger.ROOT_LOGGER_NAME);
    
    logger.detachAndStopAllAppenders();
    
    FileAppender<ILoggingEvent> fileAppender = new FileAppender<>();
    fileAppender.setFile("/Users/wsorenson/dev/mesos/executor.log");
    fileAppender.setContext(context);
    
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setContext(context);
    encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
    encoder.start();
    
    fileAppender.setEncoder(encoder);
    fileAppender.start();
    
    logger.addAppender(fileAppender);
  }
  
  @Provides
  @Singleton
  @Named(YAML_MAPPER)
  public ObjectMapper getYamlObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    return mapper;
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
  
  @Provides
  @Singleton
  @Named(RUNNER_TEMPLATE)
  public Mustache providesRunnerTemplate(MustacheFactory factory) {
    return factory.compile(RUNNER_TEMPLATE);
  }

  @Provides
  @Singleton
  @Named(ENVIRONMENT_TEMPLATE)
  public Mustache providesEnvironmentTemplate(MustacheFactory factory) {
    return factory.compile(ENVIRONMENT_TEMPLATE);
  }
  
  @Provides
  @Singleton
  public MustacheFactory providesMustacheFactory() {
    return new DefaultMustacheFactory();
  }
  
}
