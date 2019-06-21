package com.hubspot.singularity.cache;

import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.config.CacheConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.atomix.cluster.Node;
import io.atomix.cluster.discovery.BootstrapDiscoveryProvider;

public class ZkNodeDiscoveryProvider extends BootstrapDiscoveryProvider {
  private static final Logger LOG = LoggerFactory.getLogger(ZkNodeDiscoveryProvider.class);

  private final LeaderLatch leaderLatch;
  private final CacheConfiguration cacheConfiguration;
  private final AtomicReference<Set<Node>> lastFetched;

  @Inject
  public ZkNodeDiscoveryProvider(LeaderLatch leaderLatch,
                                 SingularityConfiguration configuration) {
    this.leaderLatch = leaderLatch;
    this.cacheConfiguration = configuration.getCacheConfiguration();
    this.lastFetched = new AtomicReference<>();
  }

  @Override
  public Set<Node> getNodes() {
    try {
      Set<Node> nodes = leaderLatch.getParticipants().stream()
          .map((p) -> {
            URI participantUri = URI.create(p.getId());
            String host = participantUri.getHost();
            return Node.builder()
                .withId(host)
                .withAddress(host, cacheConfiguration.getAtomixPort())
                .build();
          }).collect(Collectors.toSet());
      lastFetched.set(nodes);
      return nodes;
    } catch (Throwable t) {
      LOG.warn("Could not fetch cluster members {}, returning last known set of nodes", t.getMessage());
      Set<Node> maybeLast = lastFetched.get();
      if (maybeLast != null) {
        return maybeLast;
      } else {
        throw new RuntimeException("Could not fetch cluster members", t);
      }
    }
  }
}
