package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityCheckingUpstreamsUpdate;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdsByStatus;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.RequestHelper;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;

@Singleton
public class SingularityUpstreamChecker {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityUpstreamChecker.class);
  private static final Predicate<SingularityLoadBalancerUpdate> IS_WAITING_STATE = singularityLoadBalancerUpdate -> singularityLoadBalancerUpdate.getLoadBalancerState() == BaragonRequestState.WAITING;

  private final LoadBalancerClient lbClient;
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final RequestHelper requestHelper;
  private final SingularitySchedulerLock lock;

  @Inject
  public SingularityUpstreamChecker(LoadBalancerClient lbClient,
                                    TaskManager taskManager,
                                    RequestManager requestManager,
                                    DeployManager deployManager,
                                    RequestHelper requestHelper,
                                    SingularitySchedulerLock lock) {
    this.lbClient = lbClient;
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.deployManager = deployManager;
    this.requestHelper = requestHelper;
    this.lock = lock;
  }

  private static class TaskIdNotFoundException extends Exception {
    private TaskIdNotFoundException(String message) {
      super(message);
    }
  }

  private List<SingularityTask> getActiveHealthyAndCleaningTasksForService(String requestId) throws TaskIdNotFoundException {
    final Optional<SingularityTaskIdsByStatus> taskIdsByStatusForRequest = requestHelper.getTaskIdsByStatusForRequest(requestId);
    if (taskIdsByStatusForRequest.isPresent()) {
      List<SingularityTaskId> activeHealthyAndCleaningTaskIdsForRequest = new ArrayList<>();
      activeHealthyAndCleaningTaskIdsForRequest.addAll(taskIdsByStatusForRequest.get().getHealthy());
      activeHealthyAndCleaningTaskIdsForRequest.addAll(taskIdsByStatusForRequest.get().getCleaning());
      final Map<SingularityTaskId, SingularityTask> activeHealthyAndCleaningTasksForRequest = taskManager.getTasks(activeHealthyAndCleaningTaskIdsForRequest);
      return new ArrayList<>(activeHealthyAndCleaningTasksForRequest.values());
    }
    throw new TaskIdNotFoundException("TaskId not found");
  }

  private Collection<UpstreamInfo> getUpstreamsFromActiveHealthyAndCleaningTasksForService(String singularityRequestId, Optional<String> loadBalancerUpstreamGroup) throws TaskIdNotFoundException {
    final List<SingularityTask> activeHealthyAndCleaningTasksForService = getActiveHealthyAndCleaningTasksForService(singularityRequestId);
    return lbClient.getUpstreamsForTasks(activeHealthyAndCleaningTasksForService, singularityRequestId, loadBalancerUpstreamGroup);
  }

  /**
   * @return a collection of upstreams in the upstreams param that match with the upstream param on upstream and group.
   * We expect that the collection will have a maximum of one match, but we will keep it as a collection just in case.
   */
  private Collection<UpstreamInfo> getEqualUpstreams(UpstreamInfo upstream, Collection<UpstreamInfo> upstreams) {
    return upstreams.stream().filter(candidate -> UpstreamInfo.upstreamAndGroupMatches(candidate, upstream)).collect(Collectors.toList());
  }

  private List<UpstreamInfo> getExtraUpstreamsInLoadBalancer(Collection<UpstreamInfo> upstreamsInLoadBalancerForService, Collection<UpstreamInfo> upstreamsInSingularityForService) {
    for (UpstreamInfo upstreamInSingularity : upstreamsInSingularityForService) {
      final Collection<UpstreamInfo> matches = getEqualUpstreams(upstreamInSingularity, upstreamsInLoadBalancerForService);
      upstreamsInLoadBalancerForService.removeAll(matches);
    }
    return new ArrayList<>(upstreamsInLoadBalancerForService);
  }

  private Collection<UpstreamInfo> getLoadBalancerUpstreamsForServiceHelper(SingularityCheckingUpstreamsUpdate singularityCheckingUpstreamsUpdate, Optional<String> loadBalancerUpstreamGroup) {
    if (singularityCheckingUpstreamsUpdate.getBaragonServiceState().isPresent()){
      LOG.debug("Baragon service state for service {} is present.", singularityCheckingUpstreamsUpdate.getSingularityRequestId());
      final BaragonServiceState baragonServiceState = singularityCheckingUpstreamsUpdate.getBaragonServiceState().get();
      return baragonServiceState.getUpstreams()
          .stream()
          .filter(upstream -> upstream.getGroup().equals(loadBalancerUpstreamGroup.orElse("default")))
          .collect(Collectors.toList());
    }
    LOG.debug("Baragon service state for service {} is absent.", singularityCheckingUpstreamsUpdate.getSingularityRequestId());
    return Collections.emptyList();
  }

  private Collection<UpstreamInfo> getLoadBalancerUpstreamsForService(String singularityRequestId, Optional<String> loadBalancerServiceIdOverride, Optional<String> loadBalancerUpstreamGroup) {
    final String loadBalancerServiceId = loadBalancerServiceIdOverride.orElse(singularityRequestId);
    try {
      LOG.info("Sending request to get load balancer upstreams for service {} with loadBalancerServiceId {}.", singularityRequestId, loadBalancerServiceId);
      final SingularityCheckingUpstreamsUpdate checkUpstreamsState = lbClient.getLoadBalancerServiceStateForRequest(loadBalancerServiceId);
      LOG.debug("Succeeded getting load balancer upstreams for singularity request {} with loadBalancerServiceId {}. State is {}.", singularityRequestId, loadBalancerServiceId, checkUpstreamsState.toString());
      return getLoadBalancerUpstreamsForServiceHelper(checkUpstreamsState, loadBalancerUpstreamGroup);
    } catch (Exception e) {
      LOG.error("Failed getting load balancer upstreams for singularity request {} with loadBalancerServiceId {}. ", singularityRequestId, loadBalancerServiceId, e);
    }
    return Collections.emptyList();
  }

  private Optional<SingularityLoadBalancerUpdate> syncUpstreamsForServiceHelper(SingularityRequest singularityRequest, SingularityDeploy deploy, Optional<String> loadBalancerUpstreamGroup) {
    try {
      LOG.debug("Checking load balancer upstreams for service {}.", singularityRequest.getId());
      Collection<UpstreamInfo> upstreamsInLoadBalancerForService = getLoadBalancerUpstreamsForService(singularityRequest.getId(), deploy.getLoadBalancerServiceIdOverride(), loadBalancerUpstreamGroup);
      LOG.debug("Upstreams in load balancer for singularity service {} are {}.", singularityRequest.getId(), upstreamsInLoadBalancerForService);
      Collection<UpstreamInfo> upstreamsInSingularityForService = getUpstreamsFromActiveHealthyAndCleaningTasksForService(singularityRequest.getId(), loadBalancerUpstreamGroup);
      LOG.debug("Upstreams in singularity for service {} are {}.", singularityRequest.getId(), upstreamsInSingularityForService);
      final List<UpstreamInfo> extraUpstreams = getExtraUpstreamsInLoadBalancer(upstreamsInLoadBalancerForService, upstreamsInSingularityForService);
      if (extraUpstreams.isEmpty()) {
        LOG.debug("No extra upstreams for service {}. No load balancer request sent.", singularityRequest.getId());
        return Optional.empty();
      }

      if (extraUpstreams.containsAll(upstreamsInLoadBalancerForService) && extraUpstreams.size() == upstreamsInLoadBalancerForService.size()) {
        throw new IllegalStateException(String.format("Would remove all remaining upstreams for %s in LB, skipping", singularityRequest.getId()));
      }

      final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(String.format("%s-%s-%s", singularityRequest.getId(), deploy.getId(), System.currentTimeMillis()), LoadBalancerRequestType.REMOVE, Optional.empty());
      LOG.info("Syncing upstreams for service {}. Making and sending load balancer request {} to remove {} extra upstreams. The upstreams removed are: {}.", singularityRequest.getId(), loadBalancerRequestId, extraUpstreams.size(), extraUpstreams);
      return Optional.of(lbClient.makeAndSendLoadBalancerRequest(loadBalancerRequestId, Collections.emptyList(), extraUpstreams, deploy, singularityRequest));
    } catch (TaskIdNotFoundException e) {
      LOG.error("TaskId not found for requestId: {}.", singularityRequest.getId());
      return Optional.empty();
    }
  }

  private void syncUpstreamsForService(SingularityRequest singularityRequest) {
    final String singularityRequestId = singularityRequest.getId();
    LOG.debug("Starting syncing of upstreams for service: {}.", singularityRequestId);
    if (!singularityRequest.isLoadBalanced()) {
      LOG.debug("Singularity service {} is not load balanced. Terminating syncing.", singularityRequestId);
      return;
    }
    final Optional<String> maybeDeployId = deployManager.getActiveDeployId(singularityRequestId);
    if (!maybeDeployId.isPresent()) {
      LOG.debug("Active deploy for service {} is absent. Terminating syncing.", singularityRequestId);
      return;
    }
    final Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(singularityRequestId, maybeDeployId.get());
    if (!maybeDeploy.isPresent()) {
      LOG.debug("Deploy for service {} with deployId {} is absent. Terminating syncing.", singularityRequestId, maybeDeployId.get());
      return;
    }
    final Optional<SingularityLoadBalancerUpdate> maybeSyncUpstreamsUpdate = syncUpstreamsForServiceHelper(singularityRequest, maybeDeploy.get(), maybeDeploy.get().getLoadBalancerUpstreamGroup());
    if (!maybeSyncUpstreamsUpdate.isPresent()) {
      LOG.debug("Update of syncing of upstreams for service {} and deploy {} is absent. Terminating syncing.", singularityRequestId, maybeDeployId.get());
      return;
    }
    checkSyncUpstreamsState(maybeSyncUpstreamsUpdate.get().getLoadBalancerRequestId(), singularityRequestId);
  }

  private void checkSyncUpstreamsState(LoadBalancerRequestId loadBalancerRequestId, String singularityRequestId) {
    Retryer<SingularityLoadBalancerUpdate> syncingRetryer = RetryerBuilder.<SingularityLoadBalancerUpdate>newBuilder()
        .retryIfException()
        .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
        .retryIfResult(IS_WAITING_STATE)
        .build();
    try {
      LOG.info("Checking load balancer request to sync upstreams for service {} using a retryer until the request state is no longer waiting.", singularityRequestId);
      SingularityLoadBalancerUpdate syncUpstreamsState = syncingRetryer.call(() -> lbClient.getState(loadBalancerRequestId));
      if (syncUpstreamsState.getLoadBalancerState() == BaragonRequestState.SUCCESS){
        LOG.debug("Syncing upstreams for singularity request {} is {}.", singularityRequestId, syncUpstreamsState);
      } else {
        LOG.error("Syncing upstreams for singularity request {} is {}.", singularityRequestId, syncUpstreamsState);
      }
    } catch (Exception e) {
      LOG.error("Could not check sync upstream state for singularity request {}. ", singularityRequestId, e);
    }
  }

  public void syncUpstreams() {
    for (SingularityRequestWithState singularityRequestWithState: requestManager.getActiveRequests()){
      final SingularityRequest singularityRequest = singularityRequestWithState.getRequest();
      lock.runWithRequestLock(() -> syncUpstreamsForService(singularityRequest), singularityRequest.getId(), getClass().getSimpleName());
    }
  }
}
