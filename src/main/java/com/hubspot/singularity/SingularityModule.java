package com.hubspot.singularity;

import com.codahale.dropwizard.setup.Environment;
import com.google.common.base.Strings;
import com.google.inject.*;
import com.hubspot.mesos.JavaUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.skife.jdbi.v2.DBI;

import com.codahale.dropwizard.jackson.Jackson;
import com.codahale.dropwizard.jdbi.DBIFactory;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.data.history.HistoryJDBI;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.JDBIHistoryManager;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;

public class SingularityModule extends AbstractModule {
  private static final String LEADER_PATH = "/leader";
  
  public static final String MASTER_PROPERTY = "singularity.master";
  public static final String ZK_NAMESPACE_PROPERTY = "singularity.namespace";
  public static final String HOSTNAME_PROPERTY = "singularity.hostname";
  public static final String HTTP_PORT_PROPERTY = "singularity.http.port";
  
  @Override
  protected void configure() {
    bind(SingularityMesosScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityDriver.class).in(Scopes.SINGLETON);
    bind(HistoryManager.class).to(JDBIHistoryManager.class);
  }

  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return Jackson.newObjectMapper()
        .setSerializationInclusion(Include.NON_NULL)
        .registerModule(new ProtobufModule());
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
  public int providesHttpPortProperty(SingularityConfiguration config, Environment environment) {
    for (Connector connector : config.getServerFactory().build(environment).getConnectors()) {
      if (connector instanceof ServerConnector) {
        return ((ServerConnector) connector).getPort();
      }
    }

    throw new ProvisionException("Failed to get HTTP port from dropwizard config");
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
  
  @Singleton
  @Provides
  public CuratorFramework provideCurator(ZooKeeperConfiguration config) {
    CuratorFramework client = CuratorFrameworkFactory.newClient(
        config.getQuorum(),
        config.getSessionTimeoutMillis(),
        config.getConnectTimeoutMillis(),
        new ExponentialBackoffRetry(config.getRetryBaseSleepTimeMilliseconds(), config.getRetryMaxTries()));
    
    client.start();
    
    return client.usingNamespace(config.getZkNamespace());
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
