package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.Offer.Operation;
import org.apache.mesos.v1.Protos.Offer.Operation.Launch;
import org.apache.mesos.v1.Protos.Offer.Operation.Type;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class SingularityOfferHolder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final List<Protos.Offer> offers;
  private final List<SingularityTask> acceptedTasks;
  private final Set<SingularityPendingTaskId> rejectedPendingTaskIds;
  private List<Resource> currentResources;
  private Set<String> roles;

  private final String rackId;
  private final String slaveId;
  private final String hostname;
  private final String sanitizedHost;
  private final String sanitizedRackId;

  private final Map<String, String> textAttributes;
  private final Map<String, String> reservedSlaveAttributes;

  public SingularityOfferHolder(List<Protos.Offer> offers, int taskSizeHint, String rackId, String slaveId, String hostname, Map<String, String> textAttributes, Map<String, String> reservedSlaveAttributes) {
    this.rackId = rackId;
    this.slaveId = slaveId;
    this.hostname = hostname;
    this.offers = offers;
    this.roles = MesosUtils.getRoles(offers.get(0));
    this.acceptedTasks = Lists.newArrayListWithExpectedSize(taskSizeHint);
    this.currentResources = offers.size()  > 1 ? MesosUtils.combineResources(offers.stream().map(Protos.Offer::getResourcesList).collect(Collectors.toList())) : offers.get(0).getResourcesList();
    this.rejectedPendingTaskIds = new HashSet<>();
    this.sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(hostname);
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

  public String getSlaveId() {
    return slaveId;
  }

  public boolean hasReservedSlaveAttributes() {
    return !reservedSlaveAttributes.isEmpty();
  }

  Map<String, String> getReservedSlaveAttributes() {
    return reservedSlaveAttributes;
  }

  public String getHostname() {
    return hostname;
  }

  public String getSanitizedHost() {
    return sanitizedHost;
  }

  String getSanitizedRackId() {
    return sanitizedRackId;
  }

  public Set<String> getRoles() {
    return roles;
  }

  public void addRejectedTask(SingularityPendingTaskId pendingTaskId) {
    rejectedPendingTaskIds.add(pendingTaskId);
  }

  boolean hasRejectedPendingTaskAlready(SingularityPendingTaskId pendingTaskId) {
    return rejectedPendingTaskIds.contains(pendingTaskId);
  }

  public void addMatchedTask(SingularityTask task) {
    LOG.trace("Accepting task {} for offers {}", task.getTaskId(), offers.stream().map(Offer::getId).collect(Collectors.toList()));
    acceptedTasks.add(task);

    // subtract task resources from offer
    currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getResources());

    // subtract executor resources from offer, if any are defined
    if (task.getMesosTask().hasExecutor() && task.getMesosTask().getExecutor().getResourcesCount() > 0) {
      currentResources = MesosUtils.subtractResources(currentResources, task.getMesosTask().getExecutor().getResourcesList());
    }
  }

  public List<Offer> launchTasksAndGetUnusedOffers(SingularityMesosSchedulerClient schedulerClient) {
    final List<TaskInfo> toLaunch = Lists.newArrayListWithCapacity(acceptedTasks.size());
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(acceptedTasks.size());

    for (SingularityTask task : acceptedTasks) {
      taskIds.add(task.getTaskId());
      toLaunch.add(task.getMesosTaskProtos());
      LOG.debug("Launching {} with offer {}", task.getTaskId(), offers.get(0).getId());
      LOG.trace("Launching {} mesos task: {}", task.getTaskId(), MesosUtils.formatForLogging(task.getMesosTask()));
    }

    // At this point, `currentResources` contains a list of unused resources, because we subtracted out the required resources of every task we accepted.
    // Let's try and reclaim offers by trying to pull each offer's list of resources out of the combined pool of leftover resources.
    // n.b., This is currently not optimal. We just look through the offers in this instance and try to reclaim them with no particular priority or order.
    Map<Boolean, List<Offer>> partitionedOffers = offers.stream().collect(Collectors.partitioningBy(offer -> {
      List<Long> ports = MesosUtils.getAllPorts(offer.getResourcesList());
      boolean offerCanBeReclaimedFromUnusedResources = offer.getResourcesList().stream()
          // When matching resource requirements with resource offers, we need to take roles into account.
          // Therefore, before we can check if this offer can be reclaimed from the pool of Resources in this SingularityOfferHolder,
          // we have to group the offer's Resources by role first.
          .collect(Collectors.groupingBy(Resource::getRole))
          .entrySet().stream()
          .map((entry) -> {
            // Now, for each set of offer Resources grouped by role...
            String role = entry.getKey();
            List<Resource> offerResources = entry.getValue();
            Optional<String> maybeRole = (!role.equals("") && !role.equals("*")) ? Optional.of(role) : Optional.absent();
            // ...Check if we can pull the Resources belonging to this offer out of the pool of `currentResources`.
            return MesosUtils.doesOfferMatchResources(
                maybeRole,
                MesosUtils.buildResourcesFromMesosResourceList(offerResources, maybeRole),
                currentResources,
                ports
            );
          }).reduce(true, (x, y) -> x && y);
      //      ^ the `reduce()` call determines whether we can pull *every* role-group of Resources belonging to this offer
      //        out of the combined `currentResources` pool.

      if (offerCanBeReclaimedFromUnusedResources) {
        // We can reclaim this offer in its entirety! Pull all of its resources out of the combined pool for this SingularityOfferHolder instance.
        LOG.trace(
            "Able to reclaim offer {} from unused resources in OfferHolder from host {}. cpu: {}, mem: {}, disk: {}",
            offer.getId().getValue(), offer.getHostname(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer), MesosUtils.getDisk(offer)
        );
        currentResources = MesosUtils.subtractResources(currentResources, offer.getResourcesList());
      }

      return offerCanBeReclaimedFromUnusedResources;
    }));

    List<Offer> leftoverOffers = partitionedOffers.get(true);
    List<Offer> neededOffers = partitionedOffers.get(false);

    schedulerClient.accept(
        neededOffers.stream().map(Offer::getId).collect(Collectors.toList()),
        Collections.singletonList(Operation.newBuilder().setType(Type.LAUNCH).setLaunch(Launch.newBuilder().addAllTaskInfos(toLaunch).build()).build())
    );

    LOG.info("{} tasks ({}) launched", taskIds.size(), taskIds);
    return leftoverOffers;
  }

  public List<SingularityTask> getAcceptedTasks() {
    return acceptedTasks;
  }

  List<Resource> getCurrentResources() {
    return currentResources;
  }

  public List<Protos.Offer> getOffers() {
    return offers;
  }

  @Override
  public String toString() {
    return "SingularityOfferHolder{" +
        "offers=" + offers +
        ", acceptedTasks=" + acceptedTasks +
        ", rejectedPendingTaskIds=" + rejectedPendingTaskIds +
        ", currentResources=" + currentResources +
        ", roles=" + roles +
        ", rackId='" + rackId + '\'' +
        ", slaveId='" + slaveId + '\'' +
        ", hostname='" + hostname + '\'' +
        ", sanitizedHost='" + sanitizedHost + '\'' +
        ", sanitizedRackId='" + sanitizedRackId + '\'' +
        ", textAttributes=" + textAttributes +
        ", reservedSlaveAttributes=" + reservedSlaveAttributes +
        '}';
  }
}
