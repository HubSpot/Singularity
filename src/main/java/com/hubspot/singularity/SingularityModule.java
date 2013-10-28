package com.hubspot.singularity;

import com.codahale.dropwizard.setup.Environment;
import com.google.inject.*;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.codahale.dropwizard.jackson.Jackson;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.ServerConnector;

public class SingularityModule extends AbstractModule {
  
  public static final String MASTER_PROPERTY = "singularity.master";
  public static final String ZK_NAMESPACE_PROPERTY = "singularity.namespace";
  public static final String HOSTNAME_PROPERTY = "singularity.hostname";
  public static final String HTTP_PORT_PROPERTY = "singularity.http.port";
  
  @Override
  protected void configure() {
    bind(SingularityMesosScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityDriver.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return Jackson.newObjectMapper().setSerializationInclusion(Include.NON_NULL);
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
  public String providesHostnameProperty(SingularityConfiguration config) {
    return config.getHostname();
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
    return new LeaderLatch(curator, String.format("%s/leader", zkNamespace), String.format("%s:%d", hostname, httpPort));
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
  
}
