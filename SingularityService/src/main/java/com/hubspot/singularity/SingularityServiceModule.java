package com.hubspot.singularity;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.jets3t.service.S3Service;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SentryConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.ExecutorIdGenerator;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SandboxManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.TaskRequestManager;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.data.history.DeployHistoryHelper;
import com.hubspot.singularity.data.history.HistoryJDBI;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.JDBIHistoryManager;
import com.hubspot.singularity.data.history.NoopHistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.data.history.SingularityDeployHistoryPersister;
import com.hubspot.singularity.data.history.SingularityHistoryPersister;
import com.hubspot.singularity.data.history.SingularityRequestHistoryPersister;
import com.hubspot.singularity.data.history.SingularityTaskHistoryPersister;
import com.hubspot.singularity.data.history.TaskHistoryHelper;
import com.hubspot.singularity.data.transcoders.SingularityDeployHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployKeyTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployMarkerTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployStatisticsTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityDeployWebhookTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityKilledTaskIdRecordTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityLoadBalancerUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingDeployTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingRequestTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRackTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestDeployStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityRequestWithStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularitySlaveTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHealthcheckResultTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskStatusTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityWebhookTranscoder;
import com.hubspot.singularity.data.zkmigrations.LastTaskStatusMigration;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigration;
import com.hubspot.singularity.data.zkmigrations.ZkDataMigrationRunner;
import com.hubspot.singularity.guice.GuicePropertyFilteringMessageBodyWriter;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;
import com.hubspot.singularity.hooks.SingularityWebhookPoller;
import com.hubspot.singularity.hooks.SingularityWebhookSender;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.mesos.SingularityLogSupport;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;
import com.hubspot.singularity.mesos.SingularityMesosTaskBuilder;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager;
import com.hubspot.singularity.mesos.SingularityStartup;
import com.hubspot.singularity.resources.DeployResource;
import com.hubspot.singularity.resources.HistoryResource;
import com.hubspot.singularity.resources.IndexResource;
import com.hubspot.singularity.resources.RackResource;
import com.hubspot.singularity.resources.RequestResource;
import com.hubspot.singularity.resources.S3LogResource;
import com.hubspot.singularity.resources.SandboxResource;
import com.hubspot.singularity.resources.SlaveResource;
import com.hubspot.singularity.resources.StateResource;
import com.hubspot.singularity.resources.TaskResource;
import com.hubspot.singularity.resources.TestResource;
import com.hubspot.singularity.resources.WebhookResource;
import com.hubspot.singularity.scheduler.SingularityCleaner;
import com.hubspot.singularity.scheduler.SingularityCleanupPoller;
import com.hubspot.singularity.scheduler.SingularityCooldown;
import com.hubspot.singularity.scheduler.SingularityCooldownChecker;
import com.hubspot.singularity.scheduler.SingularityCooldownPoller;
import com.hubspot.singularity.scheduler.SingularityDeployChecker;
import com.hubspot.singularity.scheduler.SingularityDeployHealthHelper;
import com.hubspot.singularity.scheduler.SingularityDeployPoller;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliationPoller;
import com.hubspot.singularity.sentry.NotifyingExceptionMapper;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.sentry.SingularityExceptionNotifierManaged;
import com.hubspot.singularity.smtp.JadeHelper;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.ning.http.client.AsyncHttpClient;

import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;

import de.neuland.jade4j.parser.Parser;
import de.neuland.jade4j.parser.node.Node;
import de.neuland.jade4j.template.JadeTemplate;

