package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.SingularityTask;
import com.hubspot.singularity.data.TaskManager;

public class SingularityScheduler implements Scheduler {
  
  private final Resources DEFAULT_RESOURCES;
  private final TaskManager taskManager;

  @Inject
  public SingularityScheduler(SingularityConfiguration singularityConfiguration, TaskManager taskManager) {
    DEFAULT_RESOURCES = new Resources(singularityConfiguration.getMesosConfiguration().getDefaultCpus(), singularityConfiguration.getMesosConfiguration().getDefaultMemory());
    this.taskManager = taskManager;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    System.out.println("Registered.");
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    System.out.println("Re-registered.");
  }
 
  private TaskInfo buildTask(Protos.Offer offer, SingularityTask task, Resources resources) {
    TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(task.getGuid()))
        .setCommand(CommandInfo.newBuilder().setValue(task.getRequest().getCommand()));
    
    if (task.getRequest().getExecutor() != null) {
      bldr.setExecutor(ExecutorInfo.newBuilder().setName(task.getRequest().getExecutor()));
    }
    
    bldr.addResources(MesosUtils.getCpuResource(resources.getCpus()));
    bldr.addResources(MesosUtils.getMemoryResource(resources.getMemoryMb()));
    
    bldr.setSlaveId(offer.getSlaveId());
    
    bldr.setName(task.getRequest().getName());
    
    return bldr.build();
  }
  
  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    System.out.println("Got offers");
    
    final List<SingularityTask> tasks = taskManager.getPendingTasks();
    Collections.shuffle(tasks);
    
    for (Protos.Offer offer : offers) {
      SingularityTask accepted = null;
      
      for (SingularityTask task : tasks) {
        Resources taskResources = DEFAULT_RESOURCES;
        
        if (task.getRequest().getResources() != null) {
          taskResources = task.getRequest().getResources();
        }
        
        if (MesosUtils.doesOfferMatchResources(taskResources, offer)) {
          System.out.println(String.format("Task %s slot on slave %s (%s)", task.getRequest().getName(), offer.getSlaveId(), offer.getHostname()));
          
          taskManager.launchTask(task);
          
          final TaskInfo mesosTask = buildTask(offer, task, taskResources);
          
          Status initialStatus = driver.launchTasks(offer.getId(), ImmutableList.of(mesosTask));
          
          try {
            taskManager.recordStatus(initialStatus.name(), mesosTask.getTaskId().getValue(), Optional.<String> absent());
          } catch (Throwable t) {
            // TODO logg this.
            t.printStackTrace();
          }
          
          accepted = task;
          break;
        }
      }
      
      if (accepted == null) {
        driver.declineOffer(offer.getId());
      } else {
        tasks.remove(accepted);
      }
    }
    
    System.out.println("finished w. offers");
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    System.out.println("Offer rescinded");
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    taskManager.recordStatus(status.getState().name(), status.getTaskId().getValue(), status.hasMessage() ? Optional.of(status.getMessage()) : Optional.<String> absent());
    System.out.println("status update:" + status);
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    System.out.println("framework message: " + new String(data));
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    System.out.println("disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    System.out.println("lost slave");
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    System.out.println("lost executor");
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    System.out.println("error");
  }
}
