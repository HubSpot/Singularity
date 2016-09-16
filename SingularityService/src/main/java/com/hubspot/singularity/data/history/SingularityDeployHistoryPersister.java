package com.hubspot.singularity.data.history;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.TreeMultimap;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;

@Singleton
public class SingularityDeployHistoryPersister extends SingularityHistoryPersister<SingularityDeployHistory> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployHistoryPersister.class);

  private final DeployManager deployManager;
  private final HistoryManager historyManager;

  @Inject
  public SingularityDeployHistoryPersister(SingularityConfiguration configuration, DeployManager deployManager, HistoryManager historyManager) {
    super(configuration);

    this.deployManager = deployManager;
    this.historyManager = historyManager;
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Checking inactive deploys for deploy history persistance");

    final long start = System.currentTimeMillis();

    final List<SingularityDeployKey> allDeployIds = deployManager.getAllDeployIds();
    final Map<String, SingularityRequestDeployState> byRequestId = deployManager.getAllRequestDeployStatesByRequestId();
    final TreeMultimap<String, SingularityDeployHistory> deployHistoryByRequestId = TreeMultimap.create();

    int numTotal = 0;
    int numTransferred = 0;

    for (SingularityDeployKey deployKey : allDeployIds) {
      SingularityRequestDeployState deployState = byRequestId.get(deployKey.getRequestId());

      if (!shouldTransferDeploy(deployState, deployKey)) {
        continue;
      }

      Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(deployKey.getRequestId(), deployKey.getDeployId(), true);

      if (deployHistory.isPresent()) {
        deployHistoryByRequestId.put(deployKey.getRequestId(), deployHistory.get());
      } else {
        LOG.info("Deploy history for key {} not found", deployKey);
      }
    }

    for (Collection<SingularityDeployHistory> deployHistoryForRequest : deployHistoryByRequestId.asMap().values()) {
      int i=0;
      for (SingularityDeployHistory deployHistory : deployHistoryForRequest) {
        if (moveToHistoryOrCheckForPurge(deployHistory, i++)) {
          numTransferred++;
        }

        numTotal++;
      }
    }

    LOG.info("Transferred {} out of {} deploys in {}", numTransferred, numTotal, JavaUtils.duration(start));
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(configuration.getDeleteDeploysFromZkWhenNoDatabaseAfterHours());
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxStaleDeploysPerRequestInZkWhenNoDatabase();
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

  @Override
  protected boolean moveToHistory(SingularityDeployHistory deployHistory) {
    try {
      historyManager.saveDeployHistory(deployHistory);
    } catch (Throwable t) {
      LOG.warn("Failed to persist deploy {}", SingularityDeployKey.fromDeployMarker(deployHistory.getDeployMarker()), t);
      return false;
    }

    return true;
  }

  @Override
  protected SingularityDeleteResult purgeFromZk(SingularityDeployHistory deployHistory) {
    return deployManager.deleteDeployHistory(SingularityDeployKey.fromDeployMarker(deployHistory.getDeployMarker()));
  }

}
