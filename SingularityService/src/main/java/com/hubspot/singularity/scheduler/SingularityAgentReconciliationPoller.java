package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.AgentManager;
import com.hubspot.singularity.data.InactiveAgentManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.SingularityAgentAndRackManager;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityAgentReconciliationPoller extends SingularityLeaderOnlyPoller {

  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityAgentReconciliationPoller.class
  );

  private final AgentManager agentManager;
  private final SingularityConfiguration configuration;
  private final SingularityAgentAndRackManager agentAndRackManager;
  private final MesosClient mesosClient;
  private final SingularityMesosScheduler mesosScheduler;
  private final InactiveAgentManager inactiveAgentManager;

  @Inject
  SingularityAgentReconciliationPoller(
    SingularityConfiguration configuration,
    AgentManager agentManager,
    SingularityAgentAndRackManager agentAndRackManager,
    MesosClient mesosClient,
    SingularityMesosScheduler mesosScheduler,
    InactiveAgentManager inactiveAgentManager
  ) {
    super(configuration.getReconcileAgentsEveryMinutes(), TimeUnit.MINUTES);
    this.agentManager = agentManager;
    this.configuration = configuration;
    this.agentAndRackManager = agentAndRackManager;
    this.mesosClient = mesosClient;
    this.mesosScheduler = mesosScheduler;
    this.inactiveAgentManager = inactiveAgentManager;
  }

  @Override
  public void runActionOnPoll() {
    refereshAgentsAndRacks();
    checkInactiveAgents();
    inactiveAgentManager.cleanInactiveAgentsList(
      System.currentTimeMillis() -
      TimeUnit.HOURS.toMillis(configuration.getCleanInactiveHostListEveryHours())
    );
    clearOldAgentHistory();
  }

  private void refereshAgentsAndRacks() {
    try {
      Optional<MasterInfo> maybeMasterInfo = mesosScheduler.getMaster();
      if (maybeMasterInfo.isPresent()) {
        final String uri = mesosClient.getMasterUri(
          MesosUtils.getMasterHostAndPort(maybeMasterInfo.get())
        );
        MesosMasterStateObject state = mesosClient.getMasterState(uri);

        agentAndRackManager.loadAgentsAndRacksFromMaster(state, false);
      }
    } catch (Exception e) {
      LOG.error("Could not refresh agent data", e);
    }
  }

  private void checkInactiveAgents() {
    final long start = System.currentTimeMillis();

    // filter dead and missing on startup agents for cleanup
    List<SingularityAgent> deadAgents = agentManager.getObjectsFiltered(
      MachineState.DEAD
    );

    LOG.debug("Found {} dead agents", deadAgents.size());

    List<SingularityAgent> missingOnStartupAgents = agentManager.getObjectsFiltered(
      MachineState.MISSING_ON_STARTUP
    );

    LOG.debug("Found {} agents missing on startup", missingOnStartupAgents.size());

    List<SingularityAgent> inactiveAgents = new ArrayList<>();
    inactiveAgents.addAll(deadAgents);
    inactiveAgents.addAll(missingOnStartupAgents);

    if (inactiveAgents.isEmpty()) {
      LOG.trace("No inactive agents");
      return;
    }

    int deleted = 0;
    final long maxDuration = TimeUnit.HOURS.toMillis(
      configuration.getDeleteDeadAgentsAfterHours()
    );

    for (SingularityAgent inactiveAgent : inactiveAgents) {
      final long duration =
        System.currentTimeMillis() - inactiveAgent.getCurrentState().getTimestamp();

      if (duration > maxDuration) {
        SingularityDeleteResult result = agentManager.deleteObject(inactiveAgent.getId());
        inactiveAgentManager.cleanInactiveAgent(inactiveAgent.getHost()); // delete agent from inactive list too

        deleted++;

        LOG.info(
          "Removing inactive agent {} ({}) after {} (max {})",
          inactiveAgent.getId(),
          result,
          JavaUtils.durationFromMillis(duration),
          JavaUtils.durationFromMillis(maxDuration)
        );
      }
    }

    LOG.debug(
      "Checked {} inactive agents, deleted {} in {}",
      inactiveAgents.size(),
      deleted,
      JavaUtils.duration(start)
    );
  }

  private void clearOldAgentHistory() {
    for (SingularityAgent singularityAgent : agentManager.getObjects()) {
      agentManager.clearOldHistory(singularityAgent.getId());
    }
  }
}
