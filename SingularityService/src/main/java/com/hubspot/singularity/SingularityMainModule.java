package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.net.HostAndPort;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.guice.DropwizardObjectMapperProvider;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;
import com.hubspot.singularity.hooks.SingularityWebhookPoller;
import com.hubspot.singularity.hooks.SingularityWebhookSender;
import com.hubspot.singularity.sentry.NotifyingExceptionMapper;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.sentry.SingularityExceptionNotifierManaged;
import com.hubspot.singularity.smtp.JadeTemplateLoader;
import com.hubspot.singularity.smtp.MailTemplateHelpers;
import com.hubspot.singularity.smtp.SingularityMailRecordCleaner;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.hubspot.singularity.smtp.SingularitySmtpSender;
import com.ning.http.client.AsyncHttpClient;

import de.neuland.jade4j.parser.Parser;
import de.neuland.jade4j.parser.node.Node;
import de.neuland.jade4j.template.JadeTemplate;


public class SingularityMainModule implements Module {

  public static final String HOSTNAME_PROPERTY = "singularity.hostname";
  public static final String HTTP_PORT_PROPERTY = "singularity.http.port";

  public static final String TASK_TEMPLATE = "task.template";
  public static final String REQUEST_IN_COOLDOWN_TEMPLATE = "request.in.cooldown.template";
  public static final String REQUEST_MODIFIED_TEMPLATE = "request.modified.template";
  public static final String RATE_LIMITED_TEMPLATE = "rate.limited.template";

  public static final String SERVER_ID_PROPERTY = "singularity.server.id";
  public static final String HOST_ADDRESS_PROPERTY = "singularity.host.address";

  public static final String HTTP_HOST_AND_PORT = "http.host.and.port";

  public static final String SINGULARITY_URI_BASE = "_singularity_uri_base";

  public static final String HEALTHCHECK_THREADPOOL_NAME = "_healthcheck_threadpool";
  public static final Named HEALTHCHECK_THREADPOOL_NAMED = Names.named(HEALTHCHECK_THREADPOOL_NAME);

  public static final String NEW_TASK_THREADPOOL_NAME = "_new_task_threadpool";
  public static final Named NEW_TASK_THREADPOOL_NAMED = Names.named(NEW_TASK_THREADPOOL_NAME);

  public static final String BARAGON_HTTP_CLIENT = "baragon.http.client";

  private final SingularityConfiguration configuration;

  public SingularityMainModule(final SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(HostAndPort.class).annotatedWith(named(HTTP_HOST_AND_PORT)).toProvider(SingularityHostAndPortProvider.class).in(Scopes.SINGLETON);

    binder.bind(LeaderLatch.class).to(SingularityLeaderLatch.class).in(Scopes.SINGLETON);
    binder.bind(CuratorFramework.class).toProvider(SingularityCuratorProvider.class).in(Scopes.SINGLETON);

    Multibinder<ConnectionStateListener> connectionStateListeners = Multibinder.newSetBinder(binder, ConnectionStateListener.class);
    connectionStateListeners.addBinding().to(SingularityAbort.class).in(Scopes.SINGLETON);

    Multibinder<LeaderLatchListener> leaderLatchListeners = Multibinder.newSetBinder(binder, LeaderLatchListener.class);
    leaderLatchListeners.addBinding().to(SingularityLeaderController.class).in(Scopes.SINGLETON);

    binder.bind(SingularityDriverManager.class).in(Scopes.SINGLETON);
    binder.bind(SingularityLeaderController.class).in(Scopes.SINGLETON);
    binder.bind(SingularityMailer.class).in(Scopes.SINGLETON);
    binder.bind(SingularitySmtpSender.class).in(Scopes.SINGLETON);
    binder.bind(MailTemplateHelpers.class).in(Scopes.SINGLETON);
    binder.bind(SingularityExceptionNotifier.class).in(Scopes.SINGLETON);
    binder.bind(LoadBalancerClient.class).to(LoadBalancerClientImpl.class).in(Scopes.SINGLETON);
    binder.bind(SingularityMailRecordCleaner.class).in(Scopes.SINGLETON);
    binder.bind(BaragonProvider.class).in(Scopes.SINGLETON);

    binder.bind(SingularityWebhookPoller.class).in(Scopes.SINGLETON);

    binder.bind(MesosClient.class).in(Scopes.SINGLETON);

    binder.bind(SingularityAbort.class).in(Scopes.SINGLETON);
    binder.bind(SingularityExceptionNotifierManaged.class).in(Scopes.SINGLETON);
    binder.bind(SingularityWebhookSender.class).in(Scopes.SINGLETON);

    binder.bind(NotifyingExceptionMapper.class).in(Scopes.SINGLETON);

    binder.bind(ObjectMapper.class).toProvider(DropwizardObjectMapperProvider.class).in(Scopes.SINGLETON);

    binder.bind(AsyncHttpClient.class).to(SingularityHttpClient.class).in(Scopes.SINGLETON);
    binder.bind(ServerProvider.class).in(Scopes.SINGLETON);

    binder.bind(SingularityDropwizardHealthcheck.class).in(Scopes.SINGLETON);
    binder.bindConstant().annotatedWith(Names.named(SERVER_ID_PROPERTY)).to(UUID.randomUUID().toString());

    binder.bind(SingularityManagedScheduledExecutorServiceFactory.class).in(Scopes.SINGLETON);

    binder.bind(ScheduledExecutorService.class).annotatedWith(HEALTHCHECK_THREADPOOL_NAMED).toProvider(new SingularityManagedScheduledExecutorServiceProvider(configuration.getHealthcheckStartThreads(),
        configuration.getThreadpoolShutdownDelayInSeconds(),
        "healthcheck")).in(Scopes.SINGLETON);

    binder.bind(ScheduledExecutorService.class).annotatedWith(NEW_TASK_THREADPOOL_NAMED).toProvider(new SingularityManagedScheduledExecutorServiceProvider(configuration.getCheckNewTasksScheduledThreads(),
        configuration.getThreadpoolShutdownDelayInSeconds(),
        "check-new-task")).in(Scopes.SINGLETON);

    try {
      binder.bindConstant().annotatedWith(Names.named(HOST_ADDRESS_PROPERTY)).to(JavaUtils.getHostAddress());
    } catch (SocketException e) {
      throw Throwables.propagate(e);
    }
  }

