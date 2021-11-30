package com.hubspot.singularity.helpers;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TaskLagGuardrail {
  private final TaskManager taskManager;
  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executor;
  private ConcurrentMap<String, Integer> lateTasksByRequestId;

  @Inject
  public TaskLagGuardrail(
    SingularityConfiguration configuration,
    SingularityManagedScheduledExecutorServiceFactory factory,
    TaskManager taskManager
  ) {
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.lateTasksByRequestId = new ConcurrentHashMap<>();
    this.executor = factory.getSingleThreaded(getClass().getSimpleName());
    executor.scheduleWithFixedDelay(
      this::updateLateTasksByRequestId,
      0,
      configuration.getRequestCacheTtl(),
      TimeUnit.SECONDS
    );
  }

  public boolean isLagged(String requestId) {
    return lateTasksByRequestId.containsKey(requestId);
  }

  public void updateLateTasksByRequestId() {
    long now = System.currentTimeMillis();
    List<SingularityPendingTaskId> allPendingTaskIds = taskManager.getPendingTaskIds();

    // not a thread safe assignment, but should be fine for periodic updates
    this.lateTasksByRequestId =
      allPendingTaskIds
        .stream()
        .filter(
          p ->
            now - p.getNextRunAt() > configuration.getDeltaAfterWhichTasksAreLateMillis()
        )
        .collect(
          Collectors.toConcurrentMap(
            SingularityPendingTaskId::getRequestId,
            p -> 1,
            Integer::sum
          )
        );
  }
}
