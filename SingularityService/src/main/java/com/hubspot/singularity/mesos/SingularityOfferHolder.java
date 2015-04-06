package com.hubspot.singularity.mesos;

import java.util.List;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityOfferHolder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final Protos.Offer offer;
  private final List<SingularityTask> acceptedTasks;
  private List<Resource> currentResources;

  public SingularityOfferHolder(Protos.Offer offer, int taskSizeHint) {
    this.offer = offer;
    this.acceptedTasks = Lists.newArrayListWithCapacity(taskSizeHint);
    this.currentResources = offer.getResourcesList();
  }

  public void addMatchedTask(SingularityTask task) {
    acceptedTasks.add(task);

    // subtract task resources from offer
    currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getResourcesList());

    // subtract executor resources from offer, if any are defined
    if (task.getMesosTask().hasExecutor() && task.getMesosTask().getExecutor().getResourcesCount() > 0) {
      currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getExecutor().getResourcesList());
    }
  }

  public void launchTasks(SchedulerDriver driver) {
    final List<TaskInfo> toLaunch = Lists.newArrayListWithCapacity(acceptedTasks.size());
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(acceptedTasks.size());

    for (SingularityTask task : acceptedTasks) {
      taskIds.add(task.getTaskId());
      toLaunch.add(task.getMesosTask());
      LOG.trace("Launching {} mesos task: {}", task.getTaskId(), task.getMesosTask());
    }

    Status initialStatus = driver.launchTasks(ImmutableList.of(offer.getId()), toLaunch);

    LOG.info("{} tasks ({}) launched with status {}", taskIds.size(), taskIds, initialStatus);
  }

  public List<SingularityTask> getAcceptedTasks() {
    return acceptedTasks;
  }

  public List<Resource> getCurrentResources() {
    return currentResources;
  }

  public Protos.Offer getOffer() {
    return offer;
  }

}
