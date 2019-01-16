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
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
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

  private List<SingularityTask> getActiveHealthyTasksForRequest(String requestId) throws Exception {
    final Optional<SingularityTaskIdsByStatus> taskIdsByStatusForRequest = requestHelper.getTaskIdsByStatusForRequest(requestId);
    if (taskIdsByStatusForRequest.isPresent()) {
      final List<SingularityTaskId> activeHealthyTaskIdsForRequest = taskIdsByStatusForRequest.get().getHealthy();
      final Map<SingularityTaskId, SingularityTask> activeTasksForRequest = taskManager.getTasks(activeHealthyTaskIdsForRequest);
      return new ArrayList<>(activeTasksForRequest.values());
    }
    LOG.error("TaskId not found for requestId: {}.", requestId);
    throw new Exception("TaskId not found.");
  }

  private Collection<UpstreamInfo> getUpstreamsFromActiveTasksForRequest(String singularityRequestId, Optional<String> loadBalancerUpstreamGroup) throws Exception {
    return lbClient.getUpstreamsForTasks(getActiveHealthyTasksForRequest(singularityRequestId), singularityRequestId, loadBalancerUpstreamGroup);
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

  private Collection<UpstreamInfo> getLoadBalancerUpstreamsForServiceHelper(SingularityCheckingUpstreamsUpdate singularityCheckingUpstreamsUpdate) {
    if (singularityCheckingUpstreamsUpdate.getBaragonServiceState().isPresent()){
      LOG.info("Baragon service state for service {} is present.", singularityCheckingUpstreamsUpdate.getSingularityRequestId());
      final BaragonServiceState baragonServiceState = singularityCheckingUpstreamsUpdate.getBaragonServiceState().get();
      return baragonServiceState.getUpstreams();
    }
    LOG.info("Baragon service state for service {} is absent.", singularityCheckingUpstreamsUpdate.getSingularityRequestId());
    return Collections.emptyList();
  }

  private Collection<UpstreamInfo> getLoadBalancerUpstreamsForService(String singularityRequestId) {
    LOG.info("Sending request to get load balancer upstreams for service {}.", singularityRequestId);
    try {
      final SingularityCheckingUpstreamsUpdate checkUpstreamsState = lbClient.getLoadBalancerServiceStateForRequest(singularityRequestId);
      LOG.info("Succeeded getting load balancer upstreams for singularity request {}. State is {}.", singularityRequestId, checkUpstreamsState.toString());
      return getLoadBalancerUpstreamsForServiceHelper(checkUpstreamsState);
    } catch (Exception e) {
      LOG.error("Failed getting load balancer upstreams for singularity request {}. ", singularityRequestId, e);
    }
    return Collections.emptyList();
  }

  private Optional<SingularityLoadBalancerUpdate> syncUpstreamsForServiceHelper(SingularityRequest singularityRequest, SingularityDeploy deploy, Optional<String> loadBalancerUpstreamGroup) {
    LOG.info("Sending load balancer request to sync upstreams for service {}.", singularityRequest.getId());
    final LoadBalancerRequestId loadBalancerRequestId = new LoadBalancerRequestId(String.format("%s-%s-%s", singularityRequest.getId(), deploy.getId(), System.currentTimeMillis()), LoadBalancerRequestType.REMOVE, Optional.absent());
    final long start = System.currentTimeMillis();
    try {
      Collection<UpstreamInfo> upstreamsInLoadBalancerForRequest = getLoadBalancerUpstreamsForService(singularityRequest.getId());
      LOG.info("Upstreams in load balancer for service {} are {}.", singularityRequest.getId(), upstreamsInLoadBalancerForRequest);
      Collection<UpstreamInfo> upstreamsInSingularityForRequest = getUpstreamsFromActiveTasksForRequest(singularityRequest.getId(), loadBalancerUpstreamGroup);
      LOG.info("Upstreams in singularity for service {} are {}.", singularityRequest.getId(), upstreamsInSingularityForRequest);
      final List<UpstreamInfo> extraUpstreams = getExtraUpstreamsInLoadBalancer(upstreamsInLoadBalancerForRequest, upstreamsInSingularityForRequest);
      if (extraUpstreams.size() > 0){
        LOG.info("Syncing upstreams for service {}. Making and sending load balancer request {} to remove {} extra upstreams. The upstreams removed are: {}.", singularityRequest.getId(), loadBalancerRequestId, extraUpstreams.size(), extraUpstreams);
        return Optional.of(lbClient.makeAndSendLoadBalancerRequest(loadBalancerRequestId, Collections.emptyList(), extraUpstreams, deploy, singularityRequest));
      }
      LOG.info("No extra upstreams for service {}. No load balancer request sent.", singularityRequest.getId());
      return Optional.absent();
    } catch (Exception e) {
      LOG.error("Could not sync for service {}. Load balancer request {} threw error: ", singularityRequest.getId(), loadBalancerRequestId, e);
      return Optional.of(new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, loadBalancerRequestId, Optional.of(String.format("Exception %s - %s", e.getClass().getSimpleName(), e.getMessage())), start, LoadBalancerMethod.CHECK_STATE, Optional.absent()));
    }
  }

  private boolean noPendingDeploy() {
    return deployManager.getPendingDeploys().size() == 0;
  }

  private void syncUpstreamsForService(SingularityRequest singularityRequest) {
    if (singularityRequest.isLoadBalanced() && noPendingDeploy()) {
      final String singularityRequestId = singularityRequest.getId();
      LOG.info("Starting syncing of upstreams for service: {}.", singularityRequestId);
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
    LOG.info("Checking load balancer request to sync upstreams for service {} using a retryer until the request state is no longer waiting.", singularityRequestId);
    Retryer<SingularityLoadBalancerUpdate> syncingRetryer = RetryerBuilder.<SingularityLoadBalancerUpdate>newBuilder()
        .retryIfException()
        .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
        .retryIfResult(IS_WAITING_STATE)
        .build();
    try {
      SingularityLoadBalancerUpdate syncUpstreamsState = syncingRetryer.call(() -> lbClient.getState(loadBalancerRequestId));
      if (syncUpstreamsState.getLoadBalancerState() == BaragonRequestState.SUCCESS){
        LOG.info("Syncing upstreams for singularity request {} is {}.", singularityRequestId, syncUpstreamsState.toString());
      } else {
        LOG.error("Syncing upstreams for singularity request {} is {}.", singularityRequestId, syncUpstreamsState.toString());
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
