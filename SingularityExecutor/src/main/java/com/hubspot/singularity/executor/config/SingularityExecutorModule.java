package com.hubspot.singularity.executor.config;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.singularity.executor.SingularityExecutorProcessKiller;

public class SingularityExecutorModule extends AbstractModule {

  public static final String ARTIFACT_CACHE_DIRECTORY = "hubspot.mesos.executor.artifact.cache.directory";

  public static final String RUNNER_TEMPLATE = "runner.sh";
  public static final String ENVIRONMENT_TEMPLATE = "deploy.env";

  public static final String JSON_MAPPER = "object.mapper.json";
  
  public static final String ROOT_LOG_PATH = "root.log.path";
  public static final String TASK_EXECUTOR_JAVA_LOG_PATH = "task.executor.java.log.path";
  public static final String TASK_EXECUTOR_BASH_LOG_PATH = "task.executor.bash.log.path";
  public static final String TASK_SERVICE_LOG_PATH = "task.service.log.path";
  
  public static final String DEFAULT_USER = "default.user";
  
  public static final String HARD_KILL_AFTER_MILLIS = "executor.hard.kill.after.millis";
  public static final String NUM_CORE_KILL_THREADS = "executor.num.core.kill.threads";
  
  public static final String MAX_TASK_MESSAGE_LENGTH = "executor.status.update.max.task.message.length";
  
  @Override
  protected void configure() {
    bindPropertiesFile("/etc/singularity.executor.properties");
    
    bind(SingularityExecutorLogging.class).in(Scopes.SINGLETON);
    bind(SingularityTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularityExecutorProcessKiller.class).in(Scopes.SINGLETON);
  }
  
  private void bindDefaults(Properties properties) {
    properties.put(TASK_EXECUTOR_BASH_LOG_PATH, "executor.bash.log");
    properties.put(TASK_EXECUTOR_JAVA_LOG_PATH, "executor.java.log");
    properties.put(TASK_SERVICE_LOG_PATH, "service.log");
    properties.put(HARD_KILL_AFTER_MILLIS, Long.toString(TimeUnit.MINUTES.toMillis(3)));
    properties.put(NUM_CORE_KILL_THREADS, "1");
    properties.put(MAX_TASK_MESSAGE_LENGTH, "80");
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
