package com.hubspot.singularity.mesos;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
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

  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, TaskManager taskManager, SingularityScheduler scheduler, HistoryManager historyManager, WebhookManager webhookManager, SingularityRackManager rackManager,
      SingularityMesosTaskBuilder mesosTaskBuidler) {
    DEFAULT_RESOURCES = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0);
    this.taskManager = taskManager;
    this.rackManager = rackManager;
    this.scheduler = scheduler;
    this.historyManager = historyManager;
    this.webhookManager = webhookManager;
    this.mesosTaskBuilder = mesosTaskBuidler;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Registered driver %s, with frameworkId %s and master %s", driver, frameworkId, masterInfo));

    loadRacks(masterInfo);
  }

  private void loadRacks(MasterInfo masterInfo) {
    try {
      rackManager.loadRacksFromMaster(masterInfo);
    } catch (Throwable t) {
      LOG.error("Fatal - while registering and retrieving rack information", t);
      abort();
    }
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Reregistered driver %s, with master %s", driver, masterInfo));

    loadRacks(masterInfo);
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info(String.format("Recieved %s offer(s)", offers.size()));

    final long start = System.currentTimeMillis();

    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());

    for (Protos.Offer offer : offers) {
      rackManager.checkOffer(offer);
    }

    int numTasksSeen = 0;

    try {
      final List<SingularityTaskRequest> tasks = scheduler.getDueTasks();
      final List<SingularityTaskId> activeTasks = taskManager.getActiveTaskIds();

      LOG.trace(String.format("Got tasks to match with offers %s", tasks));

      numTasksSeen = tasks.size();

      for (Protos.Offer offer : offers) {
        LOG.trace(String.format("Evaluating offer %s", offer));

        Optional<SingularityTask> accepted = acceptOffer(driver, offer, tasks, activeTasks);

        if (!accepted.isPresent()) {
          driver.declineOffer(offer.getId());
        } else {
          acceptedOffers.add(offer.getId());
          tasks.remove(accepted.get().getTaskRequest());
        }
      }
    } catch (Throwable t) {
      LOG.error("Fatal - while accepting offers", t);

      for (Protos.Offer offer : offers) {
        if (acceptedOffers.contains(offer.getId())) {
          continue;
        }

        try {
          driver.declineOffer(offer.getId());
        } catch (Throwable d) {
          LOG.error("While decling an offer", d);
        }
      }

      abort();
    }

    LOG.info(String.format("Finished handling offers (%s), accepted %s, declined %s, outstanding tasks %s", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start), acceptedOffers.size(),
        offers.size() - acceptedOffers.size(), numTasksSeen - acceptedOffers.size()));
  }

  private void abort() {
    ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
    // Check for logback implementation of slf4j
    if (loggerFactory instanceof LoggerContext) {
      LoggerContext context = (LoggerContext) loggerFactory;
      context.stop();
    }

    LOG.error("Abort called - DOING NOTHING");
    // System.exit(0);
  }

  private boolean isRackAppropriate(Protos.Offer offer, SingularityTaskRequest taskRequest, List<SingularityTaskId> activeTasks) {
    if (!taskRequest.getRequest().isRackSensitive()) {
      return true;
    }

    List<SingularityTaskId> matchingTasks = Lists.newArrayList();

    for (SingularityTaskId activeTask : activeTasks) {
      if (activeTask.matches(taskRequest.getPendingTaskId())) {
        matchingTasks.add(activeTask);
      }
    }

    return rackManager.checkRack(offer, taskRequest, matchingTasks);
  }

  private Optional<SingularityTask> acceptOffer(SchedulerDriver driver, Protos.Offer offer, List<SingularityTaskRequest> tasks, List<SingularityTaskId> activeTasks) {
    for (SingularityTaskRequest taskRequest : tasks) {
      Resources taskResources = DEFAULT_RESOURCES;

      if (taskRequest.getRequest().getResources() != null) {
        taskResources = taskRequest.getRequest().getResources();
      }

      if (MesosUtils.doesOfferMatchResources(taskResources, offer) && isRackAppropriate(offer, taskRequest, activeTasks)) {
        final SingularityTask task = mesosTaskBuilder.buildTask(offer, taskRequest, taskResources);

        LOG.info(String.format("Launching task %s slot on slave %s (%s)", task.getTaskId(), offer.getSlaveId(), offer.getHostname()));

        taskManager.launchTask(task);

        LOG.debug(String.format("Launching mesos task: %s", task.getTask()));

        Status initialStatus = driver.launchTasks(offer.getId(), ImmutableList.of(task.getTask()));

        historyManager.saveTaskHistory(task, initialStatus.name());

        return Optional.of(task);
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
    LOG.info(String.format("Got a status update: %s", status));

    webhookManager.notify(new SingularityTaskUpdate(taskManager.getActiveTask(status.getTaskId().getValue()).get(), status.getState()));

    try {
      historyManager.saveTaskUpdate(status.getTaskId().getValue(), status.getState().name(), status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent());

      if (MesosUtils.isTaskDone(status.getState())) {
        taskManager.deleteActiveTask(status.getTaskId().getValue());

        scheduler.scheduleOnCompletion(status.getState(), status.getTaskId().getValue());
      }
    } catch (Throwable t) {
      LOG.error("FATAL - while recording a status update", t);
      abort();
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
    LOG.warn(String.format("Lost an executor %s on slave %s with status", executorId, slaveId, status));
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    LOG.warn(String.format("Error from mesos: %s", message));
  }
}
