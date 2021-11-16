package com.hubspot.singularity.helpers;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.TaskManager;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class TaskLagGuardrail {
  private final TaskManager taskManager;
  private final SingularityConfiguration configuration;
  private ConcurrentMap<String, Integer> lateTasksByRequestId;
  private long lastUpdate;

  @Inject
  public TaskLagGuardrail(
    SingularityConfiguration configuration,
    TaskManager taskManager
  ) {
    this.configuration = configuration;
    this.taskManager = taskManager;
    this.lateTasksByRequestId = new ConcurrentHashMap<>();
    this.lastUpdate = 0;
  }

  public boolean isLagged(String requestId) {
    updateLateTasksByRequestId();
    return lateTasksByRequestId.containsKey(requestId);
  }

  private void updateLateTasksByRequestId() {
    long now = System.currentTimeMillis();
    if (now - lastUpdate > 1000L * configuration.getRequestCacheTtl()) {
      List<SingularityPendingTaskId> allPendingTaskIds = taskManager.getPendingTaskIds();
      this.lateTasksByRequestId =
        allPendingTaskIds
          .stream()
          .filter(
            p ->
              now -
              p.getNextRunAt() >
              configuration.getDeltaAfterWhichTasksAreLateMillis()
          )
          .collect(
            Collectors.toConcurrentMap(
              SingularityPendingTaskId::getRequestId,
              p -> 1,
              Integer::sum
            )
          );
      this.lastUpdate = System.currentTimeMillis();
    }
  }
}
