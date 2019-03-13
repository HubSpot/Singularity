package com.hubspot.singularity.data.history;

import java.util.Collections;
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
        schedulerLock.runWithRequestLock(() -> {
          Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestId);

          if (!deployState.isPresent()) {
            LOG.warn("No request deploy state for {}", requestId);
            return;
          }

          int i = 0;
          for (SingularityDeployKey deployKey : allDeployIdsByRequest.getOrDefault(requestId, Collections.emptyList())) {

            if (!shouldTransferDeploy(deployState.get(), deployKey)) {
              return;
            }

            Optional<SingularityDeployHistory> deployHistory = deployManager.getDeployHistory(deployKey.getRequestId(), deployKey.getDeployId(), true);

            if (deployHistory.isPresent()) {
              if (moveToHistoryOrCheckForPurge(deployHistory.get(), i++)) {
                numTransferred.increment();
              }
              numTotal.increment();
            } else {
              LOG.info("Deploy history for key {} not found", deployKey);
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
