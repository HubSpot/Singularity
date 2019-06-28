package com.hubspot.singularity.cache;

import java.time.Duration;

import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.CacheConfiguration;

import io.atomix.core.Atomix;
import io.atomix.primitive.partition.MemberGroupStrategy;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;

public class SingularityCacheModule extends AbstractModule {
  private final CacheConfiguration cacheConfiguration;

  public SingularityCacheModule(CacheConfiguration cacheConfiguration) {
    this.cacheConfiguration = cacheConfiguration;
  }

  @Override
  public void configure() {
    bind(ZkNodeDiscoveryProvider.class).in(Scopes.SINGLETON);
    bind(SingularityCache.class).in(Scopes.SINGLETON);
    bind(CacheUtils.class).in(Scopes.SINGLETON);
  }

  @Provides
  @Singleton
  public Atomix providesAtomix(ZkNodeDiscoveryProvider zkNodeDiscoveryProvider,
                               @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort) throws Exception {
    String host = hostAndPort.getHost();
    Atomix atomix = Atomix.builder()
        .withMemberId(host)
        .withAddress(host, cacheConfiguration.getAtomixPort())
        .withMembershipProvider(zkNodeDiscoveryProvider)
        .withCompatibleSerialization()
        .withShutdownHook(false) // Closed in SingularityLifecycleManaged
        .withReachabilityTimeout(Duration.ofSeconds(cacheConfiguration.getAtomixReachabilityTimeoutSeconds()))
        .withClusterId("singularity")
        .withManagementGroup(PrimaryBackupPartitionGroup.builder("in-memory-data")
            .withNumPartitions(1)
            .withMemberGroupStrategy(MemberGroupStrategy.HOST_AWARE)
            .build()
        )
        .withPartitionGroups(
            PrimaryBackupPartitionGroup.builder("in-memory-data")
                .withNumPartitions(1)
                .withMemberGroupStrategy(MemberGroupStrategy.HOST_AWARE)
                .build())
        .build();
    return atomix;
  }
}
