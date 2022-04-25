package com.hubspot.singularity.mesos;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.scheduler.SingularityLeaderCacheCoordinator;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.mesos.v1.Protos.Offer;

public class TestMesosSchedulerImpl extends SingularityMesosSchedulerImpl {
  private final SingularityPendingTaskQueueProcessor queueProcessor;

  @Inject
  public TestMesosSchedulerImpl(
    SingularitySchedulerLock lock,
    SingularityExceptionNotifier exceptionNotifier,
    SingularityStartup startup,
    SingularityLeaderCacheCoordinator leaderCacheCoordinator,
    SingularityAbort abort,
    SingularityMesosFrameworkMessageHandler messageHandler,
    SingularityAgentAndRackManager agentAndRackManager,
    SingularityMesosOfferManager offerCache,
    DisasterManager disasterManager,
    SingularityMesosStatusUpdateHandler statusUpdateHandler,
    SingularityMesosSchedulerClient mesosSchedulerClient,
    SingularityConfiguration configuration,
    TaskManager taskManager,
    Transcoder<SingularityTaskDestroyFrameworkMessage> transcoder,
    StatusUpdateQueue queuedUpdates,
    SingularityManagedThreadPoolFactory threadPoolFactory,
    @Named(
      SingularityMainModule.LAST_MESOS_MASTER_HEARTBEAT_TIME
    ) AtomicLong lastHeartbeatTime,
    SingularityPendingTaskQueueProcessor queueProcessor
  ) {
    super(
      lock,
      exceptionNotifier,
      startup,
      leaderCacheCoordinator,
      abort,
      messageHandler,
      agentAndRackManager,
      offerCache,
      disasterManager,
      statusUpdateHandler,
      mesosSchedulerClient,
      configuration,
      taskManager,
      transcoder,
      queuedUpdates,
      threadPoolFactory,
      lastHeartbeatTime
    );
    this.queueProcessor = queueProcessor;
  }

  @Override
  public CompletableFuture<Void> resourceOffers(List<Offer> offers) {
    super.resourceOffers(offers);
    queueProcessor.drainHandledTasks();
    return CompletableFuture.completedFuture(null);
  }
}
