package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Map;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.SingularityMachineAbstraction.SingularityMachineState;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularitySchedulerBase;

public class SingularityRackManager extends SingularitySchedulerBase {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityRackManager.class);

  private final String rackIdAttributeKey;
  private final String defaultRackId;

  private final RackManager rackManager;
  private final SlaveManager slaveManager;
  
  @Inject
  public SingularityRackManager(MesosConfiguration mesosConfiguration, RackManager rackManager, SlaveManager slaveManager, TaskManager taskManager) {
    this.rackIdAttributeKey = mesosConfiguration.getRackIdAttributeKey();
    this.defaultRackId = mesosConfiguration.getDefaultRackId();
    
    this.rackManager = rackManager;
    this.slaveManager = slaveManager;
  }
  
  public void loadCache() {
    
  }
  
  public enum RackCheckState {
    ALREADY_ON_SLAVE (false), RACK_SATURATED (false), RACK_OK (true), NOT_RACK_SENSITIVE (true), SLAVE_DECOMISSIONING (false), RACK_DECOMISSIONING (false);

    private final boolean isRackAppropriate;

    private RackCheckState(boolean isRackAppropriate) {
      this.isRackAppropriate = isRackAppropriate;
    }

    public boolean isRackAppropriate() {
      return isRackAppropriate;
    }

  }

  private String getHost(String hostname) {
    return getSafeString(hostname.split("\\.")[0]);
  }
  
  public String getSlaveHost(Offer offer) {
    return getHost(offer.getHostname());
  }
  
  public RackCheckState checkRack(Protos.Offer offer, SingularityTaskRequest taskRequest, List<SingularityTaskId> activeTasks, List<SingularityTaskId> cleaningTasks) {
    final String host = getSlaveHost(offer);
    final String rackId = getRackId(offer);
    final String slaveId = offer.getSlaveId().getValue();
    
    if (isSlaveDecomissioning(slaveId)) {
      return RackCheckState.SLAVE_DECOMISSIONING;
    }
    
    if (isRackDecomissioning(rackId)) {
      return RackCheckState.RACK_DECOMISSIONING;
    }
    
    if (!taskRequest.getRequest().isRackSensitive()) {
      return RackCheckState.NOT_RACK_SENSITIVE;
    }
    
    int numDesiredInstances = taskRequest.getRequest().getInstances();

    Map<String, Integer> rackUsage = Maps.newHashMap();

    for (SingularityTaskId taskId : getMatchingActiveTaskIds(taskRequest.getRequest().getId(), activeTasks, cleaningTasks)) {
      if (taskId.getHost().equals(host)) {
        LOG.trace(String.format("Task %s is already on slave %s - %s", taskRequest.getPendingTaskId(), host, taskId));
        
        return RackCheckState.ALREADY_ON_SLAVE;
      }
      
      Integer currentUsage = Objects.firstNonNull(rackUsage.get(taskId.getRackId()), 0);

      rackUsage.put(taskId.getRackId(), currentUsage + 1);
    }

    double numPerRack = (double) numDesiredInstances / (double) getNumRacks();
    double numOnRack = Objects.firstNonNull(rackUsage.get(rackId), 0);

    boolean isRackOk = numOnRack < numPerRack;
  
    LOG.trace(String.format("Rack result %s for taskRequest %s, rackId: %s, numPerRack %s, numOnRack %s", isRackOk, taskRequest.getPendingTaskId(), rackId, numPerRack, numOnRack));
    
    if (isRackOk) {
      return RackCheckState.RACK_OK;
    } else {
      return RackCheckState.RACK_SATURATED;
    }
  }

  private void clearRacksAndSlaves() {
    final long start = System.currentTimeMillis();
    
    int racksCleared = rackManager.clearActive();
    int slavesCleared = slaveManager.clearActive();
  
    LOG.info(String.format("Cleared %s racks and %s slaves in %sms", racksCleared, slavesCleared, System.currentTimeMillis() - start));
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
      LOG.warn(String.format("Lost a slave %s, but didn't know about it", slaveId));
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
    
    LOG.info(String.format("Found %s slaves left in rack %s", numInRack, lostSlave.getRackId()));
    
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
      
      if (checkSlave(slaveId, host, rackId) == SaveResult.NEW) {
        slaves++;
      }
      
      if (checkRack(rackId) == SaveResult.NEW) {
        racks++;
      }
    }

    LOG.info(String.format("Found %s racks and %s slaves", racks, slaves));
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
  
  private SaveResult checkRack(String rackId) {
    if (isRackActive(rackId)) {
      return SaveResult.ALREADY_ACTIVE;
    }
    
    if (isRackDecomissioning(rackId)) {
      return SaveResult.DECOMISSIONING;
    }
    
    if (isRackDead(rackId)) {
      slaveManager.removeDead(rackId);
    } 
    
    rackManager.save(new SingularityRack(rackId, SingularityMachineState.ACTIVE));
    
    return SaveResult.NEW;
  }
    
  private enum SaveResult {
    NEW, DECOMISSIONING, ALREADY_ACTIVE;
  }
  
  private SaveResult checkSlave(String slaveId, String host, String rackId) {
    if (isSlaveActive(slaveId)) {
      return SaveResult.ALREADY_ACTIVE;
    }
    
    if (isSlaveDecomissioning(slaveId)) {
      return SaveResult.DECOMISSIONING;
    }
    
    if (isSlaveDead(slaveId)) {
      slaveManager.removeDead(slaveId);
    } 
      
    slaveManager.save(new SingularitySlave(slaveId, host, rackId, SingularityMachineState.ACTIVE));
    
    return SaveResult.NEW;
  }
  
  private int getNumRacks() {
    return rackManager.getNumActive();
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
    
    SaveResult slaveSave = checkSlave(slaveId, host, rackId);
    
    if (slaveSave == SaveResult.NEW) {
      LOG.info(String.format("Offer revealed a new slave %s", new SingularitySlave(slaveId, host, rackId, SingularityMachineState.ACTIVE)));
    }
    
    SaveResult rackSave = checkRack(rackId);
    
    if (rackSave == SaveResult.NEW) {
      LOG.info(String.format("Offer revealed a new rack %s", new SingularityRack(rackId, SingularityMachineState.ACTIVE)));
    }
  }

}
