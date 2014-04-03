package com.hubspot.singularity.data.history;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.data.DeployManager;

public class SingularityDeployHistoryPersister {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployHistoryPersister.class);
  
  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  
  @Inject
  public SingularityDeployHistoryPersister(DeployManager deployManager, HistoryManager historyManager) {
    this.deployManager = deployManager;
    this.historyManager = historyManager;
  }
  
  public void checkInactiveDeploys() {
    LOG.info("Checking inactive deploys for deploy history persistance");
    
    final long start = System.currentTimeMillis();
    
    final List<SingularityDeployKey> allDeployIds = deployManager.getAllDeployIds();
    final List<SingularityRequestDeployState> allRequestDeployStates = deployManager.getAllRequestDeployStates();
    final Map<String, SingularityRequestDeployState> byRequestId = Maps.uniqueIndex(allRequestDeployStates, new Function<SingularityRequestDeployState, String>() {

      @Override
      public String apply(SingularityRequestDeployState input) {
        return input.getRequestId();
      }
      
    });
    
    int numTotal = 0;
    int numTransferred = 0;
    
    for (SingularityDeployKey deployKey : allDeployIds) {
      SingularityRequestDeployState deployState = byRequestId.get(deployKey.getRequestId());
      
      if (!shouldTransferDeploy(deployState, deployKey)) {
        continue;
      }
      
      if (transferToHistoryDB(deployKey)) {
        numTransferred++;
      }
      
      numTotal++;
    }
    
    LOG.info("Transferred {} out of {} deploys in {}", numTransferred, numTotal, Utils.duration(start));
  }
  
  private boolean shouldTransferDeploy(SingularityRequestDeployState deployState, SingularityDeployKey deployKey) {
    if (deployState == null) {
      LOG.warn("Missing request deploy state for deployKey {}", deployKey);
      return true;
    }
    
    if (deployState.getActiveDeploy().isPresent() && deployState.getActiveDeploy().get().getDeployId().equals(deployKey.getDeployId())) {
      return false;
    }
   
    if (deployState.getPendingDeploy().isPresent() && deployState.getPendingDeploy().get().getDeployId().equals(deployKey.getDeployId())) {
      return false;
    }
    
    return true;
  }
  
  private boolean transferToHistoryDB(SingularityDeployKey deployKey) {
    final long start = System.currentTimeMillis();
    
    Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(deployKey.getRequestId(), deployKey.getDeployId());
    
    if (deployHistory.isPresent()) {
      LOG.info("Deploy history for key {} not found", deployKey);
      return false;
    }
    
    try {
      historyManager.saveDeployHistory(deployHistory.get());
    } catch (Throwable t) {
      LOG.warn("Failed to persist deploy history {} into History for deploy {}", deployHistory.get(), deployKey, t);
      return false;
    }
    
    deployManager.deleteDeployHistory(deployKey);
    
    LOG.debug("Moved deploy history for {} from ZK to History in {}", deployKey, Utils.duration(start));
  
    return true;
  }

}
