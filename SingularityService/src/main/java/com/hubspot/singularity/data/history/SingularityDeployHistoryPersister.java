package com.hubspot.singularity.data.history;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityDeployHistoryPersister
  extends SingularityHistoryPersister<SingularityDeployHistory> {

  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityDeployHistoryPersister.class
  );

  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  private final SingularitySchedulerLock schedulerLock;

  @Inject
  public SingularityDeployHistoryPersister(
    SingularityConfiguration configuration,
    DeployManager deployManager,
    HistoryManager historyManager,
    SingularitySchedulerLock schedulerLock,
    SingularityManagedThreadPoolFactory managedThreadPoolFactory,
    @Named(SingularityHistoryModule.PERSISTER_LOCK) ReentrantLock persisterLock,
    @Named(
      SingularityHistoryModule.LAST_DEPLOY_PERSISTER_SUCCESS
    ) AtomicLong lastPersisterSuccess
  ) {
    super(configuration, persisterLock, lastPersisterSuccess, managedThreadPoolFactory);
    this.schedulerLock = schedulerLock;
    this.deployManager = deployManager;
    this.historyManager = historyManager;
  }

  @Override
  public void runActionOnPoll() {
    LOG.info("Attempting to grab persister lock");
    persisterLock.lock();
    AtomicBoolean persisterSuccess = new AtomicBoolean(true);
    try {
      LOG.info("Acquired persister lock");
      LOG.info("Checking inactive deploys for deploy history persistence");

      final long start = System.currentTimeMillis();
      final LongAdder numTotal = new LongAdder();
      final LongAdder numTransferred = new LongAdder();
      final Map<String, List<SingularityDeployKey>> allDeployIdsByRequest = deployManager
        .getAllDeployIds()
        .stream()
        .collect(
          Collectors.groupingBy(SingularityDeployKey::getRequestId, Collectors.toList())
        );

      List<CompletableFuture<Void>> futures = new ArrayList<>();
      for (String requestId : deployManager
        .getAllRequestDeployStatesByRequestId()
        .keySet()) {
        futures.add(
          CompletableFuture.runAsync(
            () -> {
              LOG.debug("Checking deploy histories to persist for request {}", requestId);
              schedulerLock.runWithRequestLock(
                () -> {
                  Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(
                    requestId
                  );

                  if (!deployState.isPresent()) {
                    LOG.warn("No request deploy state for {}", requestId);
                    return;
                  }

                  int i = 0;
                  List<SingularityDeployHistory> deployHistories = allDeployIdsByRequest
                    .getOrDefault(requestId, Collections.emptyList())
                    .stream()
                    .map(deployKey ->
                      deployManager.getDeployHistory(
                        deployKey.getRequestId(),
                        deployKey.getDeployId(),
                        true
                      )
                    )
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(
                      Comparator
                        .comparingLong(
                          SingularityDeployHistory::getCreateTimestampForCalculatingHistoryAge
                        )
                        .reversed()
                    )
                    .collect(Collectors.toList());

                  for (SingularityDeployHistory deployHistory : deployHistories) {
                    numTotal.increment();
                    if (
                      !shouldTransferDeploy(
                        requestId,
                        deployState.get(),
                        deployHistory.getDeployMarker().getDeployId()
                      )
                    ) {
                      continue;
                    }

                    LOG.info(
                      "Persisting deploy {} for request {}",
                      deployHistory.getDeployMarker().getDeployId(),
                      requestId
                    );
                    if (moveToHistoryOrCheckForPurge(deployHistory, i++)) {
                      numTransferred.increment();
                    } else {
                      LOG.error("Deploy History Persister failed on {}", deployHistory);
                      persisterSuccess.getAndSet(false);
                    }
                  }
                },
                requestId,
                getClass().getSimpleName(),
                SingularitySchedulerLock.Priority.LOW
              );
            },
            persisterExecutor
          )
        );
      }

      CompletableFutures.allOf(futures).join();

      LOG.info(
        "Transferred {} out of {} deploys in {}",
        numTransferred,
        numTotal,
        JavaUtils.duration(start)
      );
    } finally {
      if (persisterSuccess.get()) {
        lastPersisterSuccess.set(System.currentTimeMillis());
        LOG.info(
          "Finished run on deploy history persister at {}",
          lastPersisterSuccess.get()
        );
      }
      persisterLock.unlock();
    }
  }

  @Override
  protected long getMaxAgeInMillisOfItem() {
    return TimeUnit.HOURS.toMillis(
      configuration.getDeleteDeploysFromZkWhenNoDatabaseAfterHours()
    );
  }

  @Override
  protected Optional<Integer> getMaxNumberOfItems() {
    return configuration.getMaxStaleDeploysPerRequestInZkWhenNoDatabase();
  }

  private boolean shouldTransferDeploy(
    String requestId,
    SingularityRequestDeployState deployState,
    String deployId
  ) {
    if (deployState == null) {
      LOG.warn(
        "Missing request deploy state for request {}. deploy {}",
        requestId,
        deployId
      );
      return true;
    }

    if (
      deployState.getActiveDeploy().isPresent() &&
      deployState.getActiveDeploy().get().getDeployId().equals(deployId)
    ) {
      return false;
    }

    if (
      deployState.getPendingDeploy().isPresent() &&
      deployState.getPendingDeploy().get().getDeployId().equals(deployId)
    ) {
      return false;
    }

    return true;
  }

  @Override
  protected boolean moveToHistory(SingularityDeployHistory deployHistory) {
    try {
      historyManager.saveDeployHistory(deployHistory);
    } catch (Throwable t) {
      LOG.error(
        "Failed to persist deploy {}",
        SingularityDeployKey.fromDeployMarker(deployHistory.getDeployMarker()),
        t
      );
      return false;
    }

    return true;
  }

  @Override
  protected SingularityDeleteResult purgeFromZk(SingularityDeployHistory deployHistory) {
    return deployManager.deleteDeployHistory(
      SingularityDeployKey.fromDeployMarker(deployHistory.getDeployMarker())
    );
  }
}
