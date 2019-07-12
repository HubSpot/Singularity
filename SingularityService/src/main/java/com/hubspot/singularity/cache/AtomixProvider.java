package com.hubspot.singularity.cache;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.config.CacheConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryBuilder;
import io.atomix.core.Atomix;
import io.atomix.primitive.partition.MemberGroupStrategy;
import io.atomix.protocols.backup.partition.PrimaryBackupPartitionGroup;

public class AtomixProvider implements Provider<Atomix> {
  private static final Logger LOG = LoggerFactory.getLogger(AtomixProvider.class);

  private final LeaderLatch leaderLatch;
  private final CacheConfiguration cacheConfiguration;
  private final HostAndPort hostAndPort;

  @Inject
  public AtomixProvider(LeaderLatch leaderLatch,
                                 SingularityConfiguration configuration,
                                 @Named(SingularityMainModule.HTTP_HOST_AND_PORT) HostAndPort hostAndPort) {
    this.leaderLatch = leaderLatch;
    this.cacheConfiguration = configuration.getCacheConfiguration();
    this.hostAndPort = hostAndPort;
  }

  @Override
  public Atomix get() {
    try {
      String host = hostAndPort.getHost();
      Set<Node> nodes = leaderLatch.getParticipants().stream()
          .map((p) -> {
            String participantHost = HostAndPort.fromString(p.getId()).getHost();
            return Node.builder()
                .withId(participantHost)
                .withAddress(participantHost, cacheConfiguration.getAtomixPort())
                .build();
          }).collect(Collectors.toSet());
      Atomix atomix = Atomix.builder()
          .withMemberId(host)
          .withAddress(host, cacheConfiguration.getAtomixPort())
          .withMembershipProvider(new BootstrapDiscoveryBuilder().withNodes(nodes).build())
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
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
