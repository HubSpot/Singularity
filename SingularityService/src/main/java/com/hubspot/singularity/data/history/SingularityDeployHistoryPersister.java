package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityDeployHistoryPersister extends SingularityHistoryPersister<SingularityDeployHistory> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityDeployHistoryPersister.class);

  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  private final SingularitySchedulerLock schedulerLock;

  @Inject
  public SingularityDeployHistoryPersister(SingularityConfiguration configuration,
                                           DeployManager deployManager,
                                           HistoryManager historyManager,
                                           SingularitySchedulerLock schedulerLock,
                                           @Named(SingularityHistoryModule.PERSISTER_LOCK) ReentrantLock persisterLock) {
    super(configuration, persisterLock);

    this.schedulerLock = schedulerLock;
    this.deployManager = deployManager;
    this.historyManager = historyManager;
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Attempting to grab persister lock");
    persisterLock.lock();
    try {
      LOG.info("Acquired persister lock");
      LOG.info("Checking inactive deploys for deploy history persistence");

      final long start = System.currentTimeMillis();
      final LongAdder numTotal = new LongAdder();
      final LongAdder numTransferred = new LongAdder();
      final Map<String, List<SingularityDeployKey>> allDeployIdsByRequest = deployManager.getAllDeployIds()
          .stream()
          .collect(Collectors.groupingBy(
              SingularityDeployKey::getRequestId,
              Collectors.toList()
          ));


      for (String requestId : deployManager.getAllRequestDeployStatesByRequestId().keySet()) {
        LOG.info("Checking deploy histories to persist for request {}", requestId);
        schedulerLock.runWithRequestLock(() -> {
          Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestId);

          if (!deployState.isPresent()) {
            LOG.warn("No request deploy state for {}", requestId);
            return;
          }

          int i = 0;
          List<SingularityDeployHistory> deployHistories = allDeployIdsByRequest.getOrDefault(requestId, Collections.emptyList())
              .stream()
              .map((deployKey) -> deployManager.getDeployHistory(deployKey.getRequestId(), deployKey.getDeployId(), true))
              .filter(Optional::isPresent)
              .map(Optional::get)
              .sorted(Comparator.comparingLong(SingularityDeployHistory::getCreateTimestampForCalculatingHistoryAge).reversed())
              .collect(Collectors.toList());

          for (SingularityDeployHistory deployHistory : deployHistories) {
            numTotal.increment();
            if (!shouldTransferDeploy(requestId, deployState.get(), deployHistory.getDeployMarker().getDeployId())) {
              continue;
            }

            LOG.info("Persisting deploy {} for request {}", deployHistory.getDeployMarker().getDeployId(), requestId);
            if (moveToHistoryOrCheckForPurge(deployHistory, i++)) {
              numTransferred.increment();
            }
          }
        }, requestId, getClass().getSimpleName());
      }

      LOG.info("Transferred {} out of {} deploys in {}", numTransferred, numTotal, JavaUtils.duration(start));
    } finally {
      persisterLock.unlock();
    }
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(configuration.getDeleteDeploysFromZkWhenNoDatabaseAfterHours());
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxStaleDeploysPerRequestInZkWhenNoDatabase();
  }

  private boolean shouldTransferDeploy(String requestId, SingularityRequestDeployState deployState, String deployId) {
    if (deployState == null) {
      LOG.warn("Missing request deploy state for request {}. deploy {}", requestId, deployId);
      return true;
    }

    if (deployState.getActiveDeploy().isPresent() && deployState.getActiveDeploy().get().getDeployId().equals(deployId)) {
      return false;
    }

    if (deployState.getPendingDeploy().isPresent() && deployState.getPendingDeploy().get().getDeployId().equals(deployId)) {
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
