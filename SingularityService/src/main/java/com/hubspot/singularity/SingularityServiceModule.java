package com.hubspot.singularity;

import io.dropwizard.jackson.Jackson;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.setup.Environment;

import java.io.IOException;

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
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.S3Configuration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.history.HistoryJDBI;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.JDBIHistoryManager;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;
import com.hubspot.singularity.mesos.SingularityLogSupport;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import com.hubspot.singularity.smtp.JadeHelper;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.ning.http.client.AsyncHttpClient;

import de.neuland.jade4j.parser.Parser;
import de.neuland.jade4j.parser.node.Node;
import de.neuland.jade4j.template.JadeTemplate;
import net.kencochrane.raven.Raven;
import net.kencochrane.raven.RavenFactory;

public class SingularityServiceModule extends AbstractModule {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityServiceModule.class);
  
  private static final String LEADER_PATH = "/leader";
  
  public static final String MASTER_PROPERTY = "singularity.master";
  public static final String ZK_NAMESPACE_PROPERTY = "singularity.namespace";
  public static final String HOSTNAME_PROPERTY = "singularity.hostname";
  public static final String HTTP_PORT_PROPERTY = "singularity.http.port";
  public static final String UNDERLYING_CURATOR = "curator.base.instance";
  
  public static final String TASK_FAILED_TEMPLATE = "task.failed.template";
  public static final String REQUEST_IN_COOLDOWN_TEMPLATE = "request.in.cooldown.template";
  public static final String TASK_NOT_RUNNING_WARNING_TEMPLATE = "task.not.running.warning.template";
  
  @Override
  protected void configure() {
    bind(HistoryManager.class).to(JDBIHistoryManager.class);
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
  }
  
  private void bindMethodInterceptorForStringTemplateClassLoaderWorkaround() {
    bindInterceptor(Matchers.subclassesOf(JDBIHistoryManager.class), Matchers.any(), new MethodInterceptor() {
      
      @Override
      public Object invoke(MethodInvocation invocation) throws Throwable {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        
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
  @Named(MASTER_PROPERTY)
  public String providesMaster(SingularityConfiguration config) {
    return config.getMesosConfiguration().getMaster();
  }
  
  @Provides
  @Singleton
  public ZooKeeperConfiguration zooKeeperConfiguration(SingularityConfiguration config) {
    return config.getZooKeeperConfiguration();
  }

  @Provides
  @Singleton
  public Optional<S3Service> s3Service(Optional<S3Configuration> config) {
    if (!config.isPresent()) {
      return Optional.absent();
    }
    
    try {
      S3Service s3 = new RestS3Service(new AWSCredentials(config.get().getS3AccessKey(), config.get().getS3SecretKey()));
      return Optional.of(s3);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  @Provides
  @Named(ZK_NAMESPACE_PROPERTY)
  public String providesZkNamespace(ZooKeeperConfiguration config) {
    return config.getZkNamespace();
  }

  @Provides
  @Named(HOSTNAME_PROPERTY)
  public String providesHostnameProperty(SingularityConfiguration config) throws Exception {
    return !Strings.isNullOrEmpty(config.getHostname()) ? config.getHostname() : JavaUtils.getHostAddress();
  }

  @Provides
  @Singleton
  @Named(HTTP_PORT_PROPERTY)
  public int providesHttpPortProperty(SingularityConfiguration config) {
    SimpleServerFactory simpleServerFactory = (SimpleServerFactory) config.getServerFactory();
    HttpConnectorFactory httpFactory = (HttpConnectorFactory) simpleServerFactory.getConnector();
    
    return httpFactory.getPort();
  }

  @Provides
  @Singleton
  public LeaderLatch provideLeaderLatch(CuratorFramework curator,
                                        @Named(SingularityServiceModule.ZK_NAMESPACE_PROPERTY) String zkNamespace,
                                        @Named(SingularityServiceModule.HOSTNAME_PROPERTY) String hostname,
                                        @Named(SingularityServiceModule.HTTP_PORT_PROPERTY) int httpPort) {
    return new LeaderLatch(curator, LEADER_PATH, String.format("%s:%d", hostname, httpPort));
  }
  
  @Provides
  @Singleton
  public MesosConfiguration mesosConfiguration(SingularityConfiguration config) {
    return config.getMesosConfiguration();
  }
  
  @Provides
  @Singleton
  public Optional<SMTPConfiguration> smtpConfiguration(SingularityConfiguration config) {
    return config.getSmtpConfiguration();
  }
  
  @Provides
  @Singleton
  public Optional<S3Configuration> s3Configuration(SingularityConfiguration config) {
    return config.getS3Configuration();
  }
  
  @Singleton
  @Provides
  @Named(UNDERLYING_CURATOR)
  public CuratorFramework provideCurator(ZooKeeperConfiguration config) throws InterruptedException {
    LOG.info("Creating curator/ZK client and blocking on connection to ZK quorum {} (timeout: {})", config.getQuorum(), config.getConnectTimeoutMillis());
    
    CuratorFramework client = CuratorFrameworkFactory.builder()
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
    
    return client;
  }
  
  @Singleton
  @Provides
  public CuratorFramework provideNamespaceCurator(@Named(UNDERLYING_CURATOR) CuratorFramework curator, ZooKeeperConfiguration config) {
    return curator.usingNamespace(config.getZkNamespace());  
  }
  
  @Provides
  @Singleton
  public DBI getDBI(Environment environment, SingularityConfiguration singularityConfiguration) throws ClassNotFoundException {
    final DBIFactory factory = new DBIFactory();
    return factory.build(environment, singularityConfiguration.getDataSourceFactory(), "db");
  }
  
  @Provides
  public HistoryJDBI getHistoryJDBI(DBI dbi) {
    return dbi.onDemand(HistoryJDBI.class);
  }
  
  private JadeTemplate getJadeTemplate(String name) throws IOException {
    Parser parser = new Parser("templates/" + name, JadeHelper.JADE_LOADER);
    Node root = parser.parse();

    JadeTemplate jadeTemplate = new JadeTemplate();

    jadeTemplate.setTemplateLoader(JadeHelper.JADE_LOADER);
    jadeTemplate.setRootNode(root);
    
    return jadeTemplate;
  }
  
  @Provides
  @Singleton
  @Named(TASK_FAILED_TEMPLATE)
  public JadeTemplate getTaskFailedTemplate() throws IOException {
    return getJadeTemplate("task_failed.jade");
  }
  
  @Provides
  @Singleton
  @Named(REQUEST_IN_COOLDOWN_TEMPLATE)
  public JadeTemplate getRequestPausedTemplate() throws IOException {
    return getJadeTemplate("request_in_cooldown.jade");
  }
  
  @Provides
  @Singleton
  @Named(TASK_NOT_RUNNING_WARNING_TEMPLATE)
  public JadeTemplate getTaskNotRunningWarningTemplate() throws IOException {
    return getJadeTemplate("task_not_running_warning.jade");
  }

  @Provides
  @Singleton
  public Optional<Raven> providesRavenIfConfigured(SingularityConfiguration configuration) {
    if (configuration.getSentryConfiguration().isPresent()) {
      return Optional.of(RavenFactory.ravenInstance(configuration.getSentryConfiguration().get().getDsn()));
    } else {
      return Optional.absent();
    }
  }
}
