package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.CommandInfo.URI;
import org.apache.mesos.Protos.Environment;
import org.apache.mesos.Protos.Environment.Variable;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.Status;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskUpdate;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.hooks.WebhookManager;
import com.hubspot.singularity.scheduler.SingularityScheduler;

public class SingularityMesosScheduler implements Scheduler {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosScheduler.class);
  
  private final Resources DEFAULT_RESOURCES;
  private final RequestManager requestManager;
  private final TaskManager taskManager;
  private final SingularityScheduler scheduler;
  private final ObjectMapper objectMapper;
  private final HistoryManager historyManager;
  private final WebhookManager webhookManager;
  
  @Inject
  public SingularityMesosScheduler(MesosConfiguration mesosConfiguration, TaskManager taskManager, RequestManager requestManager, SingularityScheduler scheduler, ObjectMapper objectMapper, HistoryManager historyManager, WebhookManager webhookManager) {
    DEFAULT_RESOURCES = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0);
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.scheduler = scheduler;
    this.objectMapper = objectMapper;
    this.historyManager = historyManager;
    this.webhookManager = webhookManager;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Registered driver %s, with frameworkId %s and master %s", driver, frameworkId, masterInfo));
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    LOG.info(String.format("Reregistered driver %s, with master %s", driver, masterInfo));
  }
 
  private TaskInfo buildTask(Protos.Offer offer, SingularityTaskRequest task, Resources resources) {
    TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(task.getTaskId().toString()));
    
    long[] ports = null;
    Resource portsResource = null;
    
    if (resources.getNumPorts() > 0) {
      portsResource = MesosUtils.getPortsResource(resources.getNumPorts(), offer);
      ports = MesosUtils.getPorts(portsResource, resources.getNumPorts());
    }
    
    if (task.getRequest().getExecutor() != null) {
      bldr.setExecutor(
          ExecutorInfo.newBuilder()
            .setCommand(CommandInfo.newBuilder().setValue(task.getRequest().getExecutor()))
            .setExecutorId(ExecutorID.newBuilder().setValue("custom"))
      );
      
      Object executorData = task.getRequest().getExecutorData();
      
      if (executorData != null) {
        if (executorData instanceof String) {
          bldr.setData(ByteString.copyFromUtf8(executorData.toString()));
        } else {
          try {
            if (ports != null) {
              try {
                Map<String, Object> executorDataMap = (Map<String, Object>) executorData;
                executorDataMap.put("ports", ports);
              } catch (ClassCastException cce) {
                LOG.warn(String.format("Unable to add ports executor data %s because it wasn't a map", executorData), cce);
              }
            }
            
            bldr.setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(executorData)));
          } catch (JsonProcessingException e) {
            LOG.warn(String.format("Unable to process executor data %s as json", executorData), e);
            bldr.setData(ByteString.copyFromUtf8(executorData.toString()));
          }
        }
      } else {
        bldr.setData(ByteString.copyFromUtf8(task.getRequest().getCommand()));
      }
      
    } else {
      CommandInfo.Builder commandBldr = CommandInfo.newBuilder();
      
      commandBldr.setValue(task.getRequest().getCommand());
      
      if (task.getRequest().getUris() != null) {
        for (String uri : task.getRequest().getUris()) {
          commandBldr.addUris(URI.newBuilder().setValue(uri).build());
        }
      }
      
      if (task.getRequest().getEnv() != null || ports != null) {
        Environment.Builder envBldr = Environment.newBuilder();
        
        for (Entry<String, String> envEntry : Objects.firstNonNull(task.getRequest().getEnv(), Collections.<String, String>emptyMap()).entrySet()) {
          envBldr.addVariables(Variable.newBuilder()
              .setName(envEntry.getKey())
              .setValue(envEntry.getValue())
              .build());
        }
        
        if (ports != null) {
          int portNum = 0;
          for (long port : ports) {
            envBldr.addVariables(Variable.newBuilder()
                .setName(String.format("PORT%s", portNum++))
                .setValue(Long.toString(port))
                .build());
          }
        }
      }     
    }
    
    if (portsResource != null) {
      bldr.addResources(portsResource);
    }
    
    bldr.addResources(MesosUtils.getCpuResource(resources.getCpus()));
    bldr.addResources(MesosUtils.getMemoryResource(resources.getMemoryMb()));
    
    bldr.setSlaveId(offer.getSlaveId());
    
    bldr.setName(task.getRequest().getName());
    
    return bldr.build();
  }
  
  private List<SingularityTaskRequest> getDueTasks() {
    final List<SingularityTaskId> tasks = taskManager.getPendingTasks();
      
    final long now = System.currentTimeMillis();
    
    final List<SingularityTaskId> dueTaskIds = Lists.newArrayListWithCapacity(tasks.size());
    
    for (SingularityTaskId task : tasks) {
      if (task.getNextRunAt() <= now) {
        dueTaskIds.add(task);
      } 
    }
    
    final List<SingularityTaskRequest> dueTasks = requestManager.fetchTasks(dueTaskIds);
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
      final List<SingularityTaskRequest> tasks = getDueTasks();
      
      LOG.trace(String.format("Got tasks to match with offers %s", tasks));
      
      numTasksSeen = tasks.size();
      
      for (Protos.Offer offer : offers) {
        LOG.trace(String.format("Evaluating offer %s", offer));
        
        Optional<SingularityTask> accepted = acceptOffer(driver, offer, tasks);
        
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
    
    LOG.info(String.format("Finished handling offers (%s), accepted %s, declined %s, outstanding tasks %s", DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start), acceptedOffers.size(), offers.size() - acceptedOffers.size(), numTasksSeen - acceptedOffers.size()));
  }
  
  private void abort() {
    LOG.error("Abort called - DOING NOTHING");
    //    System.exit(0);
  }
  
  private Optional<SingularityTask> acceptOffer(SchedulerDriver driver, Protos.Offer offer, List<SingularityTaskRequest> tasks) {
    for (SingularityTaskRequest taskRequest : tasks) {
      Resources taskResources = DEFAULT_RESOURCES;
      
      if (taskRequest.getRequest().getResources() != null) {
        taskResources = taskRequest.getRequest().getResources();
      }
     
      if (MesosUtils.doesOfferMatchResources(taskResources, offer)) {
        LOG.info(String.format("Launching task %s slot on slave %s (%s)", taskRequest.getTaskId(), offer.getSlaveId(), offer.getHostname()));
        
        final TaskInfo mesosTask = buildTask(offer, taskRequest, taskResources);
        
        final SingularityTask task = new SingularityTask(taskRequest, offer, mesosTask);
        
        taskManager.launchTask(task);
        
        LOG.debug(String.format("Launching mesos task: %s", mesosTask));
        
        Status initialStatus = driver.launchTasks(offer.getId(), ImmutableList.of(mesosTask));
        
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
