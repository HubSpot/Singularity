package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.CommandInfo.URI;
import org.apache.mesos.Protos.Environment;
import org.apache.mesos.Protos.Environment.Variable;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;

public class SingularityMesosTaskBuilder {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityMesosTaskBuilder.class);
  
  private final ObjectMapper objectMapper;
  private final SingularityRackManager rackManager;
  
  @Inject
  public SingularityMesosTaskBuilder(ObjectMapper objectMapper, SingularityRackManager rackManager) {
    this.objectMapper = objectMapper;
    this.rackManager = rackManager;
  }
  
  public SingularityTask buildTask(Protos.Offer offer, SingularityTaskRequest taskRequest, Resources resources) {
    final String rackId = rackManager.getRackId(offer);
    final String host = rackManager.getSlaveHost(offer);
    
    final SingularityTaskId taskId = new SingularityTaskId(taskRequest.getPendingTask().getPendingTaskId().getRequestId(), taskRequest.getDeploy().getId(), System.currentTimeMillis(), taskRequest.getPendingTask().getPendingTaskId().getInstanceNo(), host, rackId);
    
    final TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId.toString()));
    
    Optional<long[]> ports = Optional.absent();
    Optional<Resource> portsResource = Optional.absent();
    
    if (resources.getNumPorts() > 0) {
      portsResource = Optional.of(MesosUtils.getPortsResource(resources.getNumPorts(), offer));
      ports = Optional.of(MesosUtils.getPorts(portsResource.get(), resources.getNumPorts()));
    }
    
    if (taskRequest.getDeploy().getCustomExecutorCmd().isPresent()) {
      prepareCustomExecutor(bldr, taskId, taskRequest, ports);
    } else {
      prepareCommand(bldr, taskId, taskRequest, ports);
    }
    
    if (portsResource.isPresent()) {
      bldr.addResources(portsResource.get());
    }
    
    bldr.addResources(MesosUtils.getCpuResource(resources.getCpus()));
    bldr.addResources(MesosUtils.getMemoryResource(resources.getMemoryMb()));
    
    bldr.setSlaveId(offer.getSlaveId());
    
    bldr.setName(taskRequest.getRequest().getId());
    
    TaskInfo task = bldr.build();
  
    return new SingularityTask(taskRequest, taskId, offer, task);
  }
  
  private void prepareCustomExecutor(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final Optional<long[]> ports) {
    bldr.setExecutor(
        ExecutorInfo.newBuilder()
          .setCommand(CommandInfo.newBuilder().setValue(task.getDeploy().getCustomExecutorCmd().get()))
          .setExecutorId(ExecutorID.newBuilder().setValue(task.getDeploy().getCustomExecutorId().or(String.format("singularity-%s", taskId.toString().replace(':', '_')))))
    );
    
    if (task.getDeploy().getExecutorData().isPresent()) {
      ExecutorData executorData = task.getDeploy().getExecutorData().get();
      
      if (task.getPendingTask().getMaybeCmdLineArgs().isPresent()) {
        LOG.trace("Adding cmd line args {} to task {} executorData", task.getPendingTask().getMaybeCmdLineArgs().get(), taskId.getId());
        
        List<String> cmdLineArgs = Lists.newArrayList(executorData.getExtraCmdLineArgs());
        cmdLineArgs.add(task.getPendingTask().getMaybeCmdLineArgs().get());
        
        executorData = new ExecutorData(executorData.getCmd(), executorData.getEmbeddedArtifacts(), executorData.getExternalArtifacts(), executorData.getEnv(), 
            executorData.getSuccessfulExitCodes(), executorData.getUser().orNull(), executorData.getRunningSentinel().orNull(), cmdLineArgs);
      }
      
      try {
        bldr.setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(executorData)));
      } catch (JsonProcessingException e) {
        LOG.warn("Unable to process executor data {} for task {} as json (trying as string)", executorData, taskId.getId(), e);
        
        bldr.setData(ByteString.copyFromUtf8(executorData.toString()));
      }
    } else {
      bldr.setData(ByteString.copyFromUtf8(getCommand(taskId, task)));
    }
  }
  
  private String getCommand(final SingularityTaskId taskId, final SingularityTaskRequest task) {
    String cmd = task.getDeploy().getCommand().get();
    
    if (task.getPendingTask().getMaybeCmdLineArgs().isPresent()) {
      cmd = String.format("%s %s", cmd, task.getPendingTask().getMaybeCmdLineArgs().get());
      LOG.info("Adding command line args ({}) to task {} - new cmd: {}", task.getPendingTask().getMaybeCmdLineArgs().get(), taskId.getId(), cmd);
    }
    
    return cmd;
  }
  
  private void prepareCommand(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final Optional<long[]> ports) {
    CommandInfo.Builder commandBldr = CommandInfo.newBuilder();
    
    commandBldr.setValue(getCommand(taskId, task));
    
    for (String uri : task.getDeploy().getUris().or(Collections.<String> emptyList())) {
      commandBldr.addUris(URI.newBuilder().setValue(uri).build());
    }
  
    bldr.setCommand(commandBldr);
    
    if (task.getDeploy().getEnv().isPresent() || ports.isPresent()) {
      Environment.Builder envBldr = Environment.newBuilder();
      
      for (Entry<String, String> envEntry : task.getDeploy().getEnv().or(Collections.<String, String>emptyMap()).entrySet()) {
        envBldr.addVariables(Variable.newBuilder()
            .setName(envEntry.getKey())
            .setValue(envEntry.getValue())
            .build());
      }
      
      if (ports.isPresent()) {
        int portNum = 0;
        for (long port : ports.get()) {
          envBldr.addVariables(Variable.newBuilder()
              .setName(String.format("PORT%s", portNum++))
              .setValue(Long.toString(port))
              .build());
        }
      }
    }
  }

}