  public static class SingularityHostAndPortProvider implements Provider<HostAndPort> {

    private final String hostname;
    private final int httpPort;

    @Inject
    SingularityHostAndPortProvider(final SingularityConfiguration configuration, @Named(HOST_ADDRESS_PROPERTY) String hostAddress) {
      checkNotNull(configuration, "configuration is null");
      this.hostname = configuration.getHostname().or(JavaUtils.getHostName().or(hostAddress));

      SimpleServerFactory simpleServerFactory = (SimpleServerFactory) configuration.getServerFactory();
      HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();

      this.httpPort = httpFactory.getPort();
    }

    @Override
    public HostAndPort get() {
      return HostAndPort.fromParts(hostname, httpPort);
    }
  }

  @Provides
  @Named(SINGULARITY_URI_BASE)
  String getSingularityUriBase(final SingularityConfiguration configuration) {
    final String singularityUiPrefix = configuration.getUiConfiguration().getBaseUrl().or(((SimpleServerFactory) configuration.getServerFactory()).getApplicationContextPath());
    return (singularityUiPrefix.endsWith("/")) ?  singularityUiPrefix.substring(0, singularityUiPrefix.length() - 1) : singularityUiPrefix;
  }

  @Provides
  @Singleton
  public ZooKeeperConfiguration zooKeeperConfiguration(final SingularityConfiguration config) {
    return config.getZooKeeperConfiguration();
  }

  @Provides
  @Singleton
  public Optional<SentryConfiguration> sentryConfiguration(final SingularityConfiguration config) {
    return config.getSentryConfiguration();
  }

  @Provides
  @Singleton
  public Optional<BaragonServiceClient> baragonServiceClient(final SingularityConfiguration config, BaragonProvider baragonProvider) {
    if (config.getLoadBalancerConfig().isPresent() || config.getLoadBalancerUri() != null) {
      return Optional.of(baragonProvider.create());
    } else {
      return Optional.absent();
    }
  }

  @Provides
  @Named(BARAGON_HTTP_CLIENT)
  public HttpClient baragonHttpClient(final SingularityConfiguration config, ObjectMapper objectMapper) {
    HttpConfig.Builder httpBuilder = HttpConfig.newBuilder().setObjectMapper(objectMapper);

    if (config.getLoadBalancerConfig().isPresent()) {
      httpBuilder.setRequestTimeoutSeconds(config.getLoadBalancerConfig().get().getRequestTimeoutMs());
    } else {
      httpBuilder.setRequestTimeoutSeconds((config.getLoadBalancerRequestTimeoutMillis()));
    }
    return new NingHttpClient(httpBuilder.build());
  }

  @Provides
  @Singleton
  public Optional<S3Service> s3Service(Optional<S3Configuration> config) throws S3ServiceException {
    if (!config.isPresent()) {
      return Optional.absent();
    }

    return Optional.<S3Service>of(new RestS3Service(new AWSCredentials(config.get().getS3AccessKey(), config.get().getS3SecretKey())));
  }

  @Provides
  @Singleton
  public MesosConfiguration mesosConfiguration(final SingularityConfiguration config) {
    return config.getMesosConfiguration();
  }

  @Provides
  @Singleton
  public CustomExecutorConfiguration customExecutorConfiguration(final SingularityConfiguration config) {
    return config.getCustomExecutorConfiguration();
  }

  @Provides
  @Singleton
  public Optional<SMTPConfiguration> smtpConfiguration(final SingularityConfiguration config) {
    return config.getSmtpConfiguration();
  }

  @Provides
  @Singleton
  public Optional<S3Configuration> s3Configuration(final SingularityConfiguration config) {
    return config.getS3Configuration();
  }

  private JadeTemplate getJadeTemplate(String name) throws IOException {
    Parser parser = new Parser("templates/" + name, JadeTemplateLoader.JADE_LOADER);
    Node root = parser.parse();

    final JadeTemplate jadeTemplate = new JadeTemplate();

    jadeTemplate.setTemplateLoader(JadeTemplateLoader.JADE_LOADER);
    jadeTemplate.setRootNode(root);

    return jadeTemplate;
  }

  @Provides
  @Singleton
  @Named(TASK_TEMPLATE)
  public JadeTemplate getTaskTemplate() throws IOException {
    return getJadeTemplate("task.jade");
  }

  @Provides
  @Singleton
  @Named(REQUEST_IN_COOLDOWN_TEMPLATE)
  public JadeTemplate getRequestPausedTemplate() throws IOException {
    return getJadeTemplate("request_in_cooldown.jade");
  }

  @Provides
  @Singleton
  @Named(REQUEST_MODIFIED_TEMPLATE)
  public JadeTemplate getRequestModifiedTemplate() throws IOException {
    return getJadeTemplate("request_modified.jade");
  }

  @Provides
  @Singleton
  @Named(RATE_LIMITED_TEMPLATE)
  public JadeTemplate getRateLimitedTemplate() throws IOException {
    return getJadeTemplate("rate_limited.jade");
  }

}
