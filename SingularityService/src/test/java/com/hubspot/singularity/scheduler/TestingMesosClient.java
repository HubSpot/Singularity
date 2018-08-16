package com.hubspot.singularity.scheduler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosMasterMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;

public class TestingMesosClient implements MesosClient {

  private Map<String, List<MesosTaskMonitorObject>> slaveResourceUsage;
  private Map<String, MesosSlaveMetricsSnapshotObject> slaveMetrics;

  public TestingMesosClient() {
    this.slaveResourceUsage = new HashMap<>();
    this.slaveMetrics = new HashMap<>();
  }

  public void setSlaveResourceUsage(String hostname, List<MesosTaskMonitorObject> taskMonitorObjects) {
    slaveResourceUsage.put(hostname, taskMonitorObjects);
  }

  public void setSlaveMetricsSnapshot(String hostname, MesosSlaveMetricsSnapshotObject snapshotObject) {
    slaveMetrics.put(hostname, snapshotObject);
  }

  @Override
  public String getMasterUri(String hostnameAndPort) {
    return null;
  }

  @Override
  public String getMasterMetricsSnapshotUri(String hostnameAndPort) {
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
  public MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(String hostname, boolean useShortTimeout) {
    return slaveMetrics.get(hostname);
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
  public List<MesosTaskMonitorObject> getSlaveResourceUsage(String hostname, boolean useShortTimeout) {
    return slaveResourceUsage.getOrDefault(hostname, Collections.emptyList());
  }

}
