package com.hubspot.singularity.data;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TaskRequestManager {
  private static final Logger LOG = LoggerFactory.getLogger(TaskRequestManager.class);

  private final DeployManager deployManager;
  private final RequestManager requestManager;

  @Inject
  public TaskRequestManager(DeployManager deployManager, RequestManager requestManager) {
    this.deployManager = deployManager;
    this.requestManager = requestManager;
  }

  public SingularityTaskRequest getTaskRequest(SingularityPendingTask task) {
    Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(
      task.getPendingTaskId().getRequestId()
    );
    if (!maybeRequest.isPresent()) {
      LOG.warn("Could not find request for {}", task.getPendingTaskId());
      return null;
    }
    SingularityRequestWithState request = maybeRequest.get();
    if (!request.getState().isRunnable()) {
      LOG.warn("Request was in state {} for pending task {}", request.getState(), task);
      return null;
    }

    Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(
      task.getPendingTaskId().getRequestId(),
      task.getPendingTaskId().getDeployId()
    );
    if (!maybeDeploy.isPresent()) {
      LOG.warn("Could not find deploy for {}", task.getPendingTaskId());
      return null;
    }
    SingularityDeploy deploy = maybeDeploy.get();

    Optional<SingularityPendingDeploy> maybePendingDeploy = deployManager.getPendingDeploy(
      request.getRequest().getId()
    );

    Optional<SingularityRequest> updatedRequest = maybePendingDeploy.isPresent() &&
      maybePendingDeploy
        .get()
        .getDeployMarker()
        .getDeployId()
        .equals(task.getPendingTaskId().getDeployId())
      ? maybePendingDeploy.get().getUpdatedRequest()
      : Optional.<SingularityRequest>empty();

    return new SingularityTaskRequest(
      updatedRequest.orElse(request.getRequest()),
      deploy,
      task
    );
  }

  public List<SingularityTaskRequest> getTaskRequests(
    List<SingularityPendingTask> tasks
  ) {
    final Multimap<String, SingularityPendingTask> requestIdToPendingTaskId = ArrayListMultimap.create(
      tasks.size(),
      1
    );

    for (SingularityPendingTask task : tasks) {
      requestIdToPendingTaskId.put(task.getPendingTaskId().getRequestId(), task);
    }

    final List<SingularityRequestWithState> matchingRequests = requestManager.getRequests(
      requestIdToPendingTaskId.keySet()
    );

    final Map<SingularityPendingTask, SingularityDeployKey> deployKeys = SingularityDeployKey.fromPendingTasks(
      requestIdToPendingTaskId.values()
    );
    final Map<SingularityDeployKey, SingularityDeploy> matchingDeploys = deployManager.getDeploysForKeys(
      Sets.newHashSet(deployKeys.values())
    );

    final List<SingularityTaskRequest> taskRequests = Lists.newArrayListWithCapacity(
      matchingRequests.size()
    );

    for (SingularityRequestWithState request : matchingRequests) {
      Optional<SingularityPendingDeploy> maybePendingDeploy = deployManager.getPendingDeploy(
        request.getRequest().getId()
      );
      for (SingularityPendingTask task : requestIdToPendingTaskId.get(
        request.getRequest().getId()
      )) {
        SingularityDeploy foundDeploy = matchingDeploys.get(deployKeys.get(task));

        if (foundDeploy == null) {
          LOG.warn("Couldn't find a matching deploy for pending task {}", task);
          continue;
        }

        if (!request.getState().isRunnable()) {
          LOG.warn(
            "Request was in state {} for pending task {}",
            request.getState(),
            task
          );
          continue;
        }

        Optional<SingularityRequest> updatedRequest = maybePendingDeploy.isPresent() &&
          maybePendingDeploy
            .get()
            .getDeployMarker()
            .getDeployId()
            .equals(task.getPendingTaskId().getDeployId())
          ? maybePendingDeploy.get().getUpdatedRequest()
          : Optional.<SingularityRequest>empty();

        taskRequests.add(
          new SingularityTaskRequest(
            updatedRequest.orElse(request.getRequest()),
            foundDeploy,
            task
          )
        );
      }
    }

    return taskRequests;
  }
}
