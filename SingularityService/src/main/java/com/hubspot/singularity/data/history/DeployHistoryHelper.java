package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.data.DeployManager;

@Singleton
public class DeployHistoryHelper extends BlendedHistoryHelper<SingularityDeployHistory, String> {

  private final DeployManager deployManager;
  private final HistoryManager historyManager;

  @Inject
  public DeployHistoryHelper(DeployManager deployManager, HistoryManager historyManager) {
    this.deployManager = deployManager;
    this.historyManager = historyManager;
  }

  @Override
  protected List<SingularityDeployHistory> getFromZk(String requestId) {
    final List<SingularityDeployKey> deployKeys = deployManager.getDeployIdsFor(requestId);
    final List<SingularityDeployHistory> histories = Lists.newArrayListWithCapacity(deployKeys.size());

    for (SingularityDeployKey key : deployKeys) {
      Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(key.getRequestId(), key.getDeployId(), false);
      if (deployHistory.isPresent()) {
        histories.add(deployHistory.get());
      }
    }

    Collections.sort(histories);

    return histories;
  }

  @Override
  protected List<SingularityDeployHistory> getFromHistory(String requestId, int historyStart, int numFromHistory) {
    return historyManager.getDeployHistoryForRequest(requestId, historyStart, numFromHistory);
  }

  public boolean isDeployIdAvailable(String requestId, String deployId) {
    Optional<SingularityDeploy> deploy = deployManager.getDeploy(requestId, deployId);

    if (deploy.isPresent()) {
      return false;
    }

    return !historyManager.getDeployHistory(requestId, deployId).isPresent();
  }

  @Override
  protected Optional<Integer> getTotalCount(String requestId) {
    final int numFromZk = deployManager.getDeployIdsFor(requestId).size();
    final int numFromHistory = historyManager.getDeployHistoryForRequestCount(requestId);

    return Optional.of(numFromZk + numFromHistory);
  }
}
