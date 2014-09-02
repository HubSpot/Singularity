package com.hubspot.singularity;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;

import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;

public class SingularityStateGenerator {

  private final SingularityLeaderController managed;

  public SingularityStateGenerator(SingularityLeaderController managed) {
    this.managed = managed;
  }

  public SingularityHostState getState() {
    final boolean isMaster = managed.isMaster();
    final Protos.Status driverStatus = managed.getCurrentStatus();

    final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    final long uptime = mxBean.getUptime();

    final long now = System.currentTimeMillis();
    final Optional<Long> lastOfferTimestamp = managed.getLastOfferTimestamp();
    final Optional<Long> millisSinceLastOfferTimestamp = lastOfferTimestamp.isPresent() ? Optional.of(now - lastOfferTimestamp.get()) : Optional.<Long> absent();

    String hostAddress = null;

    try {
      hostAddress = JavaUtils.getHostAddress();
    } catch (Exception e) {
      hostAddress = "Unknown";
    }

    String mesosMaster = null;
    Optional<MasterInfo> mesosMasterInfo = managed.getMaster();

    if (mesosMasterInfo.isPresent()) {
      mesosMaster = MesosUtils.getMasterHostAndPort(mesosMasterInfo.get());
    }

    final SingularityHostState hostState = new SingularityHostState(isMaster, uptime, driverStatus.name(), millisSinceLastOfferTimestamp, hostAddress, JavaUtils.getHostName(), mesosMaster);

    return hostState;
  }


}
