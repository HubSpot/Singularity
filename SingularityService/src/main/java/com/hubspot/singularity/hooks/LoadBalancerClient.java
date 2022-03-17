package com.hubspot.singularity.hooks;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;

import com.google.common.collect.Lists;
import com.hubspot.mesos.protos.MesosParameter;
import com.hubspot.singularity.LoadBalancerRequestType.LoadBalancerRequestId;
import com.hubspot.singularity.LoadBalancerUpstream;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LoadBalancerClient {
  private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private final Optional<String> taskLabelForLoadBalancerUpstreamGroup;
  private final MesosProtosUtils mesosProtosUtils;

  public LoadBalancerClient(
    SingularityConfiguration configuration,
    MesosProtosUtils mesosProtosUtils
  ) {
    this.taskLabelForLoadBalancerUpstreamGroup =
      configuration.getTaskLabelForLoadBalancerUpstreamGroup();
    this.mesosProtosUtils = mesosProtosUtils;
  }

  public abstract boolean isEnabled();

  public SingularityLoadBalancerUpdate enqueue(
    LoadBalancerRequestId loadBalancerRequestId,
    SingularityRequest request,
    SingularityDeploy deploy,
    List<SingularityTask> add,
    List<SingularityTask> remove
  ) {
    final List<LoadBalancerUpstream> addUpstreams = getUpstreamsForTasks(
      add,
      loadBalancerRequestId.toString(),
      deploy.getLoadBalancerUpstreamGroup()
    );
    final List<LoadBalancerUpstream> removeUpstreams = getUpstreamsForTasks(
      remove,
      loadBalancerRequestId.toString(),
      deploy.getLoadBalancerUpstreamGroup()
    );

    return makeAndSendLoadBalancerRequest(
      loadBalancerRequestId,
      addUpstreams,
      removeUpstreams,
      deploy,
      request
    );
  }

  public abstract SingularityLoadBalancerUpdate getState(
    LoadBalancerRequestId loadBalancerRequestId
  );

  public abstract SingularityLoadBalancerUpdate cancel(
    LoadBalancerRequestId loadBalancerRequestId
  );

  public abstract SingularityLoadBalancerUpdate delete(
    LoadBalancerRequestId loadBalancerRequestId,
    String requestId,
    Set<String> loadBalancerGroups,
    String serviceBasePath
  );

  public abstract List<LoadBalancerUpstream> getUpstreamsForRequest(
    String singularityRequestId
  )
    throws IOException, InterruptedException, ExecutionException, TimeoutException;

  public List<LoadBalancerUpstream> getUpstreamsForTasks(
    List<SingularityTask> tasks,
    String requestId,
    Optional<String> loadBalancerUpstreamGroup
  ) {
    final List<LoadBalancerUpstream> upstreams = Lists.newArrayListWithCapacity(
      tasks.size()
    );

    for (SingularityTask task : tasks) {
      final Optional<Long> maybeLoadBalancerPort = MesosUtils.getPortByIndex(
        mesosProtosUtils.toResourceList(task.getMesosTask().getResources()),
        task.getTaskRequest().getDeploy().getLoadBalancerPortIndex().orElse(0)
      );

      if (maybeLoadBalancerPort.isPresent()) {
        String upstream = String.format(
          "%s:%d",
          task.getHostname(),
          maybeLoadBalancerPort.get()
        );
        Optional<String> group = loadBalancerUpstreamGroup;

        if (taskLabelForLoadBalancerUpstreamGroup.isPresent()) {
          for (MesosParameter label : task.getMesosTask().getLabels().getLabels()) {
            if (
              label.hasKey() &&
              label.getKey().equals(taskLabelForLoadBalancerUpstreamGroup.get()) &&
              label.hasValue()
            ) {
              group = Optional.of(label.getValue());
              break;
            }
          }
        }

        upstreams.add(
          new LoadBalancerUpstream(
            upstream,
            group.orElse("default"),
            task.getRackId(),
            Optional.of(task.getTaskId().getId())
          )
        );
      } else {
        LOG.warn(
          "Task {} is missing port but is being passed to LB  ({})",
          task.getTaskId(),
          task
        );
      }
    }

    return upstreams;
  }

  public abstract SingularityLoadBalancerUpdate makeAndSendLoadBalancerRequest(
    LoadBalancerRequestId loadBalancerRequestId,
    List<LoadBalancerUpstream> addUpstreams,
    List<LoadBalancerUpstream> removeUpstreams,
    SingularityDeploy deploy,
    SingularityRequest request
  );

  public void validateDeploy(SingularityDeploy deploy) {
    checkBadRequest(
      deploy.getServiceBasePath().isPresent(),
      "Deploy for loadBalanced request must include serviceBasePath"
    );
    checkBadRequest(
      deploy.getLoadBalancerGroups().isPresent() &&
      !deploy.getLoadBalancerGroups().get().isEmpty(),
      "Deploy for a loadBalanced request must include at least one load balancer group"
    );
  }
}
