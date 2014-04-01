package com.hubspot.singularity.scheduler;

import java.util.concurrent.ScheduledExecutorService;

import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.hooks.LoadBalancerClient;

public class SingularityDeployLoadBalancerCheckCommand implements Runnable {

  private final ScheduledExecutorService scheduledExecutorService;
  private final LoadBalancerClient lbClient;
  
  public SingularityDeployLoadBalancerCheckCommand(LoadBalancerClient lbClient, SingularityPendingDeploy pendingDeploy, ScheduledExecutorService scheduledExecutorService) {
    this.lbClient = lbClient;
    this.scheduledExecutorService = scheduledExecutorService;
  }
  
  @Override
  public void run() {
    try {
      
    } catch (Throwable t) {
      
    }
  }
  
  private void doit() {
    
    
  }

}
