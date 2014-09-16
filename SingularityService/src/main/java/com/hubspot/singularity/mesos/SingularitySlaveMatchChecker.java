package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

public class SingularitySlaveMatchChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularitySlaveMatchChecker.class);

  private final String rackIdAttributeKey;
  private final String defaultRackId;

  private final SingularityConfiguration configuration;

  private final RackManager rackManager;
  private final SlaveManager slaveManager;

  @Inject
  public SingularitySlaveMatchChecker(SingularityConfiguration configuration, MesosConfiguration mesosConfiguration, RackManager rackManager, SlaveManager slaveManager, TaskManager taskManager) {
    this.configuration = configuration;

    this.rackIdAttributeKey = mesosConfiguration.getRackIdAttributeKey();
    this.defaultRackId = mesosConfiguration.getDefaultRackId();

    this.rackManager = rackManager;
    this.slaveManager = slaveManager;
  }

  public enum SlaveMatchState {
    OK(true),
    NOT_RACK_OR_SLAVE_PARTICULAR(true),
    RACK_SATURATED(false),
    SLAVE_SATURATED(false),
    SLAVE_DECOMISSIONING(false),
    RACK_DECOMISSIONING(false),
    RACK_AFFINITY_NOT_MATCHING(false);

    private final boolean isMatchAllowed;

    private SlaveMatchState(boolean isMatchAllowed) {
      this.isMatchAllowed = isMatchAllowed;
    }

    public boolean isMatchAllowed() {
      return isMatchAllowed;
    }

  }

  private String getHost(String hostname) {
    return getSafeString(hostname.split("\\.")[0]);
  }

  public String getSlaveHost(Offer offer) {
    return getHost(offer.getHostname());
  }

  public SlaveMatchState checkSlave(Protos.Offer offer, SingularityTaskRequest taskRequest, SingularitySchedulerStateCache stateCache) {
    final String host = getSlaveHost(offer);
    final String rackId = getRackId(offer);
    final String slaveId = offer.getSlaveId().getValue();

    if (stateCache.isSlaveDecomissioning(slaveId)) {
      return SlaveMatchState.SLAVE_DECOMISSIONING;
    }

    if (stateCache.isRackDecomissioning(rackId)) {
      return SlaveMatchState.RACK_DECOMISSIONING;
    }

    if (!taskRequest.getRequest().getRackAffinity().or(Collections.<String> emptyList()).isEmpty()) {
      if (!taskRequest.getRequest().getRackAffinity().get().contains(rackId)) {
        LOG.trace("Task {} requires a rack in {} (current rack {})", taskRequest.getPendingTask().getPendingTaskId(), taskRequest.getRequest().getRackAffinity().get(), rackId);
        return SlaveMatchState.RACK_AFFINITY_NOT_MATCHING;
      }
    }

    final SlavePlacement slavePlacement = taskRequest.getRequest().getSlavePlacement().or(configuration.getDefaultSlavePlacement());

    if (!taskRequest.getRequest().isRackSensitive() && slavePlacement == SlavePlacement.GREEDY) {
      return SlaveMatchState.NOT_RACK_OR_SLAVE_PARTICULAR;
    }

    final int numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    double numOnRack = 0;
    double numOnSlave = 0;

    for (SingularityTaskId taskId : SingularityTaskId.matchingAndNotIn(stateCache.getActiveTaskIds(), taskRequest.getRequest().getId(), taskRequest.getDeploy().getId(), stateCache.getCleaningTasks())) {
      if (taskId.getHost().equals(host)) {
        numOnSlave++;
      }
      if (taskId.getRackId().equals(rackId)) {
        numOnRack++;
      }
    }

    if (taskRequest.getRequest().isRackSensitive()) {
      final double numPerRack = numDesiredInstances / (double) stateCache.getNumActiveRacks();

      final boolean isRackOk = numOnRack < numPerRack;

      if (!isRackOk) {
        return SlaveMatchState.RACK_SATURATED;
      }
    }

    switch (slavePlacement) {
      case SEPARATE:
        if (numOnSlave > 0) {
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case OPTIMISTIC:
        final double numPerSlave = numDesiredInstances / (double) stateCache.getNumActiveSlaves();

        final boolean isSlaveOk = numOnSlave < numPerSlave;

        if (!isSlaveOk) {
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case GREEDY:
    }

    return SlaveMatchState.OK;
  }

  private void clearRacksAndSlaves() {
    final long start = System.currentTimeMillis();

    int racksCleared = rackManager.clearActive();
    int slavesCleared = slaveManager.clearActive();

    LOG.info("Cleared {} racks and {} slaves in {}", racksCleared, slavesCleared, JavaUtils.duration(start));
  }

  public void slaveLost(SlaveID slaveIdObj) {
    final String slaveId = slaveIdObj.getValue();

    if (isSlaveDead(slaveId) || isSlaveDecomissioning(slaveId) || !isSlaveActive(slaveId)) {
      return;
    }

    Optional<SingularitySlave> slave = slaveManager.getActiveObject(slaveId);

    if (slave.isPresent()) {
      slaveManager.markAsDead(slaveId);

      checkRackAfterSlaveLoss(slave.get());
    } else {
      LOG.warn("Lost a slave {}, but didn't know about it", slaveId);
    }
  }

  private void checkRackAfterSlaveLoss(SingularitySlave lostSlave) {
    List<SingularitySlave> slaves = slaveManager.getActiveObjects();

    int numInRack = 0;

    for (SingularitySlave slave : slaves) {
      if (slave.getRackId().equals(lostSlave.getRackId())) {
        numInRack++;
      }
    }

    LOG.info("Found {} slaves left in rack {}", numInRack, lostSlave.getRackId());

    if (numInRack == 0) {
      rackManager.markAsDead(lostSlave.getRackId());
    }
  }

  public void loadRacksFromMaster(MesosMasterStateObject state) {
    clearRacksAndSlaves();

    int slaves = 0;
    int racks = 0;

    for (MesosMasterSlaveObject slave : state.getSlaves()) {
      Optional<String> maybeRackId = Optional.fromNullable(slave.getAttributes().get(rackIdAttributeKey));
      String slaveId = slave.getId();
      String rackId = getSafeString(maybeRackId.or(defaultRackId));
      String host = getHost(slave.getHostname());

      if (checkSlave(slaveId, host, rackId).saveResult == SaveResult.NEW) {
        slaves++;
      }

      if (checkRack(rackId).saveResult == SaveResult.NEW) {
        racks++;
      }
    }

    LOG.info("Found {} racks and {} slaves", racks, slaves);
  }

  public String getRackId(Offer offer) {
    for (Attribute attribute : offer.getAttributesList()) {
      if (attribute.getName().equals(rackIdAttributeKey)) {
        return getSafeString(attribute.getText().getValue());
      }
    }

    return defaultRackId;
  }

  private String getSafeString(String string) {
    return string.replace("-", "_");
  }

  private SaveResultHolder checkRack(String rackId) {
    if (isRackActive(rackId)) {
      return ALREADY_ACTIVE_HOLDER;
    }

    if (isRackDecomissioning(rackId)) {
      return DECOMISSIONING_HOLDER;
    }

    if (isRackDead(rackId)) {
      rackManager.removeDead(rackId);
    }

    SingularityRack newRack = new SingularityRack(rackId);

    rackManager.save(newRack);

    return new SaveResultHolder(Optional.of((SingularityMachineAbstraction) newRack), SaveResult.NEW);
  }

  private static class SaveResultHolder {
    private final Optional<SingularityMachineAbstraction> newObject;
    private final SaveResult saveResult;

    public SaveResultHolder(SaveResult saveResult) {
      this(Optional.<SingularityMachineAbstraction> absent(), saveResult);
    }

    public SaveResultHolder(Optional<SingularityMachineAbstraction> newObject, SaveResult saveResult) {
      this.newObject = newObject;
      this.saveResult = saveResult;
    }

  }

  private static final SaveResultHolder DECOMISSIONING_HOLDER = new SaveResultHolder(SaveResult.DECOMISSIONING);
  private static final SaveResultHolder ALREADY_ACTIVE_HOLDER = new SaveResultHolder(SaveResult.ALREADY_ACTIVE);

  private enum SaveResult {
    NEW, DECOMISSIONING, ALREADY_ACTIVE;
  }

  private SaveResultHolder checkSlave(String slaveId, String host, String rackId) {
    if (isSlaveActive(slaveId)) {
      return ALREADY_ACTIVE_HOLDER;
    }

    if (isSlaveDecomissioning(slaveId)) {
      return DECOMISSIONING_HOLDER;
    }

    if (isSlaveDead(slaveId)) {
      slaveManager.removeDead(slaveId);
    }

    SingularitySlave newSlave = new SingularitySlave(slaveId, host, rackId);

    slaveManager.save(newSlave);

    return new SaveResultHolder(Optional.of((SingularityMachineAbstraction) newSlave), SaveResult.NEW);
  }

  private boolean isRackActive(String rackId) {
    return rackManager.isActive(rackId);
  }

  private boolean isRackDead(String rackId) {
    return rackManager.isDead(rackId);
  }

  private boolean isRackDecomissioning(String rackId) {
    return rackManager.isDecomissioning(rackId);
  }

  private boolean isSlaveActive(String slaveId) {
    return slaveManager.isActive(slaveId);
  }

  private boolean isSlaveDecomissioning(String slaveId) {
    return slaveManager.isDecomissioning(slaveId);
  }

  private boolean isSlaveDead(String slaveId) {
    return slaveManager.isDead(slaveId);
  }

  public void checkOffer(Offer offer) {
    final String slaveId = offer.getSlaveId().getValue();
    final String rackId = getRackId(offer);
    final String host = getSlaveHost(offer);

    SaveResultHolder slaveHolder = checkSlave(slaveId, host, rackId);

    if (slaveHolder.saveResult == SaveResult.NEW) {
      LOG.info("Offer revealed a new slave {}", slaveHolder.newObject.get());
    }

    SaveResultHolder rackHolder = checkRack(rackId);

    if (rackHolder.saveResult == SaveResult.NEW) {
      LOG.info("Offer revealed a new rack {}", rackHolder.newObject.get());
    }
  }

}
