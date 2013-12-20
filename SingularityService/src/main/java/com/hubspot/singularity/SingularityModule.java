package com.hubspot.singularity;

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
import org.skife.jdbi.v2.DBI;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.name.Named;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SMTPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.history.HistoryJDBI;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.JDBIHistoryManager;
import com.hubspot.singularity.smtp.SingularityMailer;

public class SingularityModule extends AbstractModule {
  
  private static final String LEADER_PATH = "/leader";
  
  public static final String MASTER_PROPERTY = "singularity.master";
  public static final String ZK_NAMESPACE_PROPERTY = "singularity.namespace";
  public static final String HOSTNAME_PROPERTY = "singularity.hostname";
  public static final String HTTP_PORT_PROPERTY = "singularity.http.port";
  public static final String UNDERLYING_CURATOR = "curator.base.instance";
  
  @Override
  protected void configure() {
    bind(HistoryManager.class).to(JDBIHistoryManager.class);
    bind(SingularityDriverManager.class).in(Scopes.SINGLETON);
    bind(SingularityLeaderController.class).in(Scopes.SINGLETON);
    bind(SingularityStatePoller.class).in(Scopes.SINGLETON);
    bind(SingularityCloser.class).in(Scopes.SINGLETON);
    bind(SingularityMailer.class).in(Scopes.SINGLETON);
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
        .registerModule(new ProtobufModule());
  }
  
  public static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
  
  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return OBJECT_MAPPER;
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
                                        @Named(SingularityModule.ZK_NAMESPACE_PROPERTY) String zkNamespace,
                                        @Named(SingularityModule.HOSTNAME_PROPERTY) String hostname,
                                        @Named(SingularityModule.HTTP_PORT_PROPERTY) int httpPort) {
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
  
  @Singleton
  @Provides
  @Named(UNDERLYING_CURATOR)
  public CuratorFramework provideCurator(ZooKeeperConfiguration config) {
    CuratorFramework client = CuratorFrameworkFactory.newClient(
        config.getQuorum(),
        config.getSessionTimeoutMillis(),
        config.getConnectTimeoutMillis(),
        new ExponentialBackoffRetry(config.getRetryBaseSleepTimeMilliseconds(), config.getRetryMaxTries()));
    
    client.start();
    
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
  
}