public class SingularityServiceModule extends AbstractModule {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityServiceModule.class);

  private static final String LEADER_PATH = "/leader";

  public static final String HOSTNAME_PROPERTY = "singularity.hostname";
  public static final String HTTP_PORT_PROPERTY = "singularity.http.port";

  public static final String TASK_COMPLETED_TEMPLATE = "task.completed.template";
  public static final String REQUEST_IN_COOLDOWN_TEMPLATE = "request.in.cooldown.template";
  public static final String REQUEST_MODIFIED_TEMPLATE = "request.modified.template";

  public static final String SERVER_ID_PROPERTY = "singularity.server.id";

  @Override
  protected void configure() {
    bind(SingularityDriverManager.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderController.class).in(Scopes.SINGLETON);
    bind(SingularityStatePoller.class).in(Scopes.SINGLETON);
    bind(SingularityCloser.class).in(Scopes.SINGLETON);
    bind(SingularityMailer.class).in(Scopes.SINGLETON);
    bind(SingularityLogSupport.class).in(Scopes.SINGLETON);
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityExceptionNotifier.class).in(Scopes.SINGLETON);
    bind(LoadBalancerClient.class).to(LoadBalancerClientImpl.class).in(Scopes.SINGLETON);
    bindMethodInterceptorForStringTemplateClassLoaderWorkaround();
    bind(SingularityWebhookPoller.class).in(Scopes.SINGLETON);
    bind(SingularityHistoryPersister.class).in(Scopes.SINGLETON);

    bind(StateManager.class).in(Scopes.SINGLETON);
    bind(TaskManager.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHistoryTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityKilledTaskIdRecordTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityLoadBalancerUpdateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityPendingTaskIdTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskCleanupTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHealthcheckResultTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryUpdateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployKeyTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployMarkerTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployStatisticsTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityPendingDeployTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityPendingRequestTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRackTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestCleanupTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestDeployStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestWithStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestHistoryTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployWebhookTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityWebhookTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskIdTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskStatusTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityMesosScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityMesosTaskBuilder.class).in(Scopes.SINGLETON);
    bind(SingularityMesosSchedulerDelegator.class).in(Scopes.SINGLETON);
    bind(SingularityHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryPersister.class).in(Scopes.SINGLETON);
    bind(SingularityStartup.class).in(Scopes.SINGLETON);
    bind(SingularityCleanupPoller.class).in(Scopes.SINGLETON);
    bind(SingularityCooldownPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDeployPoller.class).in(Scopes.SINGLETON);
    bind(SingularityCooldownPoller.class).in(Scopes.SINGLETON);
    bind(SingularityDeployPoller.class).in(Scopes.SINGLETON);
    bind(SingularityTaskReconciliationPoller.class).in(Scopes.SINGLETON);
    bind(ZkDataMigrationRunner.class).in(Scopes.SINGLETON);
    bind(LastTaskStatusMigration.class).in(Scopes.SINGLETON);
    bind(SingularityScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityCooldownChecker.class).in(Scopes.SINGLETON);
    bind(SingularityDeployChecker.class).in(Scopes.SINGLETON);
    bind(SingularityTaskReconciliation.class).in(Scopes.SINGLETON);
    bind(MesosClient.class).in(Scopes.SINGLETON);
    bind(DeployManager.class).in(Scopes.SINGLETON);
    bind(RackManager.class).in(Scopes.SINGLETON);
    bind(RequestManager.class).in(Scopes.SINGLETON);
    bind(SlaveManager.class).in(Scopes.SINGLETON);
    bind(TaskRequestManager.class).in(Scopes.SINGLETON);
    bind(MetadataManager.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveAndRackManager.class).in(Scopes.SINGLETON);
    bind(SingularityCleaner.class).in(Scopes.SINGLETON);
    bind(SingularityCooldown.class).in(Scopes.SINGLETON);
    bind(SingularityDeployHealthHelper.class).in(Scopes.SINGLETON);
    bind(SingularityCooldown.class).in(Scopes.SINGLETON);

    bind(SandboxManager.class).in(Scopes.SINGLETON);
    bind(SingularityValidator.class).in(Scopes.SINGLETON);

    bind(ExecutorIdGenerator.class).in(Scopes.SINGLETON);
    bind(SingularityAbort.class).in(Scopes.SINGLETON);
    bind(SingularityDriver.class).in(Scopes.SINGLETON);
    bind(SingularityHealthchecker.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderController.class).in(Scopes.SINGLETON);
    bind(SingularityLogSupport.class).in(Scopes.SINGLETON);
    bind(SingularityMailer.class).in(Scopes.SINGLETON);
    bind(SingularityNewTaskChecker.class).in(Scopes.SINGLETON);
    bind(SingularityExceptionNotifierManaged.class).in(Scopes.SINGLETON);
    bind(WebhookManager.class).in(Scopes.SINGLETON);
    bind(SingularityWebhookSender.class).in(Scopes.SINGLETON);

    bind(NotifyingExceptionMapper.class).in(Scopes.SINGLETON);
    bind(GuicePropertyFilteringMessageBodyWriter.class).in(Scopes.SINGLETON);
    bind(TaskHistoryHelper.class).in(Scopes.SINGLETON);
    bind(RequestHistoryHelper.class).in(Scopes.SINGLETON);
    bind(DeployHistoryHelper.class).in(Scopes.SINGLETON);
    bind(SingularityRequestHistoryPersister.class).in(Scopes.SINGLETON);

    bind(NoopHistoryManager.class).in(Scopes.SINGLETON);
    bind(JDBIHistoryManager.class).in(Scopes.SINGLETON);

    // At least WebhookResource must not be a singleton. Make all of them
    // not singletons, just in case.
    bind(DeployResource.class);
    bind(HistoryResource.class);
    bind(IndexResource.class);
    bind(RackResource.class);
    bind(RequestResource.class);
    bind(S3LogResource.class);
    bind(SandboxResource.class);
    bind(SlaveResource.class);
    bind(StateResource.class);
    bind(TaskResource.class);
    bind(TestResource.class);
    bind(WebhookResource.class);

    bind(SingularitySchedulerStateCache.class);

    bindMethodInterceptorForStringTemplateClassLoaderWorkaround();

    bindConstant().annotatedWith(Names.named(SERVER_ID_PROPERTY)).to(UUID.randomUUID().toString());
  }

  private void bindMethodInterceptorForStringTemplateClassLoaderWorkaround() {
    bindInterceptor(Matchers.subclassesOf(JDBIHistoryManager.class), Matchers.any(), new MethodInterceptor() {

      @Override
      public Object invoke(final MethodInvocation invocation) throws Throwable {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (cl == null) {
          Thread.currentThread().setContextClassLoader(ClassLoader.getSystemClassLoader());
        }

        try {
          return invocation.proceed();
        } finally {
          Thread.currentThread().setContextClassLoader(cl);
        }
      }
    });
  }

  @Provides
  public List<SingularityStartable> getStartableSingletons(final Injector injector) {
    final List<SingularityStartable> startables = Lists.newArrayList();

    for (final Map.Entry<Key<?>, Binding<?>> bindingEntry : injector.getAllBindings().entrySet()) {
      final Key<?> key = bindingEntry.getKey();
      if (SingularityStartable.class.isAssignableFrom(key.getTypeLiteral().getRawType())) {
        @SuppressWarnings("unchecked")
        final Binding<SingularityStartable> binding = (Binding<SingularityStartable>) bindingEntry.getValue();

        if (Scopes.isSingleton(binding)) {
          final SingularityStartable startable = binding.getProvider().get();
          startables.add(startable);
        }
      }
    }

    return startables;
  }

  @Provides
  @Singleton
  public static List<ZkDataMigration> getZkDataMigrations(LastTaskStatusMigration lastTaskStatusMigration) {
    return ImmutableList.<ZkDataMigration> of(lastTaskStatusMigration);
  }

  private static ObjectMapper createObjectMapper() {
    return Jackson.newObjectMapper()
        .setSerializationInclusion(Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new ProtobufModule());
  }

  public static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return OBJECT_MAPPER;
  }

  @Provides
  @Singleton
  public AsyncHttpClient providesAsyncHTTPClient() {
    return new AsyncHttpClient();
  }

  @Provides
  @Singleton
  public HistoryManager getHistoryManager(SingularityConfiguration configuration, Injector injector) {
    if (configuration.getDatabaseConfiguration().isPresent()) {
      return injector.getInstance(JDBIHistoryManager.class);
    }
    else {
      return injector.getInstance(NoopHistoryManager.class);
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
  public Optional<S3Service> s3Service(final Optional<S3Configuration> config) {
    if (!config.isPresent()) {
      return Optional.absent();
    }

    try {
      final S3Service s3 = new RestS3Service(new AWSCredentials(config.get().getS3AccessKey(), config.get().getS3SecretKey()));
      return Optional.of(s3);
    } catch (final Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  @Provides
  @Singleton
  @Named(HOSTNAME_PROPERTY)
  public String providesHostnameProperty(final SingularityConfiguration config) throws Exception {
    return !Strings.isNullOrEmpty(config.getHostname()) ? config.getHostname() : JavaUtils.getHostAddress();
  }

  @Provides
  @Singleton
  @Named(HTTP_PORT_PROPERTY)
  public int providesHttpPortProperty(final SingularityConfiguration config) {
    final SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
    final HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();

    return httpFactory.getPort();
  }

  @Provides
  @Singleton
  public LeaderLatch provideLeaderLatch(final CuratorFramework curator,
      @Named(SingularityServiceModule.HOSTNAME_PROPERTY) final String hostname,
      @Named(SingularityServiceModule.HTTP_PORT_PROPERTY) final int httpPort) {
    return new LeaderLatch(curator, LEADER_PATH, String.format("%s:%d", hostname, httpPort));
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

  @Provides
  @Singleton
  public CuratorFramework provideCurator(ZooKeeperConfiguration config) throws InterruptedException {
    LOG.info("Creating curator/ZK client and blocking on connection to ZK quorum {} (timeout: {})", config.getQuorum(), config.getConnectTimeoutMillis());

    final CuratorFramework client = CuratorFrameworkFactory.builder()
        .defaultData(null)
        .sessionTimeoutMs(config.getSessionTimeoutMillis())
        .connectionTimeoutMs(config.getConnectTimeoutMillis())
        .connectString(config.getQuorum())
        .retryPolicy(new ExponentialBackoffRetry(config.getRetryBaseSleepTimeMilliseconds(), config.getRetryMaxTries()))
        .build();

    client.start();

    final long start = System.currentTimeMillis();

    Preconditions.checkState(client.getZookeeperClient().blockUntilConnectedOrTimedOut());

    LOG.info("Connected to ZK after {}", JavaUtils.duration(start));

    return client.usingNamespace(config.getZkNamespace());
  }

  @Provides
  @Singleton
  public Optional<DBI> getDBI(final Environment environment, final SingularityConfiguration singularityConfiguration) throws ClassNotFoundException {
    final DBIFactory factory = new DBIFactory();
    if (singularityConfiguration.getDatabaseConfiguration().isPresent()) {
      return Optional.of(factory.build(environment, singularityConfiguration.getDatabaseConfiguration().get(), "db"));
    } else {
      return Optional.absent();
    }
  }

  @Provides
  public Optional<HistoryJDBI> getHistoryJDBI(final Optional<DBI> dbi) {
    if (dbi.isPresent()) {
      return Optional.of(dbi.get().onDemand(HistoryJDBI.class));
    } else {
      return Optional.absent();
    }
  }

  private JadeTemplate getJadeTemplate(final String name) throws IOException {
    final Parser parser = new Parser("templates/" + name, JadeHelper.JADE_LOADER);
    final Node root = parser.parse();

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
  public Optional<Raven> providesRavenIfConfigured(final SingularityConfiguration configuration) {
    if (configuration.getSentryConfiguration().isPresent()) {
      return Optional.of(RavenFactory.ravenInstance(configuration.getSentryConfiguration().get().getDsn()));
    } else {
      return Optional.absent();
    }
  }
}
