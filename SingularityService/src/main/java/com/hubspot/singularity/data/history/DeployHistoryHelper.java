package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.data.DeployManager;

public class DeployHistoryHelper extends BlendedHistoryHelper<SingularityDeployHistory> {

  private final String requestId;
  private final DeployManager deployManager;
  private final HistoryManager historyManager;

  public DeployHistoryHelper(String requestId, DeployManager deployManager, HistoryManager historyManager) {
    this.requestId = requestId;
    this.deployManager = deployManager;
    this.historyManager = historyManager;
  }

  @Override
  protected List<SingularityDeployHistory> getFromZk() {
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
  protected List<SingularityDeployHistory> getFromHistory(int historyStart, int numFromHistory) {
    return historyManager.getDeployHistoryForRequest(requestId, historyStart, numFromHistory);
  }

}
