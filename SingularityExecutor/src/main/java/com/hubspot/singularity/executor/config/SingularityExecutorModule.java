package com.hubspot.singularity.executor.config;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.handlebars.BashEscapedHelper;
import com.hubspot.singularity.executor.handlebars.EscapeNewLinesAndQuotesHelper;
import com.hubspot.singularity.executor.handlebars.IfHasNewLinesOrBackticksHelper;
import com.hubspot.singularity.executor.handlebars.IfPresentHelper;
import com.hubspot.singularity.executor.handlebars.ShellQuoteHelper;
import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseLogging;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import com.ning.http.client.extra.ThrottleRequestFilter;
import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DefaultDockerClient.Builder;
import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.messages.RegistryAuth;

public class SingularityExecutorModule extends AbstractModule {

  public static final String RUNNER_TEMPLATE = "runner.sh";
  public static final String ENVIRONMENT_TEMPLATE = "deploy.env";
  public static final String LOGROTATE_TEMPLATE = "logrotate.conf";
  public static final String LOGROTATE_CRON_TEMPLATE = "logrotate.cron";
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
    configBldr.addRequestFilter(new ThrottleRequestFilter(configuration.getLocalDownloadServiceMaxConnections()));

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
  @Named(LOGROTATE_CRON_TEMPLATE)
  public Template providesLogrotateCronTemplate(Handlebars handlebars) throws IOException {
    return handlebars.compile(LOGROTATE_CRON_TEMPLATE);
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
    handlebars.registerHelper(ShellQuoteHelper.NAME, new ShellQuoteHelper());
    handlebars.registerHelper(IfPresentHelper.NAME, new IfPresentHelper());
    handlebars.registerHelper(IfHasNewLinesOrBackticksHelper.NAME, new IfHasNewLinesOrBackticksHelper());
    handlebars.registerHelper(EscapeNewLinesAndQuotesHelper.NAME, new EscapeNewLinesAndQuotesHelper());

    return handlebars;
  }

  @Provides
  @Singleton
  public DockerClient providesDockerClient(SingularityExecutorConfiguration configuration) {
    Builder dockerClientBuilder = DefaultDockerClient.builder()
      .uri(URI.create("unix://localhost/var/run/docker.sock"))
      .connectionPoolSize(configuration.getDockerClientConnectionPoolSize());

    if(configuration.getDockerAuthConfig().isPresent()) {
      SingularityExecutorDockerAuthConfig authConfig = configuration.getDockerAuthConfig().get();

      if(authConfig.isFromDockerConfig()) {
        try {
          dockerClientBuilder.registryAuth(RegistryAuth.fromDockerConfig().build());

        } catch(IOException e) {
          throw Throwables.propagate(e);
        }
      }
      else {
        dockerClientBuilder.registryAuth(RegistryAuth.builder()
                  .email(authConfig.getEmail())
                  .username(authConfig.getUsername())
                  .password(authConfig.getPassword())
                  .serverAddress(authConfig.getServerAddress())
                  .build());
      }
    }

    return dockerClientBuilder.build();
  }

  @Provides
  @Singleton
  @Named(ALREADY_SHUT_DOWN)
  public AtomicBoolean providesAlreadyShutDown() {
    return new AtomicBoolean(false);
  }
}
