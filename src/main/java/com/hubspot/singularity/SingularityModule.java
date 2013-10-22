package com.hubspot.singularity;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.codahale.dropwizard.jackson.Jackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.ZooKeeperConfiguration;
import com.hubspot.singularity.mesos.SingularityDriver;
import com.hubspot.singularity.mesos.SingularityScheduler;

public class SingularityModule extends AbstractModule {
  
  public static final String MASTER_PROPERTY = "singularity.master";
  
  @Override
  protected void configure() {
    bind(SingularityScheduler.class).in(Scopes.SINGLETON);
    bind(SingularityDriver.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public ObjectMapper getObjectMapper() {
    return Jackson.newObjectMapper();
  }
  
  @Provides
  @Named(MASTER_PROPERTY)
  public String providesMaster(SingularityConfiguration config) {
    return config.getMesosConfiguration().getMaster();
  }
  
  @Singleton
  @Provides
  public CuratorFramework provideCurator(SingularityConfiguration config) {
    ZooKeeperConfiguration zkConfig = config.getZooKeeperConfiguration();
    
    CuratorFramework client = CuratorFrameworkFactory.newClient(
        zkConfig.getQuorum(),
        zkConfig.getSessionTimeoutMillis(),
        zkConfig.getConnectTimeoutMillis(),
        new ExponentialBackoffRetry(zkConfig.getRetryBaseSleepTimeMilliseconds(), zkConfig.getRetryMaxTries()));
    
    client.start();
    
    return client.usingNamespace(zkConfig.getZkNamespace());
  }
  
}
