package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.InactiveAgentManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.SingularityAgentAndRackManager;
import com.hubspot.singularity.mesos.SingularityMesosScheduler;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos.MasterInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularitySlaveReconciliationPoller extends SingularityLeaderOnlyPoller {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularitySlaveReconciliationPoller.class
  );

  private final SlaveManager slaveManager;
  private final SingularityConfiguration configuration;
  private final SingularityAgentAndRackManager slaveAndRackManager;
  private final MesosClient mesosClient;
  private final SingularityMesosScheduler mesosScheduler;
  private final InactiveAgentManager inactiveAgentManager;

  @Inject
  SingularitySlaveReconciliationPoller(
    SingularityConfiguration configuration,
    SlaveManager slaveManager,
    SingularityAgentAndRackManager slaveAndRackManager,
    MesosClient mesosClient,
    SingularityMesosScheduler mesosScheduler,
    InactiveAgentManager inactiveAgentManager
  ) {
    super(configuration.getReconcileSlavesEveryMinutes(), TimeUnit.MINUTES);
    this.slaveManager = slaveManager;
    this.configuration = configuration;
    this.slaveAndRackManager = slaveAndRackManager;
    this.mesosClient = mesosClient;
    this.mesosScheduler = mesosScheduler;
    this.inactiveAgentManager = inactiveAgentManager;
  }

  @Override
  public void runActionOnPoll() {
    refereshSlavesAndRacks();
    checkDeadSlaves();
    inactiveAgentManager.cleanInactiveAgentsList(
      System.currentTimeMillis() -
      TimeUnit.HOURS.toMillis(configuration.getCleanInactiveHostListEveryHours())
    );
    clearOldSlaveHistory();
  }

  private void refereshSlavesAndRacks() {
    try {
      Optional<MasterInfo> maybeMasterInfo = mesosScheduler.getMaster();
      if (maybeMasterInfo.isPresent()) {
        final String uri = mesosClient.getMasterUri(
          MesosUtils.getMasterHostAndPort(maybeMasterInfo.get())
        );
        MesosMasterStateObject state = mesosClient.getMasterState(uri);

        slaveAndRackManager.loadSlavesAndRacksFromMaster(state, false);
      }
    } catch (Exception e) {
      LOG.error("Could not refresh slave data", e);
    }
  }

  private void checkDeadSlaves() {
    final long start = System.currentTimeMillis();

    final List<SingularityAgent> deadSlaves = slaveManager.getObjectsFiltered(
      MachineState.DEAD
    );

    if (deadSlaves.isEmpty()) {
      LOG.trace("No dead slaves");
      return;
    }

    int deleted = 0;
    final long maxDuration = TimeUnit.HOURS.toMillis(
      configuration.getDeleteDeadSlavesAfterHours()
    );

    for (SingularityAgent deadSlave : slaveManager.getObjectsFiltered(
      MachineState.DEAD
    )) {
      final long duration =
        System.currentTimeMillis() - deadSlave.getCurrentState().getTimestamp();

      if (duration > maxDuration) {
        SingularityDeleteResult result = slaveManager.deleteObject(deadSlave.getId());

        deleted++;

        LOG.info(
          "Removing dead slave {} ({}) after {} (max {})",
          deadSlave.getId(),
          result,
          JavaUtils.durationFromMillis(duration),
          JavaUtils.durationFromMillis(maxDuration)
        );
      }
    }

    LOG.debug(
      "Checked {} dead slaves, deleted {} in {}",
      deadSlaves.size(),
      deleted,
      JavaUtils.duration(start)
    );
  }

  private void clearOldSlaveHistory() {
    for (SingularityAgent singularityAgent : slaveManager.getObjects()) {
      slaveManager.clearOldHistory(singularityAgent.getId());
    }
  }
}
