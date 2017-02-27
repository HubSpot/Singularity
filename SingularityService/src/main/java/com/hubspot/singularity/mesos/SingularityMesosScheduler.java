package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskRequestWithPriority;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

@Singleton
public class SingularityMesosScheduler implements Scheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final Resources defaultResources;
  private final Resources defaultCustomExecutorResources;
  private final TaskManager taskManager;
  private final PriorityManager priorityManager;
  private final SingularityScheduler scheduler;
  private final SingularityConfiguration configuration;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularityMesosFrameworkMessageHandler messageHandler;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularityTaskSizeOptimizer taskSizeOptimizer;


  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  private final SchedulerDriverSupplier schedulerDriverSupplier;

  private final SingularityMesosStatusUpdateHandler statusUpdateHandler;

  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, CustomExecutorConfiguration customExecutorConfiguration, TaskManager taskManager, PriorityManager priorityManager,
      SingularityScheduler scheduler, SingularityConfiguration configuration, SingularityMesosTaskBuilder mesosTaskBuilder,
      SingularityMesosFrameworkMessageHandler messageHandler, SingularitySlaveAndRackManager slaveAndRackManager, SingularityTaskSizeOptimizer taskSizeOptimizer,
      Provider<SingularitySchedulerStateCache> stateCacheProvider, SchedulerDriverSupplier schedulerDriverSupplier, SingularityMesosStatusUpdateHandler statusUpdateHandler) {
    this.defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0, mesosConfiguration.getDefaultDisk());
    this.defaultCustomExecutorResources = new Resources(customExecutorConfiguration.getNumCpus(), customExecutorConfiguration.getMemoryMb(), 0, customExecutorConfiguration.getDiskMb());
    this.taskManager = taskManager;
    this.priorityManager = priorityManager;
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.messageHandler = messageHandler;
    this.slaveAndRackManager = slaveAndRackManager;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.stateCacheProvider = stateCacheProvider;
    this.schedulerDriverSupplier = schedulerDriverSupplier;

    this.statusUpdateHandler = statusUpdateHandler;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info("Registered driver {}, with frameworkId {} and master {}", driver, frameworkId, masterInfo);
    schedulerDriverSupplier.setSchedulerDriver(driver);
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info("Reregistered driver {}, with master {}", driver, masterInfo);
    schedulerDriverSupplier.setSchedulerDriver(driver);
  }

  private void removeTasksAffectedByPriorityFreeze(List<SingularityTaskRequest> taskRequests) {
    final Optional<SingularityPriorityFreezeParent> maybePriorityFreeze = priorityManager.getActivePriorityFreeze();

    if (maybePriorityFreeze.isPresent()) {
      final ListIterator<SingularityTaskRequest> iterator = taskRequests.listIterator();

      while (iterator.hasNext()) {
        final SingularityTaskRequest taskRequest = iterator.next();

        final double taskPriorityLevel = priorityManager.getTaskPriorityLevelForRequest(taskRequest.getRequest());

        if (taskPriorityLevel < maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel()) {
          LOG.trace("Skipping scheduled task {} because taskPriorityLevel ({}) is less than active priority freeze ({})", taskRequest.getPendingTask().getPendingTaskId(), taskPriorityLevel, maybePriorityFreeze.get().getPriorityFreeze().getMinimumPriorityLevel());
          iterator.remove();
        }
      }
    }
  }

  @Override
  @Timed
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info("Received {} offer(s)", offers.size());

    for (Offer offer : offers) {
      String rolesInfo = MesosUtils.getRoles(offer).toString();
      LOG.debug("Received offer ID {} with roles {} from {} ({}) for {} cpu(s), {} memory, {} ports, and {} disk", offer.getId().getValue(), rolesInfo, offer.getHostname(), offer.getSlaveId().getValue(), MesosUtils.getNumCpus(offer), MesosUtils.getMemory(offer),
          MesosUtils.getNumPorts(offer), MesosUtils.getDisk(offer));
    }

    final long start = System.currentTimeMillis();

    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();

    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);

    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());

    for (Protos.Offer offer : offers) {
      slaveAndRackManager.checkOffer(offer);
    }

    int numDueTasks = 0;

    try {
      final List<SingularityTaskRequest> taskRequests = getSortedDueTasks(scheduler.getDueTasks());

      for (SingularityTaskRequest taskRequest : taskRequests) {
        LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId());
      }

      removeTasksAffectedByPriorityFreeze(taskRequests);

      numDueTasks = taskRequests.size();

      final List<SingularityOfferHolder> offerHolders = Lists.newArrayListWithCapacity(offers.size());
      final Map<String, Map<String, Integer>> tasksPerOfferPerRequest = new HashMap<>();
      for (Protos.Offer offer : offers) {
        offerHolders.add(new SingularityOfferHolder(offer, numDueTasks));
      }

      boolean addedTaskInLastLoop = true;

      while (!taskRequests.isEmpty() && addedTaskInLastLoop) {
        addedTaskInLastLoop = false;
        Collections.shuffle(offerHolders);

        for (SingularityOfferHolder offerHolder : offerHolders) {
          if (configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()) {
            LOG.trace("Offer {} is full ({}) - skipping", offerHolder.getOffer(), offerHolder.getAcceptedTasks().size());
            continue;
          }

          Optional<SingularityTask> accepted = match(taskRequests, stateCache, offerHolder, tasksPerOfferPerRequest);
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

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (!offerHolder.getAcceptedTasks().isEmpty()) {
          offerHolder.launchTasks(driver);

          acceptedOffers.add(offerHolder.getOffer().getId());
        } else {
          driver.declineOffer(offerHolder.getOffer().getId());
        }
      }
    } catch (Throwable t) {
      LOG.error("Received fatal error while accepting offers - will decline all available offers", t);

      for (Protos.Offer offer : offers) {
        if (acceptedOffers.contains(offer.getId())) {
          continue;
        }

        driver.declineOffer(offer.getId());
      }

      throw t;
    }

    LOG.info("Finished handling {} offer(s) ({}), {} accepted, {} declined, {} outstanding tasks", offers.size(), JavaUtils.duration(start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size(), numDueTasks - acceptedOffers.size());
  }

  public List<SingularityTaskRequest> getSortedDueTasks(List<SingularityTaskRequest> dueTasks) {
    long now = System.currentTimeMillis();
    List<SingularityTaskRequestWithPriority> taskRequestWithPriorities = new ArrayList<>();
    for (SingularityTaskRequest taskRequest : dueTasks) {
      taskRequestWithPriorities.add(new SingularityTaskRequestWithPriority(taskRequest, getWeightedPriority(taskRequest, now)));
    }
    Collections.sort(taskRequestWithPriorities, SingularityTaskRequestWithPriority.weightedPriorityComparator());
    List<SingularityTaskRequest> taskRequests = new ArrayList<>();
    for (SingularityTaskRequestWithPriority taskRequestWithPriority : taskRequestWithPriorities) {
      taskRequests.add(taskRequestWithPriority.getTaskRequest());
    }
    return taskRequests;
  }

  private double getWeightedPriority(SingularityTaskRequest taskRequest, long now) {
    Long overdueMillis = Math.max(now - taskRequest.getPendingTask().getPendingTaskId().getNextRunAt(), 1);
    Double requestPriority = priorityManager.getTaskPriorityLevelForRequest(taskRequest.getRequest());
    return overdueMillis * Math.pow(requestPriority, configuration.getSchedulerPriorityWeightFactor());
  }

  private Optional<SingularityTask> match(Collection<SingularityTaskRequest> taskRequests, SingularitySchedulerStateCache stateCache, SingularityOfferHolder offerHolder, Map<String, Map<String, Integer>> tasksPerOfferPerRequest) {
    String offerId = offerHolder.getOffer().getId().getValue();
    for (SingularityTaskRequest taskRequest : taskRequests) {
      if (tooManyTasksPerOfferForRequest(tasksPerOfferPerRequest, offerId, taskRequest)) {
        LOG.debug("Skipping task request for request id {}, too many tasks already scheduled using offer {}", taskRequest.getRequest().getId(), offerId);
        continue;
      }

      final Resources taskResources = taskRequest.getPendingTask().getResources().or(taskRequest.getDeploy().getResources()).or(defaultResources);

      // only factor in executor resources if we're running a custom executor
      final Resources executorResources = taskRequest.getDeploy().getCustomExecutorCmd().isPresent() ? taskRequest.getDeploy().getCustomExecutorResources().or(defaultCustomExecutorResources) : Resources.EMPTY_RESOURCES;

      final Resources totalResources = Resources.add(taskResources, executorResources);

      final List<Long> requestedPorts = new ArrayList<>();

      if (taskRequest.getDeploy().getContainerInfo().isPresent() && taskRequest.getDeploy().getContainerInfo().get().getDocker().isPresent()) {
        requestedPorts.addAll(taskRequest.getDeploy().getContainerInfo().get().getDocker().get().getLiteralHostPorts());
      }

      Optional<String> requiredRole = taskRequest.getRequest().getRequiredRole();
      LOG.debug("Attempting to match task {} against offer {}", taskRequest.getPendingTask().getPendingTaskId(), offerHolder.getOffer().getId());
      LOG.trace("Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(), totalResources, requiredRole.or("*"),taskResources, executorResources, offerHolder.getCurrentResources());

      final boolean matchesResources = MesosUtils.doesOfferMatchResources(requiredRole, totalResources, offerHolder.getCurrentResources(), requestedPorts);
      final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder.getOffer(), taskRequest, stateCache);

      if (matchesResources && slaveMatchState.isMatchAllowed()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offerHolder.getOffer(), offerHolder.getCurrentResources(), taskRequest, taskResources, executorResources);

        final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(task);

        LOG.debug("Built task {}", zkTask.getTaskId());
        LOG.trace("Accepted and built task {}", zkTask);

        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offerHolder.getOffer().getSlaveId().getValue(), offerHolder.getOffer().getHostname());

        taskManager.createTaskAndDeletePendingTask(zkTask);

        stateCache.getActiveTaskIds().add(task.getTaskId());
        addRequestToMapByOfferId(tasksPerOfferPerRequest, offerId, taskRequest.getRequest().getId());
        stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());

        return Optional.of(task);
      } else {
        String rolesInfo = MesosUtils.getRoles(offerHolder.getOffer()).toString();
        LOG.trace("Ignoring offer {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getOffer().getId(), rolesInfo, offerHolder.getOffer().getHostname(), taskRequest
                .getPendingTask().getPendingTaskId(), matchesResources, slaveMatchState);
      }
    }

    return Optional.absent();
  }

  private void addRequestToMapByOfferId(Map<String, Map<String, Integer>> tasksPerOfferPerRequest, String offerId, String requestId) {
    if (tasksPerOfferPerRequest.containsKey(offerId)) {
      if (tasksPerOfferPerRequest.get(offerId).containsKey(requestId)) {
        int count = tasksPerOfferPerRequest.get(offerId).get(requestId);
        tasksPerOfferPerRequest.get(offerId).put(requestId, count + 1);
      } else {
        tasksPerOfferPerRequest.get(offerId).put(requestId, 0);
      }
    } else {
      tasksPerOfferPerRequest.put(offerId, new HashMap<String, Integer>());
      tasksPerOfferPerRequest.get(offerId).put(requestId, 1);
    }
  }

  private boolean tooManyTasksPerOfferForRequest(Map<String, Map<String, Integer>> tasksPerOfferPerRequest, String offerId, SingularityTaskRequest taskRequest) {
    if (!tasksPerOfferPerRequest.containsKey(offerId)) {
      return false;
    }
    if (!tasksPerOfferPerRequest.get(offerId).containsKey(taskRequest.getRequest().getId())) {
      return false;
    }

    int maxPerOfferPerRequest = taskRequest.getRequest().getMaxTasksPerOffer().or(configuration.getMaxTasksPerOfferPerRequest());
    if (!(maxPerOfferPerRequest > 0)) {
      return false;
    }
    return tasksPerOfferPerRequest.get(offerId).get(taskRequest.getRequest().getId()) > maxPerOfferPerRequest;
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    statusUpdateHandler.enqueueStatusUpdate(status);
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    LOG.info("Framework message from executor {} on slave {} with {} bytes of data", executorId, slaveId, data.length);

    messageHandler.handleMessage(executorId, slaveId, data);
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    schedulerDriverSupplier.setSchedulerDriver(null);
    LOG.warn("Scheduler/Driver disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    LOG.warn("Lost a slave {}", slaveId);

    slaveAndRackManager.slaveLost(slaveId);
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    LOG.warn("Lost an executor {} on slave {} with status {}", executorId, slaveId, status);
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    LOG.warn("Error from mesos: {}", message);
  }

  public boolean isConnected() {
    return schedulerDriverSupplier.get().isPresent();
  }

}
