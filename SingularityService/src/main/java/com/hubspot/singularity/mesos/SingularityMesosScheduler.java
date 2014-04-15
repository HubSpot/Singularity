package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.mesos.SingularityRackManager.RackCheckState;
import com.hubspot.singularity.scheduler.SingularityHealthchecker;
import com.hubspot.singularity.scheduler.SingularityNewTaskChecker;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;

public class SingularityMesosScheduler implements Scheduler {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final Resources DEFAULT_RESOURCES;
  private final TaskManager taskManager;
  private final DeployManager deployManager;
  private final SingularityScheduler scheduler;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularityHealthchecker healthchecker;
  private final SingularityNewTaskChecker newTaskChecker;
  private final SingularityRackManager rackManager;
  private final SingularityLogSupport logSupport;

  private final Provider<SingularitySchedulerStateCache> stateCacheProvider;
  
  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, TaskManager taskManager, SingularityScheduler scheduler, SingularityRackManager rackManager, SingularityNewTaskChecker newTaskChecker,
      SingularityMesosTaskBuilder mesosTaskBuilder, SingularityLogSupport logSupport, Provider<SingularitySchedulerStateCache> stateCacheProvider, SingularityHealthchecker healthchecker, DeployManager deployManager) {
    DEFAULT_RESOURCES = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0);
    this.taskManager = taskManager;
    this.deployManager = deployManager;
    this.newTaskChecker = newTaskChecker;
    this.rackManager = rackManager;
    this.scheduler = scheduler;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.logSupport = logSupport;
    this.stateCacheProvider = stateCacheProvider;
    this.healthchecker = healthchecker;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info("Registered driver {}, with frameworkId {} and master {}", driver, frameworkId, masterInfo);
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info("Reregistered driver {}, with master {}", driver, masterInfo);
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info("Received {} offer(s)", offers.size());

    final long start = System.currentTimeMillis();
    
    final SingularitySchedulerStateCache stateCache = stateCacheProvider.get();
    
    scheduler.checkForDecomissions(stateCache);
    scheduler.drainPendingQueue(stateCache);
    
    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());

    for (Protos.Offer offer : offers) {
      rackManager.checkOffer(offer);
    }

    int numTasksSeen = 0;

    try {
      final List<SingularityTaskRequest> tasks = scheduler.getDueTasks();
      
      for (SingularityTaskRequest taskRequest : tasks) {
        LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId());
      }
      
      numTasksSeen = tasks.size();

      for (Protos.Offer offer : offers) {
        LOG.trace("Evaluating offer {}", offer);

        Optional<SingularityTask> accepted = acceptOffer(driver, offer, tasks, stateCache);

        if (!accepted.isPresent()) {
          driver.declineOffer(offer.getId());
        } else {
          acceptedOffers.add(offer.getId());
          tasks.remove(accepted.get().getTaskRequest());
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

    LOG.info("Finished handling offers ({}), accepted {}, declined {}, outstanding tasks {}", JavaUtils.duration(start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size(), numTasksSeen - acceptedOffers.size());
  }

  private Optional<SingularityTask> acceptOffer(SchedulerDriver driver, Protos.Offer offer, List<SingularityTaskRequest> tasks, SingularitySchedulerStateCache stateCache) {
    for (SingularityTaskRequest taskRequest : tasks) {
      Resources taskResources = DEFAULT_RESOURCES;

      if (taskRequest.getDeploy().getResources().isPresent()) {
        taskResources = taskRequest.getDeploy().getResources().get();
      }

      LOG.trace("Attempting to match resources {} with offer resources {}", taskResources, offer.getResourcesList());
          
      final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskResources, offer);
      final RackCheckState rackCheckState = rackManager.checkRack(offer, taskRequest, stateCache);
            
      if (matchesResources && rackCheckState.isRackAppropriate()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offer, taskRequest, taskResources);

        LOG.trace("Accepted and built task {}", task);
        
        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offer.getSlaveId(), offer.getHostname());

        taskManager.launchTask(task);

        LOG.debug("Launching mesos task: {}", task.getMesosTask());

        Status initialStatus = driver.launchTasks(ImmutableList.of(offer.getId()), ImmutableList.of(task.getMesosTask()));

        LOG.trace("Task {} launched with status {}", task.getTaskId(), initialStatus.name());
        
        return Optional.of(task);
      } else {
        LOG.trace("Turning down offer {} for task {}; matched resources: {}, rack state: {}", offer.getId(), taskRequest.getPendingTask().getPendingTaskId(), matchesResources, rackCheckState);
      }
    }

    return Optional.absent();
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {    
    final String taskId = status.getTaskId().getValue();
    LOG.debug("Got a status update for task: {}, status - {}", taskId, status);
    
    Optional<SingularityTask> maybeActiveTask = taskManager.getActiveTask(taskId);
    
    if (maybeActiveTask.isPresent() && status.getState() == TaskState.TASK_RUNNING) {
      healthchecker.enqueueHealthcheck(maybeActiveTask.get());
    }

    final long now = System.currentTimeMillis();
    
    final SingularityTaskId taskIdObj = SingularityTaskId.fromString(taskId);
    final ExtendedTaskState taskState = ExtendedTaskState.fromTaskState(status.getState());
    
    taskManager.saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(taskIdObj, now, taskState, status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent()));

    logSupport.checkDirectory(taskIdObj);
    
    if (taskState.isDone()) {
      healthchecker.cancelHealthcheck(taskId);
      newTaskChecker.cancelNewTaskCheck(taskId);
      
      scheduler.handleCompletedTask(maybeActiveTask, taskIdObj, taskState, stateCacheProvider.get());
    } else if (maybeActiveTask.isPresent()) {
      Optional<SingularityPendingDeploy> pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());
      
      if (!pendingDeploy.isPresent() || !pendingDeploy.get().getDeployMarker().getDeployId().equals(taskIdObj.getDeployId())) {
        newTaskChecker.enqueueNewTaskCheck(maybeActiveTask.get());
      }
    }
    
  }
  
  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    LOG.info("Framework message from executor {} on slave {} with data {}", executorId, slaveId, JavaUtils.toString(data));
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    LOG.warn("Scheduler/Driver disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    LOG.warn("Lost a slave {}", slaveId);

    rackManager.slaveLost(slaveId);
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    LOG.warn("Lost an executor {} on slave {} with status {}", executorId, slaveId, status);
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    LOG.warn("Error from mesos: {}", message);
  }

}
