package com.hubspot.mesos.client;

import java.util.List;

import com.hubspot.mesos.json.MesosMasterMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosMasterStateObject;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.mesos.json.MesosSlaveStateObject;
import com.hubspot.mesos.json.MesosTaskMonitorObject;

public interface MesosClient {

  public String getMasterUri(String hostnameAndPort);

  default String getMetricsSnapshotUri(String hostnameAndPort) {
    return getMasterMetricsSnapshotUri(hostnameAndPort);
  }

  public String getMasterMetricsSnapshotUri(String hostnameAndPort);

  public static class MesosClientException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public MesosClientException(String message) {
      super(message);
    }

    public MesosClientException(String message, Throwable cause) {
      super(message, cause);
    }

  }

  public MesosMasterStateObject getMasterState(String uri);

  public MesosMasterMetricsSnapshotObject getMasterMetricsSnapshot(String uri);

  default MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(String uri) {
    return getSlaveMetricsSnapshot(uri, false);
  }

  public MesosSlaveMetricsSnapshotObject getSlaveMetricsSnapshot(String uri, boolean useShortTimeout);

  public String getSlaveUri(String hostname);

  public MesosSlaveStateObject getSlaveState(String uri);

  default List<MesosTaskMonitorObject> getSlaveResourceUsage(String hostname) {
    return getSlaveResourceUsage(hostname, false);
  }

  public List<MesosTaskMonitorObject> getSlaveResourceUsage(String hostname, boolean useShortTimeout);

}
