package com.hubspot.singularity.data;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import java.util.List;
import java.util.Optional;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class AgentManager extends AbstractMachineManager<SingularityAgent> {

  private static final Logger LOG = LoggerFactory.getLogger(AgentManager.class);

  private static final String AGENT_ROOT = "/slaves";
  private final SingularityLeaderCache leaderCache;
  private final UsageManager usageManager;

  @Inject
  public AgentManager(
    CuratorFramework curator,
    SingularityConfiguration configuration,
    MetricRegistry metricRegistry,
    Transcoder<SingularityAgent> agentTranscoder,
    Transcoder<SingularityMachineStateHistoryUpdate> stateHistoryTranscoder,
    Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder,
    SingularityLeaderCache leaderCache,
    UsageManager usageManager
  ) {
    super(
      curator,
      configuration,
      metricRegistry,
      agentTranscoder,
      stateHistoryTranscoder,
      expiringMachineStateTranscoder
    );
    this.leaderCache = leaderCache;
    this.usageManager = usageManager;
  }

  @Override
  protected String getRoot() {
    return AGENT_ROOT;
  }

  public void activateLeaderCache() {
    leaderCache.cacheAgents(getObjectsNoCache(getRoot()));
  }

  @Override
  public Optional<SingularityAgent> getObjectFromLeaderCache(String agentId) {
    if (leaderCache.active()) {
      return leaderCache.getAgent(agentId);
    }

    return Optional.empty(); // fallback to zk
  }

  @Override
  public List<SingularityAgent> getObjectsFromLeaderCache() {
    if (leaderCache.active()) {
      return leaderCache.getAgents();
    }
    return null; // fallback to zk
  }

  @Override
  public void saveObjectToLeaderCache(SingularityAgent singularityAgent) {
    if (leaderCache.active()) {
      leaderCache.putAgent(singularityAgent);
    } else {
      LOG.info("Asked to save agents to leader cache when not active");
    }
  }

  @Override
  public void deleteFromLeaderCache(String agentId) {
    if (leaderCache.active()) {
      leaderCache.removeAgent(agentId);
    } else {
      LOG.info("Asked to remove agent from leader cache when not active");
    }
  }

  @Override
  public StateChangeResult changeState(
    SingularityAgent singularityAgent,
    MachineState newState,
    Optional<String> message,
    Optional<String> user
  ) {
    if (newState == MachineState.DEAD) {
      usageManager.deleteAgentUsage(singularityAgent.getId());
    }
    return super.changeState(singularityAgent, newState, message, user);
  }
}
