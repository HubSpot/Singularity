package com.hubspot.singularity.mesos;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityCreateResult;
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

        Collection<SingularityTask> accepted = acceptOffer(driver, offer, tasks, stateCache);

        if (accepted.isEmpty()) {
          driver.declineOffer(offer.getId());
        } else {
          acceptedOffers.add(offer.getId());
          for (SingularityTask acceptedTask : accepted) {
            tasks.remove(acceptedTask.getTaskRequest());
          }
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
        offers.size() - acceptedOffers.size(), numTasksSeen - acceptedOffers.size());
  }

  private List<SingularityTask> acceptOffer(SchedulerDriver driver, Protos.Offer offer, List<SingularityTaskRequest> tasks, SingularitySchedulerStateCache stateCache) {
    final List<SingularityTask> accepted = Lists.newArrayListWithCapacity(tasks.size());
    List<Resource> resources = offer.getResourcesList();
    
    for (SingularityTaskRequest taskRequest : tasks) {
      Resources taskResources = DEFAULT_RESOURCES;

      if (taskRequest.getDeploy().getResources().isPresent()) {
        taskResources = taskRequest.getDeploy().getResources().get();
      }

      LOG.trace("Attempting to match task {} resources {} with remaining offer resources {}", taskRequest.getPendingTask().getPendingTaskId(), taskResources, resources);
          
      final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskResources, resources);
      final RackCheckState rackCheckState = rackManager.checkRack(offer, taskRequest, stateCache);
            
      if (matchesResources && rackCheckState.isRackAppropriate()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offer, resources, taskRequest, taskResources);

        resources = MesosUtils.subtractResources(resources, task.getMesosTask().getResourcesList());
        
        LOG.trace("Accepted and built task {}", task);
        
        LOG.info("Launching task {} slot on slave {} ({})", task.getTaskId(), offer.getSlaveId().getValue(), offer.getHostname());

        taskManager.createTaskAndDeletePendingTask(task);
        
        stateCache.getActiveTaskIds().add(task.getTaskId());
        stateCache.getScheduledTasks().remove(taskRequest.getPendingTask());
        
        accepted.add(task);
      } else {
        LOG.trace("Ignoring offer {} for task {}; matched resources: {}, rack state: {}", offer.getId(), taskRequest.getPendingTask().getPendingTaskId(), matchesResources, rackCheckState);
      }
    }
  
    if (accepted.isEmpty()) {
      return accepted;
    }
    
    final List<TaskInfo> toLaunch = Lists.newArrayListWithCapacity(accepted.size());
    final List<SingularityTaskId> taskIds = Lists.newArrayListWithCapacity(accepted.size());
    for (SingularityTask task : accepted) {
      taskIds.add(task.getTaskId());
      toLaunch.add(task.getMesosTask());
      LOG.trace("Launching {} mesos task: {}", task.getTaskId(), task.getMesosTask()); 
    }

    Status initialStatus = driver.launchTasks(ImmutableList.of(offer.getId()), toLaunch);
    
    LOG.info("{} tasks ({}) launched with status {}", taskIds.size(), taskIds, initialStatus.name());
    
    return accepted;
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info("Offer {} rescinded", offerId);
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {    
    final String taskId = status.getTaskId().getValue();
    
    LOG.debug("Task {} is now {} ({})", taskId, status.getState(), status.getMessage());
    
    final SingularityTaskId taskIdObj = SingularityTaskId.fromString(taskId);
    final ExtendedTaskState taskState = ExtendedTaskState.fromTaskState(status.getState());
    
    final long now = System.currentTimeMillis();
    
    final Optional<SingularityTask> maybeActiveTask = taskManager.getActiveTask(taskId);
    
    Optional<SingularityPendingDeploy> pendingDeploy = null;
    
    if (maybeActiveTask.isPresent() && status.getState() == TaskState.TASK_RUNNING) {
      pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());
      
      healthchecker.enqueueHealthcheck(maybeActiveTask.get(), pendingDeploy);
    }
    
    final SingularityTaskHistoryUpdate taskUpdate = new SingularityTaskHistoryUpdate(taskIdObj, now, taskState, status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent());
    final SingularityCreateResult taskHistoryUpdateCreateResult = taskManager.saveTaskHistoryUpdate(taskUpdate);
    
    logSupport.checkDirectory(taskIdObj);
    
    if (taskState.isDone()) {
      healthchecker.cancelHealthcheck(taskId);
      newTaskChecker.cancelNewTaskCheck(taskId);
      
      taskManager.deleteKilledRecord(taskIdObj);
      
      scheduler.handleCompletedTask(maybeActiveTask, taskIdObj, taskState, taskHistoryUpdateCreateResult, stateCacheProvider.get());
    } else if (maybeActiveTask.isPresent()) {
      if (pendingDeploy == null) {
        pendingDeploy = deployManager.getPendingDeploy(taskIdObj.getRequestId());
      }
      
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
