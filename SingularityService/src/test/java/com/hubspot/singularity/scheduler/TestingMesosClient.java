package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosMasterMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;

public class TestingMesosClient implements MesosClient {

  private Map<String, List<MesosTaskMonitorObject>> slaveResourceUsage;

  public TestingMesosClient() {
    this.slaveResourceUsage = new HashMap<>();
  }

  public void setSlaveResourceUsage(String hostname, List<MesosTaskMonitorObject> taskMonitorObjects) {
    slaveResourceUsage.put(hostname, taskMonitorObjects);
  }

  @Override
  public String getMasterUri(String hostnameAndPort) {
    return null;
  }

  @Override
  public String getMetricsSnapshotUri(String hostnameAndPort) {
    return null;
  }

  @Override
  public MesosMasterStateObject getMasterState(String uri) {
    return null;
  }

  @Override
  public MesosMasterMetricsSnapshotObject getMasterMetricsSnapshot(String uri) {
    return null;
  }

  @Override
  public String getSlaveUri(String hostname) {
    return null;
  }

  @Override
  public MesosSlaveStateObject getSlaveState(String uri) {
    return null;
  }

  @Override
  public List<MesosTaskMonitorObject> getSlaveResourceUsage(String hostname) {
    if (!slaveResourceUsage.containsKey(hostname)) {
      return Collections.emptyList();
    }
    return slaveResourceUsage.get(hostname);
  }

}
