package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityTask;
import com.hubspot.singularity.data.SingularityTaskId;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.scheduler.SingularityScheduler;

public class SingularityMesosScheduler implements Scheduler {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);
  
  private final Resources DEFAULT_RESOURCES;
  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final SingularityScheduler scheduler;
  
  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, TaskManager taskManager, RequestManager requestManager, SingularityScheduler scheduler) {
    DEFAULT_RESOURCES = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory());
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.scheduler = scheduler;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Registered driver %s, with frameworkId %s and master %s", driver, frameworkId, masterInfo));
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Reregistered driver %s, with master %s", driver, masterInfo));
  }
 
  private TaskInfo buildTask(Protos.Offer offer, SingularityTask task, Resources resources) {
    TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(task.getTaskId().toString()));
    
    if (task.getRequest().getExecutor() != null) {
      bldr.setExecutor(
          ExecutorInfo.newBuilder()
            .setCommand(CommandInfo.newBuilder().setValue(task.getRequest().getExecutor()))
            .setExecutorId(ExecutorID.newBuilder().setValue("custom"))
            .setData(ByteString.copyFromUtf8(task.getRequest().getCommand()))
      );
    } else {
      bldr.setCommand(CommandInfo.newBuilder().setValue(task.getRequest().getCommand()));
    }
    
    bldr.addResources(MesosUtils.getCpuResource(resources.getCpus()));
    bldr.addResources(MesosUtils.getMemoryResource(resources.getMemoryMb()));
    
    bldr.setSlaveId(offer.getSlaveId());
    
    bldr.setName(task.getRequest().getName());
    
    return bldr.build();
  }
  
  private List<SingularityTask> getDueTasks() {
    final List<SingularityTaskId> tasks = taskManager.getPendingTasks();
      
    final long now = System.currentTimeMillis();
    
    final List<SingularityTaskId> dueTaskIds = Lists.newArrayListWithCapacity(tasks.size());
    
    for (SingularityTaskId task : tasks) {
      if (task.getNextRunAt() <= now) {
        dueTaskIds.add(task);
      } 
    }
    
    final List<SingularityTask> dueTasks = requestManager.fetchTasks(dueTaskIds);
    Collections.sort(dueTasks);
  
    return dueTasks;
  }
  
  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    LOG.info(String.format("Recieved %s offer(s)", offers.size()));
    
    final long start = System.currentTimeMillis();
    
    final Set<Protos.OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(offers.size());
    
    int numTasksSeen = 0;
    
    try {
      final List<SingularityTask> tasks = getDueTasks();
      
      LOG.debug(String.format("Got tasks to match with offers %s", tasks));
      
      numTasksSeen = tasks.size();
      
      for (Protos.Offer offer : offers) {
        LOG.debug(String.format("Evaluating offer %s", offer));
        
        Optional<SingularityTask> accepted = acceptOffer(driver, offer, tasks);
        
        if (!accepted.isPresent()) {
          driver.declineOffer(offer.getId());
        } else {
          acceptedOffers.add(offer.getId());
          tasks.remove(accepted.get());
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
    
    LOG.info(String.format("Finished handling offers (%s), accepted %s, declined %s, outstanding tasks %s", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start), acceptedOffers.size(), offers.size(), numTasksSeen - acceptedOffers.size()));
  }
  
  private void abort() {
    LOG.error("Aborting");
    System.out.println("Aborting... due to failure");
//    System.exit(0);
  }
  
  private Optional<SingularityTask> acceptOffer(SchedulerDriver driver, Protos.Offer offer, List<SingularityTask> tasks) {
    for (SingularityTask task : tasks) {
      Resources taskResources = DEFAULT_RESOURCES;
      
      if (task.getRequest().getResources() != null) {
        taskResources = task.getRequest().getResources();
      }
     
      if (MesosUtils.doesOfferMatchResources(taskResources, offer)) {
        LOG.info(String.format("Launching task %s slot on slave %s (%s)", task.getTaskId(), offer.getSlaveId(), offer.getHostname()));
        
        taskManager.launchTask(task);
        
        final TaskInfo mesosTask = buildTask(offer, task, taskResources);
        
        LOG.debug(String.format("Launching mesos task: %s", mesosTask));
        
        Status initialStatus = driver.launchTasks(offer.getId(), ImmutableList.of(mesosTask));
        
        taskManager.recordStatus(initialStatus.name(), mesosTask.getTaskId().getValue(), Optional.<String> absent());
        
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
    
    try {
      taskManager.recordStatus(status.getState().name(), status.getTaskId().getValue(), status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent());
    
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
