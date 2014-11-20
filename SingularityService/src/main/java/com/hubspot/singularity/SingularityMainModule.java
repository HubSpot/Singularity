package com.hubspot.singularity;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.inject.name.Names.named;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;

import java.io.IOException;
import java.net.SocketException;
import java.util.UUID;

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
import com.google.common.base.Strings;
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
import com.hubspot.singularity.smtp.JadeHelper;
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

  public static final String TASK_COMPLETED_TEMPLATE = "task.completed.template";
  public static final String REQUEST_IN_COOLDOWN_TEMPLATE = "request.in.cooldown.template";
  public static final String REQUEST_MODIFIED_TEMPLATE = "request.modified.template";
  public static final String RATE_LIMITED_TEMPLATE = "rate.limited.template";

  public static final String SERVER_ID_PROPERTY = "singularity.server.id";
  public static final String HOST_ADDRESS_PROPERTY = "singularity.host.address";

  public static final String HTTP_HOST_AND_PORT = "http.host.and.port";

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

    binder.bind(AsyncHttpClient.class).to(SingularityHttpClient.class).in(Scopes.SINGLETON);

    binder.bind(ServerProvider.class).in(Scopes.SINGLETON);

    binder.bind(SingularityDropwizardHealthcheck.class).in(Scopes.SINGLETON);
    binder.bindConstant().annotatedWith(Names.named(SERVER_ID_PROPERTY)).to(UUID.randomUUID().toString());

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
      this.hostname = !Strings.isNullOrEmpty(configuration.getHostname()) ? configuration.getHostname() : JavaUtils.getHostName().or(hostAddress);

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
  public Optional<SMTPConfiguration> smtpConfiguration(final SingularityConfiguration config) {
    return config.getSmtpConfiguration();
  }

  @Provides
  @Singleton
  public Optional<S3Configuration> s3Configuration(final SingularityConfiguration config) {
    return config.getS3Configuration();
  }

  private JadeTemplate getJadeTemplate(String name) throws IOException {
    Parser parser = new Parser("templates/" + name, JadeHelper.JADE_LOADER);
    Node root = parser.parse();

    final JadeTemplate jadeTemplate = new JadeTemplate();

    jadeTemplate.setTemplateLoader(JadeHelper.JADE_LOADER);
    jadeTemplate.setRootNode(root);

    return jadeTemplate;
  }

  @Provides
  @Singleton
  @Named(TASK_COMPLETED_TEMPLATE)
  public JadeTemplate getTaskCompletedTemplate() throws IOException {
    return getJadeTemplate("task_completed.jade");
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
