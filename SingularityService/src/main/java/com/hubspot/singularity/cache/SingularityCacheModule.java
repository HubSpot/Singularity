package com.hubspot.singularity.cache;

import java.util.concurrent.TimeUnit;

import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.CacheConfiguration;

import io.atomix.core.Atomix;

public class SingularityCacheModule extends AbstractModule {
  private final CacheConfiguration cacheConfiguration;

  public SingularityCacheModule(CacheConfiguration cacheConfiguration) {
    this.cacheConfiguration = cacheConfiguration;
  }

  @Override
  public void configure() {
    bind(ZkNodeDiscoveryProvider.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public Atomix providesAtomixCache(ZkNodeDiscoveryProvider zkNodeDiscoveryProvider,
                                    @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort) {
    String host = hostAndPort.getHost();
    Atomix atomix = Atomix.builder()
        .withMemberId(host)
        .withAddress(host, cacheConfiguration.getAtomixPort())
        .withMembershipProvider(zkNodeDiscoveryProvider)
        .build();
    atomix.start().get(cacheConfiguration.getAtomixStartTimeoutSeconds(), TimeUnit.SECONDS);
    return atomix;
  }
}
