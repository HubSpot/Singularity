package com.hubspot.singularity.mesos;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.Status;
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
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskUpdate;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.hooks.WebhookManager;
import com.hubspot.singularity.mesos.SingularityRackManager.RackCheckState;
import com.hubspot.singularity.scheduler.SingularityScheduleStateCache;
import com.hubspot.singularity.scheduler.SingularityScheduler;

public class SingularityMesosScheduler implements Scheduler {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);

  private final Resources DEFAULT_RESOURCES;
  private final TaskManager taskManager;
  private final SingularityScheduler scheduler;
  private final HistoryManager historyManager;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final WebhookManager webhookManager;
  private final SingularityRackManager rackManager;
  private final SingularityLogSupport logSupport;

  private final Provider<SingularityScheduleStateCache> stateCacheProvider;
  
  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, TaskManager taskManager, SingularityScheduler scheduler, HistoryManager historyManager, WebhookManager webhookManager, SingularityRackManager rackManager,
      SingularityMesosTaskBuilder mesosTaskBuilder, SingularityLogSupport logSupport, Provider<SingularityScheduleStateCache> stateCacheProvider) {
    DEFAULT_RESOURCES = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0);
    this.taskManager = taskManager;
    this.rackManager = rackManager;
    this.scheduler = scheduler;
    this.historyManager = historyManager;
    this.webhookManager = webhookManager;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.logSupport = logSupport;
    this.stateCacheProvider = stateCacheProvider;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Registered driver %s, with frameworkId %s and master %s", driver, frameworkId, masterInfo));
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Reregistered driver %s, with master %s", driver, masterInfo));
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info(String.format("Received %s offer(s)", offers.size()));

    final long start = System.currentTimeMillis();
    
    final SingularityScheduleStateCache stateCache = stateCacheProvider.get();
    
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
        LOG.trace(String.format("Task %s is due", taskRequest.getPendingTaskId()));
      }
      
      numTasksSeen = tasks.size();

      for (Protos.Offer offer : offers) {
        LOG.trace(String.format("Evaluating offer %s", offer));

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

    LOG.info(String.format("Finished handling offers (%s), accepted %s, declined %s, outstanding tasks %s", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size(), numTasksSeen - acceptedOffers.size()));
  }

  private Optional<SingularityTask> acceptOffer(SchedulerDriver driver, Protos.Offer offer, List<SingularityTaskRequest> tasks, SingularityScheduleStateCache stateCache) {
    for (SingularityTaskRequest taskRequest : tasks) {
      Resources taskResources = DEFAULT_RESOURCES;

      if (taskRequest.getRequest().getResources() != null) {
        taskResources = taskRequest.getRequest().getResources();
      }

      LOG.trace(String.format("Attempting to match resources %s with offer resources %s", taskResources, offer.getResourcesList()));
          
      final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskResources, offer);
      final RackCheckState rackCheckState = rackManager.checkRack(offer, taskRequest, stateCache);
            
      if (matchesResources && rackCheckState.isRackAppropriate()) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offer, taskRequest, taskResources);

        LOG.trace(String.format("Accepted and built task %s", task));
        
        LOG.info(String.format("Launching task %s slot on slave %s (%s)", task.getTaskId(), offer.getSlaveId(), offer.getHostname()));

        taskManager.launchTask(task);

        LOG.debug(String.format("Launching mesos task: %s", task.getMesosTask()));

        Status initialStatus = driver.launchTasks(offer.getId(), ImmutableList.of(task.getMesosTask()));

        LOG.trace(String.format("Task %s launched with status %s", task.getTaskId(), initialStatus.name()));
        
        historyManager.saveTaskHistory(task, initialStatus.name());

        return Optional.of(task);
      } else {
        LOG.trace(String.format("Turning down offer %s for task %s; matched resources: %s, rack state: %s", offer.getId(), taskRequest.getPendingTaskId(), matchesResources, rackCheckState));
      }
    }

    return Optional.absent();
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    LOG.info(String.format("Offer %s rescinded", offerId));
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    final String taskId = status.getTaskId().getValue();
    
    LOG.debug(String.format("Got a status update for task: %s, status - %s", taskId, status));
    
    Optional<SingularityTask> maybeActiveTask = taskManager.getActiveTask(taskId);
    
    if (maybeActiveTask.isPresent()) {
      webhookManager.notify(new SingularityTaskUpdate(maybeActiveTask.get(), status.getState()));
    } else {
      LOG.info(String.format("Got an update for non-active task %s, skipping webhooks", taskId));
    }

    final Date now = new Date();
    
    historyManager.updateTaskHistory(taskId, status.getState().name(), now);
    historyManager.saveTaskUpdate(taskId, status.getState().name(), status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent(), now);

    logSupport.checkDirectory(SingularityTaskId.fromString(taskId));
    
    if (MesosUtils.isTaskDone(status.getState())) {
      if (maybeActiveTask.isPresent()) {
        taskManager.deleteActiveTask(taskId);
      }
      
      scheduler.handleCompletedTask(maybeActiveTask, taskId, status.getState(), stateCacheProvider.get());
    }
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    LOG.info(String.format("Framework message from executor %s on slave %s with data %s", executorId, slaveId, JavaUtils.toString(data)));
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    LOG.warn("Scheduler/Driver disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    LOG.warn(String.format("Lost a slave %s", slaveId));

    rackManager.slaveLost(slaveId);
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    LOG.warn(String.format("Lost an executor %s on slave %s with status %s", executorId, slaveId, status));
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    LOG.warn(String.format("Error from mesos: %s", message));
  }
}
