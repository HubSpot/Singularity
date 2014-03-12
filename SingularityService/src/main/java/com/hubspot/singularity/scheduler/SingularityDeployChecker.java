package com.hubspot.singularity.scheduler;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;

public class SingularityDeployChecker {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployChecker.class);

  private final DeployManager deployManager;
  
  @Inject
  public SingularityDeployChecker(DeployManager deployManager) {
    this.deployManager = deployManager;
  }
  
  public void check() {
   // a deploy is successful if the tasks get to TASK_RUNNING and they pass health checks.
   // if there are no health checks, they simply need to hit TASK_RUNNING
   
   SingularitySchedulerStateCache stateCache = null;
   TaskManager taskManager;
    
   final List<SingularityDeployMarker> activeDeploys = deployManager.getActiveDeploys();
   final Map<SingularityDeployMarker, SingularityDeployKey> markerToKey = SingularityDeployKey.fromDeployMarkers(activeDeploys);
   final Map<SingularityDeployKey, SingularityDeploy> deployKeyToDeploy = deployManager.getDeploysForKeys(markerToKey.values());
   
   final List<SingularityTaskId> activeTaskIds = stateCache.getActiveTaskIds();
   
   for (SingularityDeployMarker activeDeployMarker : activeDeploys) {
     final SingularityDeployKey deployKey = markerToKey.get(activeDeployMarker);
     final SingularityDeploy deploy = deployKeyToDeploy.get(deployKey);

     final List<SingularityTaskId> matchingActiveTasks = SingularityTaskId.filter(activeTaskIds, activeDeployMarker.getRequestId());
     
     
     
     
   }
    
  }
  
  
  

  
}
