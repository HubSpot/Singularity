package com.hubspot.singularity.hooks;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.hubspot.baragon.client.BaragonClientException;
import com.hubspot.baragon.client.BaragonServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityLoadBalancerUpdate.LoadBalancerMethod;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;

public class LoadBalancerClientImpl implements LoadBalancerClient {
  private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private final Optional<BaragonServiceClient> baragonServiceClient;

  @Inject
  public LoadBalancerClientImpl(Optional<BaragonServiceClient> baragonServiceClient) {
    this.baragonServiceClient = baragonServiceClient;
  }

  @Override
  public SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId) {
    return process(loadBalancerRequestId, LoadBalancerMethod.CHECK_STATE, BaragonRequestState.UNKNOWN);
  }

  @Override
  public SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId) {
    return process(loadBalancerRequestId, LoadBalancerMethod.CANCEL, BaragonRequestState.UNKNOWN);
  }

  @Override
  public SingularityLoadBalancerUpdate enqueue(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove) {
    BaragonRequest baragonRequest = buildEnqueueRequest(loadBalancerRequestId, request, deploy, add, remove);
    LOG.trace("Deploy {} is preparing to send {}", deploy.getId(), baragonRequest);
    return process(loadBalancerRequestId, LoadBalancerMethod.ENQUEUE, BaragonRequestState.FAILED, Optional.of(baragonRequest));
  }

  private BaragonRequest buildEnqueueRequest(LoadBalancerRequestId loadBalancerRequestId, SingularityRequest request, SingularityDeploy deploy, List<SingularityTask> add, List<SingularityTask> remove) {
    final List<String> serviceOwners = request.getOwners().or(Collections.<String>emptyList());
    final Set<String> loadBalancerGroups = deploy.getLoadBalancerGroups().or(Collections.<String> emptySet());
    final BaragonService lbService = new BaragonService(request.getId(), serviceOwners, deploy.getServiceBasePath().get(), loadBalancerGroups, deploy.getLoadBalancerOptions().orNull());

    final List<UpstreamInfo> addUpstreams = tasksToUpstreams(add, loadBalancerRequestId.toString());
    final List<UpstreamInfo> removeUpstreams = tasksToUpstreams(remove, loadBalancerRequestId.toString());

    return new BaragonRequest(loadBalancerRequestId.toString(), lbService, addUpstreams, removeUpstreams, Optional.<String>absent());
  }

  private List<UpstreamInfo> tasksToUpstreams(List<SingularityTask> tasks, String requestId) {
    final List<UpstreamInfo> upstreams = Lists.newArrayListWithCapacity(tasks.size());

    for (SingularityTask task : tasks) {
      final Optional<Long> maybeFirstPort = task.getFirstPort();
      if (maybeFirstPort.isPresent()) {
        String upstream = String.format("%s:%d", task.getOffer().getHostname(), maybeFirstPort.get());
        String rackId = task.getTaskId().getRackId();
        upstreams.add(new UpstreamInfo(upstream, Optional.of(requestId), Optional.fromNullable(rackId)));
      } else {
        LOG.warn("Task {} is missing port but is being passed to LB  ({})", task.getTaskId(), task);
      }
    }

    return upstreams;
  }

  private SingularityLoadBalancerUpdate process(LoadBalancerRequestId loadBalancerRequestId, LoadBalancerMethod method, BaragonRequestState onFailure, Optional<BaragonRequest> request) {
    final long start = System.currentTimeMillis();
    try {
      Optional<BaragonResponse> response;
      switch (method) {
        case ENQUEUE:
          response = baragonServiceClient.get().enqueueRequest(request.get());
          break;
        case CANCEL:
          response = baragonServiceClient.get().cancelRequest(loadBalancerRequestId.toString());
          break;
        case CHECK_STATE:
        default:
          response = baragonServiceClient.get().getRequest(loadBalancerRequestId.toString());
          break;
      }
      if (response.isPresent()) {
        return new SingularityLoadBalancerUpdate(response.get().getLoadBalancerState(), loadBalancerRequestId, response.get().getMessage(), start, method, Optional.<String>absent());
      } else {
        return new SingularityLoadBalancerUpdate(BaragonRequestState.UNKNOWN, loadBalancerRequestId, Optional.of("No response or error from Baragon"), start, method, Optional.<String>absent());
      }
    } catch (BaragonClientException e) {
      return new SingularityLoadBalancerUpdate(onFailure, loadBalancerRequestId, Optional.of(e.getMessage()), start, method, Optional.<String>absent());
    }
  }

  private SingularityLoadBalancerUpdate process(LoadBalancerRequestId requestId, LoadBalancerMethod method, BaragonRequestState onFailure) {
    return process(requestId, method, onFailure, Optional.<BaragonRequest>absent());
  }
}
