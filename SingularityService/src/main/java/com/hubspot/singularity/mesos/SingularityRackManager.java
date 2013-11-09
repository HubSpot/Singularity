package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosMasterSlaveObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.MesosConfiguration;

public class SingularityRackManager {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityRackManager.class);

  private final String rackIdAttributeKey;
  private final String defaultRackId;

  private final Map<String, String> slaveToRacks;
  private final Set<String> racks;

  @Inject
  public SingularityRackManager(MesosConfiguration mesosConfiguration) {
    this.rackIdAttributeKey = mesosConfiguration.getRackIdAttributeKey();
    this.defaultRackId = mesosConfiguration.getDefaultRackId();

    slaveToRacks = Maps.newHashMap();
    racks = Sets.newHashSet();
  }

  // TODO log or better reasoning behind why certain rack choices are made
  // (maybe an ENUM?)
  public boolean checkRack(Protos.Offer offer, SingularityTaskRequest taskRequest, List<SingularityTaskId> activeTasks) {
    String rackId = getRackId(offer);

    int numDesiredInstances = taskRequest.getRequest().getInstances();

    Map<String, Integer> rackUsage = Maps.newHashMap();

    for (SingularityTaskId taskId : activeTasks) {
      Integer currentUsage = Objects.firstNonNull(rackUsage.get(taskId.getRackId()), 0);

      rackUsage.put(taskId.getRackId(), currentUsage + 1);
    }

    double numPerRack = (double) numDesiredInstances / (double) racks.size();
    double numOnRack = Objects.firstNonNull(rackUsage.get(rackId), 0);

    boolean isRackOk = numOnRack < numPerRack;
  
    LOG.trace(String.format("Rack result %s for taskRequest %s, rackId: %s, numPerRack %s, numOnRack %s", isRackOk, taskRequest.getPendingTaskId(), rackId, numPerRack, numOnRack));
    
    return isRackOk;
  }

  private void clearRacks() {
    slaveToRacks.clear();
    racks.clear();
  }

  public void slaveLost(SlaveID slaveId) {
    String rackId = slaveToRacks.remove(slaveId.getValue());

    if (rackId != null) {
      boolean stillPresent = false;

      for (String rackIds : slaveToRacks.values()) {
        if (rackIds.equals(rackId)) {
          stillPresent = true;
          break;
        }
      }

      if (!stillPresent) {
        racks.remove(rackId);
      }
    }
  }

  public void loadRacksFromMaster(MesosMasterStateObject state) {
    clearRacks();

    for (MesosMasterSlaveObject slave : state.getSlaves()) {
      Optional<String> maybeRackId = Optional.fromNullable(slave.getAttributes().get(rackIdAttributeKey));

      saveRackId(slave.getId(), maybeRackId);
    }

    LOG.info(String.format("Found %s racks", racks.size()));

  }

  public String getRackId(Offer offer) {
    for (Attribute attribute : offer.getAttributesList()) {
      if (attribute.getName().equals(rackIdAttributeKey)) {
        return attribute.getText().getValue();
      }
    }
    return defaultRackId;
  }

  private void saveRackId(String slaveId, Optional<String> maybeRackId) {
    final String rackId = maybeRackId.or(defaultRackId);

    slaveToRacks.put(slaveId, rackId);
    racks.add(rackId);
  }

  public void checkOffer(Offer offer) {
    final String slaveId = offer.getSlaveId().getValue();

    if (!slaveToRacks.containsKey(slaveId)) {
      String rackId = getRackId(offer);

      saveRackId(slaveId, Optional.of(rackId));
    }
  }

}
