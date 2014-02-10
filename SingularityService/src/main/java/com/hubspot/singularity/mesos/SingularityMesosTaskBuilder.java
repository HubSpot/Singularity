package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.Map;
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
import com.google.common.base.Objects;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
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
    
    final SingularityTaskId taskId = new SingularityTaskId(taskRequest.getPendingTaskId().getRequestId(), System.currentTimeMillis(), taskRequest.getPendingTaskId().getInstanceNo(), host, rackId);
    
    final TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId.toString()));
    
    long[] ports = null;
    Resource portsResource = null;
    
    if (resources.getNumPorts() > 0) {
      portsResource = MesosUtils.getPortsResource(resources.getNumPorts(), offer);
      ports = MesosUtils.getPorts(portsResource, resources.getNumPorts());
    }
    
    if (taskRequest.getRequest().getExecutor() != null) {
      prepareCustomExecutor(bldr, taskId, taskRequest, ports);
    } else {
      prepareCommand(bldr, taskRequest, ports);
    }
    
    if (portsResource != null) {
      bldr.addResources(portsResource);
    }
    
    bldr.addResources(MesosUtils.getCpuResource(resources.getCpus()));
    bldr.addResources(MesosUtils.getMemoryResource(resources.getMemoryMb()));
    
    bldr.setSlaveId(offer.getSlaveId());
    
    bldr.setName(taskRequest.getRequest().getId());
    
    TaskInfo task = bldr.build();
  
    return new SingularityTask(taskRequest, taskId, offer, task);
  }
  
  @SuppressWarnings("unchecked")
  private void prepareCustomExecutor(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final long[] ports) {
    bldr.setExecutor(
        ExecutorInfo.newBuilder()
          .setCommand(CommandInfo.newBuilder().setValue(task.getRequest().getExecutor()))
          .setExecutorId(ExecutorID.newBuilder().setValue(String.format("singularity-%s", taskId.toString().replace(':', '_'))))
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
  }
  
  private void prepareCommand(final TaskInfo.Builder bldr, final SingularityTaskRequest task, final long[] ports) {
    CommandInfo.Builder commandBldr = CommandInfo.newBuilder();
    
    commandBldr.setValue(task.getRequest().getCommand());
    
    if (task.getRequest().getUris() != null) {
      for (String uri : task.getRequest().getUris()) {
        commandBldr.addUris(URI.newBuilder().setValue(uri).build());
      }
    }

    bldr.setCommand(commandBldr);
    
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

}
