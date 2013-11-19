package com.hubspot.singularity;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

import org.apache.mesos.Protos;

import com.hubspot.mesos.JavaUtils;

public class SingularityStateGenerator {

  private final SingularityManaged managed;
  
  public SingularityStateGenerator(SingularityManaged managed) {
    this.managed = managed;
  }

  public SingularityHostState getState() {
    final boolean isMaster = managed.isMaster();
    final Protos.Status driverStatus = managed.getCurrentStatus();
    
    final RuntimeMXBean mxBean = ManagementFactory.getRuntimeMXBean();
    final long uptime = mxBean.getUptime();
    
    final long now = System.currentTimeMillis();
    final long lastOfferTimestamp = managed.getLastOfferTimestamp();
    final long millisSinceLastOfferTimestamp = now - lastOfferTimestamp;
    
    String hostAddress = null;
    
    try {
      hostAddress = JavaUtils.getHostAddress();
    } catch (Exception e) {
      hostAddress = "Unknown";
    }
        
    final SingularityHostState hostState = new SingularityHostState(isMaster, uptime, driverStatus.name(), millisSinceLastOfferTimestamp, hostAddress, JavaUtils.getHostName());

    return hostState;
  }
  
  
}
