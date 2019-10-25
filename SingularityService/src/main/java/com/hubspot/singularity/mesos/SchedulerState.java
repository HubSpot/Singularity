package com.hubspot.singularity.mesos;

import org.apache.curator.framework.state.ConnectionState;

public class SchedulerState {
  public enum MesosSchedulerState {
    NOT_STARTED,
    SUBSCRIBED,
    STOPPED,
    PAUSED_FOR_MESOS_RECONNECT,
  }

  private MesosSchedulerState mesosSchedulerState = MesosSchedulerState.NOT_STARTED;
  private ConnectionState zkConnectionState = ConnectionState.CONNECTED;

  public boolean isRunning() {
    return mesosSchedulerState != null &&
        mesosSchedulerState == MesosSchedulerState.SUBSCRIBED &&
        zkConnectionState != null &&
        zkConnectionState.isConnected();
  }

  public MesosSchedulerState getMesosSchedulerState() {
    return mesosSchedulerState;
  }

  public void setMesosSchedulerState(MesosSchedulerState mesosSchedulerState) {
    this.mesosSchedulerState = mesosSchedulerState;
  }

  public ConnectionState getZkConnectionState() {
    return zkConnectionState;
  }

  public void setZkConnectionState(ConnectionState zkConnectionState) {
    this.zkConnectionState = zkConnectionState;
  }
}
