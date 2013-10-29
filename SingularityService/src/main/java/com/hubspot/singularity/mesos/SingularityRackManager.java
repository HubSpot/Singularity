package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Attribute;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.SlaveID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.json.MesosSlaveObject;
import com.hubspot.mesos.json.MesosStateObject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.MesosConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class SingularityRackManager {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityRackManager.class);
  
  private final static String MASTER_STATE_FORMAT = "http://%s:%s/master/state.json";
  
  private final String rackIdAttributeKey;
  
  private final Map<String, String> slaveToRacks;
  private final Set<String> racks;
  
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  
  @Inject
  public SingularityRackManager(ObjectMapper objectMapper, MesosConfiguration mesosConfiguration) {
    this.objectMapper = objectMapper;
    this.rackIdAttributeKey = mesosConfiguration.getRackIdAttributeKey();
    
    asyncHttpClient = new AsyncHttpClient();
  
    slaveToRacks = Maps.newHashMap();
    racks = Sets.newHashSet();
  }
  
  private String getStateUri(MasterInfo masterInfo) {
    return String.format(MASTER_STATE_FORMAT, InetAddresses.fromInteger(masterInfo.getIp()).getHostAddress(), masterInfo.getPort());
  }
  
  // TODO log or better reasoning behind why certain rack choices are made (maybe an ENUM?)
  public boolean checkRack(Protos.Offer offer, SingularityTaskRequest taskRequest, List<SingularityTaskId> activeTasks) {
    Optional<String> rackId = getRackId(offer);
    
    if (!rackId.isPresent()) {
      if (racks.isEmpty()) {
        return true;
      } else {
        return false;
      }
    }  
    
    int numDesiredInstances = taskRequest.getRequest().getInstances();
    
    Map<String, Integer> rackUsage = Maps.newHashMap();
    
    for (SingularityTaskId taskId : activeTasks) {
      Integer currentUsage = Objects.firstNonNull(rackUsage.get(taskId.getRackId()), 0);
      
      rackUsage.put(taskId.getRackId(), currentUsage + 1);
    }
    
    double numPerRack = (double) numDesiredInstances / (double) racks.size();
    double numOnRack = Objects.firstNonNull(rackUsage.get(rackId.get()), 0);
    
    return numOnRack < numPerRack;
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
  
  public void loadRacksFromMaster(MasterInfo masterInfo) {
    clearRacks();
    
    final String uri = getStateUri(masterInfo);
    
    LOG.info("Fetching rack data from: " + uri);
    
    try {
      Response response = asyncHttpClient.prepareGet(uri).execute().get();
      
      Preconditions.checkState(response.getStatusCode() >= 200 && response.getStatusCode() < 300, "Invalid response from master %s : %s", uri, response.getStatusCode());

      MesosStateObject state = objectMapper.readValue(response.getResponseBodyAsStream(), MesosStateObject.class);
      
      for (MesosSlaveObject slave : state.getSlaves()) {
        String rackId = slave.getAttributes().get(rackIdAttributeKey); 

        if (rackId != null) {
          saveRackId(slave.getId(), rackId);
        }
      }
      
      LOG.info(String.format("Found %s racks", racks.size()));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public Optional<String> getRackId(Offer offer) {
    for (Attribute attribute : offer.getAttributesList()) {
      if (attribute.getName().equals(rackIdAttributeKey)) {
        return Optional.of(attribute.getText().getValue());
      }
    }
    return Optional.absent();
  }
  
  private void saveRackId(String slaveId, String rackId) {
    slaveToRacks.put(slaveId, rackId);
    racks.add(rackId);
  }
  
  public void checkOffer(Offer offer) {
    final String slaveId = offer.getSlaveId().getValue();
    if (!slaveToRacks.containsKey(slaveId)) {
      Optional<String> rackId = getRackId(offer);
      
      if (rackId.isPresent()) {
        saveRackId(slaveId, rackId.get());
      }
    }
  }

}
