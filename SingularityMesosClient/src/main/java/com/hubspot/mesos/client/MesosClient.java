package com.hubspot.mesos.client;

import com.hubspot.mesos.json.MesosAgentMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosAgentStateObject;
import com.hubspot.mesos.json.MesosMasterMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;
import java.util.List;

public interface MesosClient {
  String getMasterUri(String hostnameAndPort);

  default String getMetricsSnapshotUri(String hostnameAndPort) {
    return getMasterMetricsSnapshotUri(hostnameAndPort);
  }

  String getMasterMetricsSnapshotUri(String hostnameAndPort);

  class MesosClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MesosClientException(String message) {
      super(message);
    }

    public MesosClientException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  MesosMasterStateObject getMasterState(String uri);

  MesosMasterMetricsSnapshotObject getMasterMetricsSnapshot(String uri);

  /**
   * @deprecated use {@link MesosClient#getAgentMetricsSnapshot(String)}}
   */
  @Deprecated
  default MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(String uri) {
    return getSlaveMetricsSnapshot(uri, false);
  }

  /**
   * @deprecated use {@link MesosClient#getAgentMetricsSnapshot(String, boolean)}
   */
  @Deprecated
  MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(
    String uri,
    boolean useShortTimeout
  );

  default MesosAgentMetricsSnapshotObject getAgentMetricsSnapshot(String uri) {
    return getAgentMetricsSnapshot(uri, false);
  }

  MesosAgentMetricsSnapshotObject getAgentMetricsSnapshot(
    String uri,
    boolean useShortTimeout
  );

  /**
   * @deprecated use {@link MesosClient#getAgentUri(String)}
   */
  @Deprecated
  String getSlaveUri(String hostname);

  String getAgentUri(String hostname);

  /**
   * @deprecated use {@link MesosClient#getAgentState(String)}
   */
  @Deprecated
  MesosSlaveStateObject getSlaveState(String uri);

  MesosAgentStateObject getAgentState(String uri);

  /**
   * @deprecated use {@link MesosClient#getAgentState(String)}
   */
  @Deprecated
  default List<MesosTaskMonitorObject> getSlaveResourceUsage(String hostname) {
    return getSlaveResourceUsage(hostname, false);
  }

  default List<MesosTaskMonitorObject> getAgentResourceUsage(String hostname) {
    return getAgentResourceUsage(hostname, false);
  }

  /**
   * @deprecated use {@link MesosClient#getAgentState(String)}
   */
  @Deprecated
  List<MesosTaskMonitorObject> getSlaveResourceUsage(
    String hostname,
    boolean useShortTimeout
  );

  List<MesosTaskMonitorObject> getAgentResourceUsage(
    String hostname,
    boolean useShortTimeout
  );
}
