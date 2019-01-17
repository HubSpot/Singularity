package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.rholder.retry.Retryer;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
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
import com.hubspot.singularity.SingularityCheckingUpstreamsUpdate;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;

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

  private List<SingularityTask> getActiveHealthyTasksForService(String requestId) throws TaskIdNotFoundException {
    final Optional<SingularityTaskIdsByStatus> taskIdsByStatusForRequest = requestHelper.getTaskIdsByStatusForRequest(requestId);
    if (taskIdsByStatusForRequest.isPresent()) {
      final List<SingularityTaskId> activeHealthyTaskIdsForRequest = taskIdsByStatusForRequest.get().getHealthy();
      final Map<SingularityTaskId, SingularityTask> activeTasksForRequest = taskManager.getTasks(activeHealthyTaskIdsForRequest);
      return new ArrayList<>(activeTasksForRequest.values());
    }
    throw new TaskIdNotFoundException("TaskId not found");
  }

  private Collection<UpstreamInfo> getUpstreamsFromActiveTasksForService(String singularityRequestId, Optional<String> loadBalancerUpstreamGroup) throws TaskIdNotFoundException {
    final List<SingularityTask> activeHealthyTasksForService = getActiveHealthyTasksForService(singularityRequestId);
    return lbClient.getUpstreamsForTasks(activeHealthyTasksForService, singularityRequestId, loadBalancerUpstreamGroup);
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
      LOG.trace("Baragon service state for service {} is present.", singularityCheckingUpstreamsUpdate.getSingularityRequestId());
      final BaragonServiceState baragonServiceState = singularityCheckingUpstreamsUpdate.getBaragonServiceState().get();
      return baragonServiceState.getUpstreams().stream().filter(upstream -> upstream.getGroup() == loadBalancerUpstreamGroup.or("default")).collect(Collectors.toList());
    }
    LOG.trace("Baragon service state for service {} is absent.", singularityCheckingUpstreamsUpdate.getSingularityRequestId());
    return Collections.emptyList();
  }

  private Collection<UpstreamInfo> getLoadBalancerUpstreamsForService(String singularityRequestId, Optional<String> loadBalancerUpstreamGroup) {
    try {
      LOG.info("Sending request to get load balancer upstreams for service {}.", singularityRequestId);
      final SingularityCheckingUpstreamsUpdate checkUpstreamsState = lbClient.getLoadBalancerServiceStateForRequest(singularityRequestId);
      LOG.trace("Succeeded getting load balancer upstreams for singularity request {}. State is {}.", singularityRequestId, checkUpstreamsState.toString());
      return getLoadBalancerUpstreamsForServiceHelper(checkUpstreamsState, loadBalancerUpstreamGroup);
    } catch (Exception e) {
      LOG.error("Failed getting load balancer upstreams for singularity request {}. ", singularityRequestId, e);
    }
    return Collections.emptyList();
  }

  private Optional<SingularityLoadBalancerUpdate> syncUpstreamsForServiceHelper(SingularityRequest singularityRequest, SingularityDeploy deploy, Optional<String> loadBalancerUpstreamGroup) {
    try {
      LOG.trace("Sending load balancer request to sync upstreams for service {}.", singularityRequest.getId());
      final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(String.format("%s-%s-%s", singularityRequest.getId(), deploy.getId(), System.currentTimeMillis()), LoadBalancerRequestType.REMOVE, Optional.absent());
      Collection<UpstreamInfo> upstreamsInLoadBalancerForService = getLoadBalancerUpstreamsForService(singularityRequest.getId(), loadBalancerUpstreamGroup);
      LOG.trace("Upstreams in load balancer for service {} are {}.", singularityRequest.getId(), upstreamsInLoadBalancerForService);
      Collection<UpstreamInfo> upstreamsInSingularityForService = getUpstreamsFromActiveTasksForService(singularityRequest.getId(), loadBalancerUpstreamGroup);
      LOG.trace("Upstreams in singularity for service {} are {}.", singularityRequest.getId(), upstreamsInSingularityForService);
      final List<UpstreamInfo> extraUpstreams = getExtraUpstreamsInLoadBalancer(upstreamsInLoadBalancerForService, upstreamsInSingularityForService);
      if (extraUpstreams.size() == 0) {
        LOG.trace("No extra upstreams for service {}. No load balancer request sent.", singularityRequest.getId());
        return Optional.absent();
      }
      LOG.info("Syncing upstreams for service {}. Making and sending load balancer request {} to remove {} extra upstreams. The upstreams removed are: {}.", singularityRequest.getId(), loadBalancerRequestId, extraUpstreams.size(), extraUpstreams);
      return Optional.of(lbClient.makeAndSendLoadBalancerRequest(loadBalancerRequestId, Collections.emptyList(), extraUpstreams, deploy, singularityRequest));
    } catch (TaskIdNotFoundException e) {
      LOG.error("TaskId not found for requestId: {}.", singularityRequest.getId());
      return Optional.absent();
    }
  }

  private boolean noPendingDeploy() {
    return deployManager.getPendingDeploys().size() == 0;
  }

  private void syncUpstreamsForService(SingularityRequest singularityRequest) {
    if (singularityRequest.isLoadBalanced() && noPendingDeploy()) {
      final String singularityRequestId = singularityRequest.getId();
      LOG.trace("Starting syncing of upstreams for service: {}.", singularityRequestId);
      final Optional<String> maybeDeployId = deployManager.getInUseDeployId(singularityRequestId);
      if (maybeDeployId.isPresent()) {
        final String deployId = maybeDeployId.get();
        final Optional<SingularityDeploy> maybeDeploy = deployManager.getDeploy(singularityRequestId, deployId);
        if (maybeDeploy.isPresent()) {
          final Optional<SingularityLoadBalancerUpdate> maybeSyncUpstreamsUpdate = syncUpstreamsForServiceHelper(singularityRequest, maybeDeploy.get(), maybeDeploy.get().getLoadBalancerUpstreamGroup());
          if (maybeSyncUpstreamsUpdate.isPresent()) {
            checkSyncUpstreamsState(maybeSyncUpstreamsUpdate.get().getLoadBalancerRequestId(), singularityRequestId);
          }
        }
      }
    }
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
        LOG.trace("Syncing upstreams for singularity request {} is {}.", singularityRequestId, syncUpstreamsState);
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
