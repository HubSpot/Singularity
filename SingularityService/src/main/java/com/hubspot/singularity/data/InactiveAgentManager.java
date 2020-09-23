package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;

@Singleton
public class InactiveAgentManager extends CuratorManager {
  private static final String ROOT_PATH = "/inactiveSlaves";

  @Inject
  public InactiveAgentManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry
  ) {
    super(curator, configuration, metricRegistry);
  }

  public Set<String> getInactiveAgents() {
    return new HashSet<>(getChildren(ROOT_PATH));
  }

  public void deactivateAgent(String host) {
    create(pathOf(host));
  }

  public void activateAgent(String host) {
    delete(pathOf(host));
  }

  public boolean isInactive(String host) {
    return exists(pathOf(host));
  }

  private String pathOf(String host) {
    return String.format("%s/%s", ROOT_PATH, host);
  }

  public void cleanInactiveAgentsList(long thresholdTime) {
    for (String host : getInactiveAgents()) {
      Optional<Stat> stat = checkExists(pathOf(host));
      if (stat.isPresent() && stat.get().getMtime() < thresholdTime) {
        delete(pathOf(host));
      }
    }
  }
}
