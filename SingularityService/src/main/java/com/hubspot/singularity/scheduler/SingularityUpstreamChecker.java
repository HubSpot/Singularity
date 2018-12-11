package com.hubspot.singularity.scheduler;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.mesos.protos.MesosParameter;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.hooks.LoadBalancerClient;
import com.hubspot.singularity.hooks.LoadBalancerClientImpl;

@Singleton
public class SingularityUpstreamChecker {

  private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerClient.class);

  private final LoadBalancerClientImpl lbClient;
  private final TaskManager taskManager;

  @Inject
  public SingularityUpstreamChecker(LoadBalancerClientImpl lbClient, TaskManager taskManager) {
    this.lbClient = lbClient;
    this.taskManager = taskManager;
  }

  private UpstreamInfo taskToUpstreamInfo (SingularityTask task, Optional<String> loadBalancerUpstreamGroup) {
    UpstreamInfo upstreamInfo = null; //TODO: what is a better way to do this? We cannot initialize an empty upstream
    final Optional<Long> maybeLoadBalancerPort = MesosUtils.getPortByIndex(lbClient.getMesosProtosUtils().toResourceList(task.getMesosTask().getResources()), task.getTaskRequest().getDeploy().getLoadBalancerPortIndex().or(0));
    if (maybeLoadBalancerPort.isPresent()) {
      final String host = task.getHostname();
      final long port = maybeLoadBalancerPort.get();
      String upstream = String.format("%s:%d", host, port);
      final Optional<String> requestId = Optional.of(task.getTaskRequest().getRequest().getId());
      Optional<String> group = loadBalancerUpstreamGroup;
      if (lbClient.getTaskLabelForLoadBalancerUpstreamGroup().isPresent()) {
        for (MesosParameter label : task.getMesosTask().getLabels().getLabels()) {
          if (label.hasKey() && label.getKey().equals(lbClient.getTaskLabelForLoadBalancerUpstreamGroup().get()) && label.hasValue()) {
            group = Optional.of(label.getValue());
            break;
          }
        }
      }
      upstreamInfo = new UpstreamInfo(upstream, requestId, task.getRackId(), Optional.<String>absent(), group);
    } else {
      LOG.warn("Task {} is missing port, not passed to upstreams({})", task.getTaskId(), task);
    }
    return upstreamInfo;
  }

  public Collection<UpstreamInfo> getUpstreamsFromActiveTasks() {
    final List<UpstreamInfo> upstreams = Lists.newArrayListWithCapacity(taskManager.getNumActiveTasks());
    for (SingularityTask task: taskManager.getActiveTasks()){
      final Optional<String> loadBalancerUpstreamGroup = task.getTaskRequest().getDeploy().getLoadBalancerUpstreamGroup();
      final UpstreamInfo upstream = taskToUpstreamInfo(task, loadBalancerUpstreamGroup);
      if (upstream.equals(null)){
        upstreams.add(upstream);
      }
    }
    return upstreams;
  }

  private void syncUpstreams( ) {
    Collection<UpstreamInfo> upstreamsInBaragon = lbClient.getUpstreams();
    Collection<UpstreamInfo> upstreamsInSingularity = getUpstreamsFromActiveTasks();
    upstreamsInBaragon.removeAll(upstreamsInSingularity);
    for (UpstreamInfo upstream: upstreamsInBaragon){
      //TODO: remove the upstream from Baragon
    }
  }
}
