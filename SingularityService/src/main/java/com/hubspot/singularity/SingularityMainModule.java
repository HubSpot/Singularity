package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.HistoryPurgingConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.config.S3GroupOverrideConfiguration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.SingularityTaskMetadataConfiguration;
import com.hubspot.singularity.config.UIConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.guice.DropwizardMetricRegistryProvider;
import com.hubspot.singularity.guice.DropwizardObjectMapperProvider;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;
import com.hubspot.singularity.hooks.SingularityWebhookPoller;
import com.hubspot.singularity.hooks.SingularityWebhookSender;
import com.hubspot.singularity.metrics.SingularityGraphiteReporterManaged;
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
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;


public class SingularityMainModule implements Module {

  public static final String TASK_TEMPLATE = "task.template";
  public static final String REQUEST_IN_COOLDOWN_TEMPLATE = "request.in.cooldown.template";
  public static final String REQUEST_MODIFIED_TEMPLATE = "request.modified.template";
  public static final String RATE_LIMITED_TEMPLATE = "rate.limited.template";

  public static final String SERVER_ID_PROPERTY = "singularity.server.id";
  public static final String HOST_NAME_PROPERTY = "singularity.host.name";

  public static final String HTTP_HOST_AND_PORT = "http.host.and.port";

  public static final String SINGULARITY_URI_BASE = "_singularity_uri_base";

  public static final String HEALTHCHECK_THREADPOOL_NAME = "_healthcheck_threadpool";
  public static final Named HEALTHCHECK_THREADPOOL_NAMED = Names.named(HEALTHCHECK_THREADPOOL_NAME);

  public static final String NEW_TASK_THREADPOOL_NAME = "_new_task_threadpool";
  public static final Named NEW_TASK_THREADPOOL_NAMED = Names.named(NEW_TASK_THREADPOOL_NAME);

  public static final String CURRENT_HTTP_REQUEST = "_singularity_current_http_request";

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

    binder.bind(SingularityWebhookPoller.class).in(Scopes.SINGLETON);

    binder.bind(MesosClient.class).in(Scopes.SINGLETON);

    binder.bind(SingularityAbort.class).in(Scopes.SINGLETON);
    binder.bind(SingularityExceptionNotifierManaged.class).in(Scopes.SINGLETON);
    binder.bind(SingularityWebhookSender.class).in(Scopes.SINGLETON);

    binder.bind(NotifyingExceptionMapper.class).in(Scopes.SINGLETON);

    binder.bind(ObjectMapper.class).toProvider(DropwizardObjectMapperProvider.class).in(Scopes.SINGLETON);
    binder.bind(MetricRegistry.class).toProvider(DropwizardMetricRegistryProvider.class).in(Scopes.SINGLETON);

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

    binder.bind(SingularityGraphiteReporterManaged.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Named(HOST_NAME_PROPERTY)
  @Singleton
  public String getHostname(final SingularityConfiguration configuration) {
    if (configuration.getHostname().isPresent()) {
      return configuration.getHostname().get();
    }

    try {
      InetAddress addr = InetAddress.getLocalHost();

      return addr.getHostName();
    } catch (UnknownHostException e) {
      throw new RuntimeException("No local hostname found, unable to start without functioning local networking (or configured hostname)", e);
    }
  }

  public static class SingularityHostAndPortProvider implements Provider<HostAndPort> {

    private final String hostname;
    private final int httpPort;

    @Inject
    SingularityHostAndPortProvider(final SingularityConfiguration configuration, @Named(HOST_NAME_PROPERTY) String hostname) {
      checkNotNull(configuration, "configuration is null");
      this.hostname = configuration.getHostname().or(hostname);

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
  public SingularityTaskMetadataConfiguration taskMetadataConfiguration(SingularityConfiguration config) {
    return config.getTaskMetadataConfiguration();
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
  public Map<String, S3Service> s3ServiceGroupOverrides(Optional<S3Configuration> config) throws S3ServiceException {
    if (!config.isPresent() || config.get().getGroupOverrides().isEmpty()) {
      return Collections.emptyMap();
    }

    final ImmutableMap.Builder<String, S3Service> s3ServiceBuilder = ImmutableMap.builder();

    for (Map.Entry<String, S3GroupOverrideConfiguration> entry : config.get().getGroupOverrides().entrySet()) {
      s3ServiceBuilder.put(entry.getKey(), new RestS3Service(new AWSCredentials(entry.getValue().getS3AccessKey(), entry.getValue().getS3SecretKey())));
    }

    return s3ServiceBuilder.build();
  }

  @Provides
  @Singleton
  public MesosConfiguration mesosConfiguration(final SingularityConfiguration config) {
    return config.getMesosConfiguration();
  }

  @Provides
  @Singleton
  public UIConfiguration uiConfiguration(final SingularityConfiguration config) {
    return config.getUiConfiguration();
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

  @Provides
  @Singleton
  public HistoryPurgingConfiguration historyPurgingConfiguration(final SingularityConfiguration config) {
    return config.getHistoryPurgingConfiguration();
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

  @Provides
  @Named(CURRENT_HTTP_REQUEST)
  public Optional<HttpServletRequest> providesUrl(Provider<HttpServletRequest> requestProvider) {
    try {
      return Optional.of(requestProvider.get());
    } catch (ProvisionException pe) {  // this will happen if we're not in the REQUEST scope
      return Optional.absent();
    }
  }
}
