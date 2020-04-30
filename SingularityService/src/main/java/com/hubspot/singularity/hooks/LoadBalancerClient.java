package com.hubspot.singularity.hooks;

import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.SingularityCheckingUpstreamsUpdate;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public interface LoadBalancerClient {
  SingularityLoadBalancerUpdate enqueue(
    LoadBalancerRequestId loadBalancerRequestId,
    SingularityRequest request,
    SingularityDeploy deploy,
    List<SingularityTask> add,
    List<SingularityTask> remove
  );

  SingularityLoadBalancerUpdate getState(LoadBalancerRequestId loadBalancerRequestId);

  SingularityLoadBalancerUpdate cancel(LoadBalancerRequestId loadBalancerRequestId);

  SingularityLoadBalancerUpdate delete(
    LoadBalancerRequestId loadBalancerRequestId,
    String requestId,
    Set<String> loadBalancerGroups,
    String serviceBasePath
  );

  SingularityCheckingUpstreamsUpdate getLoadBalancerServiceStateForRequest(
    String singularityRequestId
  )
    throws IOException, InterruptedException, ExecutionException, TimeoutException;

  List<UpstreamInfo> getUpstreamsForTasks(
    List<SingularityTask> tasks,
    String requestId,
    Optional<String> loadBalancerUpstreamGroup
  );

  SingularityLoadBalancerUpdate makeAndSendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    List<UpstreamInfo> addUpstreams,
    List<UpstreamInfo> removeUpstreams,
    SingularityDeploy deploy,
    SingularityRequest request
  );
}
