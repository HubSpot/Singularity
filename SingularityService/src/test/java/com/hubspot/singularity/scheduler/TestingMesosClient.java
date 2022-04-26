package com.hubspot.singularity.scheduler;

import com.hubspot.mesos.client.MesosClient;
import com.hubspot.mesos.json.MesosAgentMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosAgentStateObject;
import com.hubspot.mesos.json.MesosMasterMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestingMesosClient implements MesosClient {

  private Map<String, List<MesosTaskMonitorObject>> agentResourceUsage;
  private Map<String, MesosAgentMetricsSnapshotObject> agentMetrics;

  public TestingMesosClient() {
    this.agentResourceUsage = new HashMap<>();
    this.agentMetrics = new HashMap<>();
  }

  public void setAgentResourceUsage(
    String hostname,
    List<MesosTaskMonitorObject> taskMonitorObjects
  ) {
    agentResourceUsage.put(hostname, taskMonitorObjects);
  }

  public void setAgentMetricsSnapshot(
    String hostname,
    MesosAgentMetricsSnapshotObject snapshotObject
  ) {
    agentMetrics.put(hostname, snapshotObject);
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
  @Deprecated
  public MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(
    String hostname,
    boolean useShortTimeout
  ) {
    return null;
  }

  @Override
  public MesosAgentMetricsSnapshotObject getAgentMetricsSnapshot(
    String hostname,
    boolean useShortTimeout
  ) {
    return agentMetrics.get(hostname);
  }

  @Override
  @Deprecated
  public String getSlaveUri(String hostname) {
    return null;
  }

  @Override
  public String getAgentUri(String hostname) {
    return null;
  }

  @Override
  @Deprecated
  public MesosSlaveStateObject getSlaveState(String uri) {
    return null;
  }

  @Override
  public MesosAgentStateObject getAgentState(String uri) {
    return null;
  }

  @Override
  @Deprecated
  public List<MesosTaskMonitorObject> getSlaveResourceUsage(
    String hostname,
    boolean useShortTimeout
  ) {
    return agentResourceUsage.getOrDefault(hostname, Collections.emptyList());
  }

  @Override
  public List<MesosTaskMonitorObject> getAgentResourceUsage(
    String hostname,
    boolean useShortTimeout
  ) {
    return agentResourceUsage.getOrDefault(hostname, Collections.emptyList());
  }
}
