package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlavePlacement;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularityServiceResourceScheduler implements SingularityResourceScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityServiceResourceScheduler.class);

  // To be used once we deprecate the older settings
  private static final int defaultMaxTasks = 0;
  private static final SlavePlacement defaultPlacement = SlavePlacement.GREEDY;

  private final int maxTasksPerOffer;
  private final SlavePlacement defaultSlavePlacement;
  private final Resources defaultResources;
  private final Resources defaultCustomExecutorResources;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularitySlaveAndRackHelper slaveAndRackHelper;

  public SingularityServiceResourceScheduler(SingularityConfiguration configuration,
                                             SingularityMesosTaskBuilder mesosTaskBuilder,
                                             SingularitySlaveAndRackHelper slaveAndRackHelper) {
    this.maxTasksPerOffer = configuration.getResourceSchedulerConfiguration().getMaxTasksPerOffer().or(configuration.getMaxTasksPerOffer());
    this.defaultSlavePlacement = configuration.getResourceSchedulerConfiguration().getDefaultSlavePlacement().or(configuration.getDefaultSlavePlacement());
    this.defaultResources = new Resources(configuration.getMesosConfiguration().getDefaultCpus(), configuration.getMesosConfiguration().getDefaultMemory(), 0);
    this.defaultCustomExecutorResources = new Resources(configuration.getCustomExecutorConfiguration().getNumCpus(), configuration.getCustomExecutorConfiguration().getMemoryMb(), 0);
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.slaveAndRackHelper = slaveAndRackHelper;
  }

  @Override
  public List<SingularityOfferHolder> processOffers(SingularitySchedulerStateCache stateCache, List<SingularityTaskRequest> taskRequests, SchedulerDriver driver, List<Protos.Offer> offers) {
    for (SingularityTaskRequest taskRequest : taskRequests) {
      LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId());
    }

    int numDueTasks = taskRequests.size();

    final List<SingularityOfferHolder> offerHolders = Lists.newArrayListWithCapacity(offers.size());

    for (Protos.Offer offer : offers) {
      offerHolders.add(new SingularityOfferHolder(offer, numDueTasks));
    }

    boolean addedTaskInLastLoop = true;

    while (!taskRequests.isEmpty() && addedTaskInLastLoop) {
      addedTaskInLastLoop = false;
      Collections.shuffle(offerHolders);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (maxTasksPerOffer > 0 && offerHolder.getAcceptedTasks().size() >= maxTasksPerOffer) {
          LOG.trace("Offer {} is full ({}) - skipping", offerHolder.getOffer(), offerHolder.getAcceptedTasks().size());
          continue;
        }

        Optional<SingularityTask> accepted = match(taskRequests, stateCache, offerHolder);
        if (accepted.isPresent()) {
          offerHolder.addMatchedTask(accepted.get());
          addedTaskInLastLoop = true;
          taskRequests.remove(accepted.get().getTaskRequest());
        }

        if (taskRequests.isEmpty()) {
          break;
        }
      }
    }

    return offerHolders;
  }

  private Optional<SingularityTask> match(Collection<SingularityTaskRequest> taskRequests, SingularitySchedulerStateCache stateCache, SingularityOfferHolder offerHolder) {

    for (SingularityTaskRequest taskRequest : taskRequests) {
      final Resources taskResources = taskRequest.getDeploy().getResources().or(defaultResources);

      // only factor in executor resources if we're running a custom executor
      final Resources executorResources = taskRequest.getDeploy().getCustomExecutorCmd().isPresent() ? taskRequest.getDeploy().getCustomExecutorResources().or(defaultCustomExecutorResources) : Resources.EMPTY_RESOURCES;

      final Resources totalResources = Resources.add(taskResources, executorResources);

      LOG.trace("Attempting to match task {} resources {} ({} for task + {} for executor) with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(), totalResources, taskResources, executorResources, offerHolder.getCurrentResources());

      final boolean matchesResources = MesosUtils.doesOfferMatchResources(totalResources, offerHolder.getCurrentResources());
      final SlaveMatchState slaveMatchState = doesOfferMatch(offerHolder.getOffer(), taskRequest, stateCache);

      if (matchesResources && slaveMatchState.isMatchAllowed()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskResources, executorResources);
        LOG.trace("Accepted and built task {}", task);
        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());
        return Optional.of(task);
      } else {
        LOG.trace("Ignoring offer {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getOffer().getId(), offerHolder.getOffer().getHostname(), taskRequest
          .getPendingTask().getPendingTaskId(), matchesResources, slaveMatchState);
      }
    }

    return Optional.absent();
  }

  public SlaveMatchState doesOfferMatch(Protos.Offer offer, SingularityTaskRequest taskRequest, SingularitySchedulerStateCache stateCache) {
    final String host = offer.getHostname();
    final String rackId = slaveAndRackHelper.getRackIdOrDefault(offer);
    final String slaveId = offer.getSlaveId().getValue();

    if (stateCache.getSlave(slaveId).get().getCurrentState().getState().isDecommissioning()) {
      return SlaveMatchState.SLAVE_DECOMMISSIONING;
    }

    if (stateCache.getRack(rackId).get().getCurrentState().getState().isDecommissioning()) {
      return SlaveMatchState.RACK_DECOMMISSIONING;
    }

    if (!taskRequest.getRequest().getRackAffinity().or(Collections.<String> emptyList()).isEmpty()) {
      if (!taskRequest.getRequest().getRackAffinity().get().contains(rackId)) {
        LOG.trace("Task {} requires a rack in {} (current rack {})", taskRequest.getPendingTask().getPendingTaskId(), taskRequest.getRequest().getRackAffinity().get(), rackId);
        return SlaveMatchState.RACK_AFFINITY_NOT_MATCHING;
      }
    }

    final SlavePlacement slavePlacement = taskRequest.getRequest().getSlavePlacement().or(defaultSlavePlacement);

    if (!taskRequest.getRequest().isRackSensitive() && slavePlacement == SlavePlacement.GREEDY) {
      return SlaveMatchState.NOT_RACK_OR_SLAVE_PARTICULAR;
    }

    final int numDesiredInstances = taskRequest.getRequest().getInstancesSafe();
    double numOnRack = 0;
    double numOnSlave = 0;
    double numCleaningOnSlave = 0;
    double numOtherDeploysOnSlave = 0;

    final String sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(host);
    final String sanitizedRackId = JavaUtils.getReplaceHyphensWithUnderscores(rackId);
    Collection<SingularityTaskId> cleaningTasks = stateCache.getCleaningTasks();

    for (SingularityTaskId taskId : SingularityTaskId.matchingAndNotIn(stateCache.getActiveTaskIds(), taskRequest.getRequest().getId(), Collections.<SingularityTaskId>emptyList())) {
      // TODO consider using executorIds
      if (taskId.getSanitizedHost().equals(sanitizedHost)) {
        if (taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
          if (cleaningTasks.contains(taskId)) {
            numCleaningOnSlave++;
          } else {
            numOnSlave++;
          }
        } else {
          numOtherDeploysOnSlave++;
        }
      }
      if (taskId.getSanitizedRackId().equals(sanitizedRackId) && !cleaningTasks.contains(taskId) && taskRequest.getDeploy().getId().equals(taskId.getDeployId())) {
        numOnRack++;
      }
    }

    if (taskRequest.getRequest().isRackSensitive()) {
      final double numPerRack = numDesiredInstances / (double) stateCache.getNumActiveRacks();

      final boolean isRackOk = numOnRack < numPerRack;

      if (!isRackOk) {
        LOG.trace("Rejecting RackSensitive task {} from slave {} ({}) due to numOnRack {} and cleaningOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnRack, numCleaningOnSlave);
        return SlaveMatchState.RACK_SATURATED;
      }
    }

    switch (slavePlacement) {
      case SEPARATE:
      case SEPARATE_BY_DEPLOY:
        if (numOnSlave > 0 || numCleaningOnSlave > 0) {
          LOG.trace("Rejecting SEPARATE task {} from slave {} ({}) due to numOnSlave {} numCleaningOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave, numCleaningOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case SEPARATE_BY_REQUEST:
        if (numOnSlave > 0 || numCleaningOnSlave > 0 || numOtherDeploysOnSlave > 0) {
          LOG.trace("Rejecting SEPARATE task {} from slave {} ({}) due to numOnSlave {} numCleaningOnSlave {} numOtherDeploysOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave, numCleaningOnSlave, numOtherDeploysOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case OPTIMISTIC:
        final double numPerSlave = numDesiredInstances / (double) stateCache.getNumActiveSlaves();

        final boolean isSlaveOk = numOnSlave < numPerSlave;

        if (!isSlaveOk) {
          LOG.trace("Rejecting OPTIMISTIC task {} from slave {} ({}) due to numOnSlave {}", taskRequest.getRequest().getId(), slaveId, host, numOnSlave);
          return SlaveMatchState.SLAVE_SATURATED;
        }
        break;
      case GREEDY:
    }

    return SlaveMatchState.OK;
  }

  public enum SlaveMatchState {
    OK(true),
    NOT_RACK_OR_SLAVE_PARTICULAR(true),
    RACK_SATURATED(false),
    SLAVE_SATURATED(false),
    SLAVE_DECOMMISSIONING(false),
    RACK_DECOMMISSIONING(false),
    RACK_AFFINITY_NOT_MATCHING(false);

    private final boolean isMatchAllowed;

    private SlaveMatchState(boolean isMatchAllowed) {
      this.isMatchAllowed = isMatchAllowed;
    }

    public boolean isMatchAllowed() {
      return isMatchAllowed;
    }

  }

}
