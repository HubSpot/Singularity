package com.hubspot.singularity.mesos;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityOfferHolder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final Protos.Offer offer;
  private final List<SingularityTask> acceptedTasks;
  private final Set<SingularityPendingTaskId> rejectedPendingTaskIds;
  private List<Resource> currentResources;

  private final String rackId;
  private final String sanitizedHost;
  private final String sanitizedRackId;

  private final Map<String, String> textAttributes;
  private final Map<String, String> reservedSlaveAttributes;

  SingularityOfferHolder(Protos.Offer offer, int taskSizeHint, String rackId, Map<String, String> textAttributes, Map<String, String> reservedSlaveAttributes) {
    this.rackId = rackId;
    this.offer = offer;
    this.acceptedTasks = Lists.newArrayListWithCapacity(taskSizeHint);
    this.currentResources = offer.getResourcesList();
    this.rejectedPendingTaskIds = new HashSet<>();
    this.sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(offer.getHostname());
    this.sanitizedRackId = JavaUtils.getReplaceHyphensWithUnderscores(rackId);
    this.textAttributes = textAttributes;
    this.reservedSlaveAttributes = reservedSlaveAttributes;
  }

  Map<String, String> getTextAttributes() {
    return textAttributes;
  }

  String getRackId() {
    return rackId;
  }

  boolean hasReservedSlaveAttributes() {
    return !reservedSlaveAttributes.isEmpty();
  }

  Map<String, String> getReservedSlaveAttributes() {
    return reservedSlaveAttributes;
  }

  String getSanitizedHost() {
    return sanitizedHost;
  }

  String getSanitizedRackId() {
    return sanitizedRackId;
  }

  void addRejectedTask(SingularityPendingTaskId pendingTaskId) {
    rejectedPendingTaskIds.add(pendingTaskId);
  }

  boolean hasRejectedPendingTaskAlready(SingularityPendingTaskId pendingTaskId) {
    return rejectedPendingTaskIds.contains(pendingTaskId);
  }

  void addMatchedTask(SingularityTask task) {
    acceptedTasks.add(task);

    // subtract task resources from offer
    currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getResourcesList());

    // subtract executor resources from offer, if any are defined
    if (task.getMesosTask().hasExecutor() && task.getMesosTask().getExecutor().getResourcesCount() > 0) {
      currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getExecutor().getResourcesList());
    }
  }

  public void launchTasks(SingularityDriver driver) {
    final List<TaskInfo> toLaunch = Lists.newArrayListWithCapacity(acceptedTasks.size());
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(acceptedTasks.size());

    for (SingularityTask task : acceptedTasks) {
      taskIds.add(task.getTaskId());
      toLaunch.add(task.getMesosTask());
      LOG.debug("Launching {} with offer {}", task.getTaskId(), offer.getId());
      LOG.trace("Launching {} mesos task: {}", task.getTaskId(), MesosUtils.formatForLogging(task.getMesosTask()));
    }

    driver.launchTasks(offer, toLaunch);
  }

  public List<SingularityTask> getAcceptedTasks() {
    return acceptedTasks;
  }

  List<Resource> getCurrentResources() {
    return currentResources;
  }

  public Protos.Offer getOffer() {
    return offer;
  }

}
