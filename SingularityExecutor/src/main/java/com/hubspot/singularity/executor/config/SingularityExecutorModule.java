package com.hubspot.singularity.executor.config;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.handlebars.BashEscapedHelper;
import com.hubspot.singularity.executor.handlebars.IfPresentHelper;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseLogging;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerClient;

public class SingularityExecutorModule extends AbstractModule {
  public static final String RUNNER_TEMPLATE = "runner.sh";
  public static final String ENVIRONMENT_TEMPLATE = "deploy.env";
  public static final String LOGROTATE_TEMPLATE = "logrotate.conf";
  public static final String DOCKER_TEMPLATE = "docker.sh";
  public static final String LOCAL_DOWNLOAD_HTTP_CLIENT = "SingularityExecutorModule.local.download.http.client";
  public static final String ALREADY_SHUT_DOWN = "already.shut.down";

  @Override
  protected void configure() {

  }

  @Provides
  @Singleton
  @Named(LOCAL_DOWNLOAD_HTTP_CLIENT)
  public AsyncHttpClient providesHttpClient(SingularityExecutorConfiguration configuration) {
    AsyncHttpClientConfig.Builder configBldr = new AsyncHttpClientConfig.Builder();
    configBldr.setRequestTimeoutInMs((int) configuration.getLocalDownloadServiceTimeoutMillis());
    configBldr.setIdleConnectionTimeoutInMs((int) configuration.getLocalDownloadServiceTimeoutMillis());

    return new AsyncHttpClient(configBldr.build());
  }

  @Provides
  @Singleton
  @Named(RUNNER_TEMPLATE)
  public Template providesRunnerTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(RUNNER_TEMPLATE);
  }

  @Provides
  @Singleton
  @Named(ENVIRONMENT_TEMPLATE)
  public Template providesEnvironmentTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(ENVIRONMENT_TEMPLATE);
  }

  @Provides
  @Singleton
  @Named(LOGROTATE_TEMPLATE)
  public Template providesLogrotateTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(LOGROTATE_TEMPLATE);
  }

  @Provides
  @Singleton
  @Named(DOCKER_TEMPLATE)
  public Template providesDockerTempalte(Handlebars handlebars) throws IOException {
    return handlebars.compile(DOCKER_TEMPLATE);
  }

  @Provides
  @Singleton
  public Handlebars providesHandlebars() {
    SingularityRunnerBaseLogging.quietEagerLogging();  // handlebars emits DEBUG logs before logger is properly configured
    final Handlebars handlebars = new Handlebars();

    handlebars.registerHelper(BashEscapedHelper.NAME, new BashEscapedHelper());
    handlebars.registerHelper(IfPresentHelper.NAME, new IfPresentHelper());

    return handlebars;
  }

  @Provides
  @Singleton
  public DockerClient providesDockerClient() {
    return new DefaultDockerClient("unix:///var/run/docker.sock");
  }

  @Provides
  @Singleton
  @Named(ALREADY_SHUT_DOWN)
  public AtomicBoolean providesAlreadyShutDown() {
    return new AtomicBoolean(false);
  }
}
