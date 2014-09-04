package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.CommandInfo.URI;
import org.apache.mesos.Protos.ContainerInfo;
import org.apache.mesos.Protos.ContainerInfo.DockerInfo;
import org.apache.mesos.Protos.Environment;
import org.apache.mesos.Protos.Environment.Variable;
import org.apache.mesos.Protos.ExecutorID;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskID;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExecutorDataBuilder;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.ExecutorIdGenerator;

public class SingularityMesosTaskBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosTaskBuilder.class);

  private final ObjectMapper objectMapper;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final ExecutorIdGenerator idGenerator;

  @Inject
  public SingularityMesosTaskBuilder(ObjectMapper objectMapper, SingularitySlaveAndRackManager slaveAndRackManager, ExecutorIdGenerator idGenerator) {
    this.objectMapper = objectMapper;
    this.slaveAndRackManager = slaveAndRackManager;
    this.idGenerator = idGenerator;
  }

  public SingularityTask buildTask(Protos.Offer offer, List<Resource> availableResources, SingularityTaskRequest taskRequest, Resources desiredTaskResources) {
    final String rackId = slaveAndRackManager.getRackId(offer);
    final String host = slaveAndRackManager.getSlaveHost(offer);

    final SingularityTaskId taskId = new SingularityTaskId(taskRequest.getPendingTask().getPendingTaskId().getRequestId(), taskRequest.getDeploy().getId(), System.currentTimeMillis(), taskRequest.getPendingTask().getPendingTaskId().getInstanceNo(), host, rackId);

    final TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId.toString()));

    Optional<long[]> ports = Optional.absent();
    Optional<Resource> portsResource = Optional.absent();

    if (desiredTaskResources.getNumPorts() > 0) {
      portsResource = Optional.of(MesosUtils.getPortsResource(desiredTaskResources.getNumPorts(), availableResources));
      ports = Optional.of(MesosUtils.getPorts(portsResource.get(), desiredTaskResources.getNumPorts()));
    }

    final Optional<SingularityContainerInfo> containerInfo = taskRequest.getDeploy().getContainerInfo();
    if (containerInfo.isPresent()) {
      prepareContainerInfo(bldr, containerInfo.get());
    }

    if (taskRequest.getDeploy().getCustomExecutorCmd().isPresent()) {
      prepareCustomExecutor(bldr, taskId, taskRequest, ports);
    } else {
      prepareCommand(bldr, taskId, taskRequest, ports);
    }

    if (portsResource.isPresent()) {
      bldr.addResources(portsResource.get());
    }

    bldr.addResources(MesosUtils.getCpuResource(desiredTaskResources.getCpus()));
    bldr.addResources(MesosUtils.getMemoryResource(desiredTaskResources.getMemoryMb()));

    bldr.setSlaveId(offer.getSlaveId());

    bldr.setName(taskRequest.getRequest().getId());

    TaskInfo task = bldr.build();

    return new SingularityTask(taskRequest, taskId, offer, task);
  }

  private void prepareEnvironment(final SingularityTaskRequest task, CommandInfo.Builder commandBuilder, final Optional<long[]> ports) {
    Environment.Builder envBldr = Environment.newBuilder();

    envBldr.addVariables(Variable.newBuilder()
        .setName("INSTANCE_NO")
        .setValue(Integer.toString(task.getPendingTask().getPendingTaskId().getInstanceNo()))
        .build());

    envBldr.addVariables(Variable.newBuilder()
        .setName("TASK_REQUEST_ID")
        .setValue(task.getPendingTask().getPendingTaskId().getRequestId())
        .build());

    for (Entry<String, String> envEntry : task.getDeploy().getEnv().or(Collections.<String, String>emptyMap()).entrySet()) {
      envBldr.addVariables(Variable.newBuilder()
          .setName(envEntry.getKey())
          .setValue(envEntry.getValue())
          .build());
    }

    if (ports.isPresent()) {
      for (int portNum = 0; portNum < ports.get().length; portNum++) {
        if (portNum == 0) {
          envBldr.addVariables(Variable.newBuilder()
              .setName("PORT")
              .setValue(Long.toString(ports.get()[portNum]))
              .build());
        }

        envBldr.addVariables(Variable.newBuilder()
            .setName(String.format("PORT%s", portNum))
            .setValue(Long.toString(ports.get()[portNum]))
            .build());
      }
    }

    commandBuilder.setEnvironment(envBldr.build());
  }

  private void prepareContainerInfo(final TaskInfo.Builder bldr, final SingularityContainerInfo containerInfo) {
    ContainerInfo.Builder containerBuilder = ContainerInfo.newBuilder();
    containerBuilder.setType(containerInfo.getType());

    final Optional<SingularityDockerInfo> dockerInfo = containerInfo.getDocker();
    if (dockerInfo.isPresent()) {
      containerBuilder.setDocker(DockerInfo.newBuilder().setImage(dockerInfo.get().getImage()));
    }

    for (SingularityVolume volumeInfo : containerInfo.getVolumes().or(Collections.<SingularityVolume>emptyList())) {
      final Volume.Builder volumeBuilder = Volume.newBuilder();
      volumeBuilder.setContainerPath(volumeInfo.getContainerPath());
      if (volumeInfo.getHostPath().isPresent()) {
        volumeBuilder.setHostPath(volumeInfo.getHostPath().get());
      }
      volumeBuilder.setMode(volumeInfo.getMode());
      containerBuilder.addVolumes(volumeBuilder);
    }

    bldr.setContainer(containerBuilder);
  }

  private void prepareCustomExecutor(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final Optional<long[]> ports) {
    CommandInfo.Builder commandBuilder = CommandInfo.newBuilder().setValue(task.getDeploy().getCustomExecutorCmd().get());

    prepareEnvironment(task, commandBuilder, ports);

    bldr.setExecutor(
        ExecutorInfo.newBuilder()
        .setCommand(commandBuilder.build())
        .setExecutorId(ExecutorID.newBuilder().setValue(task.getDeploy().getCustomExecutorId().or(idGenerator.getNextExecutorId())))
        .setSource(task.getDeploy().getCustomExecutorSource().or(task.getPendingTask().getPendingTaskId().getId()))
        );

    if (task.getDeploy().getExecutorData().isPresent()) {
      ExecutorData executorData = task.getDeploy().getExecutorData().get();

      if (task.getPendingTask().getMaybeCmdLineArgs().isPresent()) {
        LOG.trace("Adding cmd line args {} to task {} executorData", task.getPendingTask().getMaybeCmdLineArgs().get(), taskId.getId());

        ExecutorDataBuilder executorDataBldr = executorData.toBuilder();

        final ImmutableList.Builder<String> extraCmdLineArgsBuilder = ImmutableList.builder();
        if (executorDataBldr.getExtraCmdLineArgs() != null && !executorDataBldr.getExtraCmdLineArgs().isEmpty()) {
          extraCmdLineArgsBuilder.addAll(executorDataBldr.getExtraCmdLineArgs());
        }
        extraCmdLineArgsBuilder.add(task.getPendingTask().getMaybeCmdLineArgs().get());
        executorDataBldr.setExtraCmdLineArgs(extraCmdLineArgsBuilder.build());

        executorData = executorDataBldr.build();
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

    if (task.getDeploy().getCommand().isPresent()) {
      commandBldr.setValue(getCommand(taskId, task));
    }

    if (task.getDeploy().getContainerInfo().isPresent()) {
      // Hopefully temporary workaround for
      // http://www.mail-archive.com/user@mesos.apache.org/msg01449.html
      commandBldr.setShell(false);
    }

    for (String uri : task.getDeploy().getUris().or(Collections.<String> emptyList())) {
      commandBldr.addUris(URI.newBuilder().setValue(uri).build());
    }

    prepareEnvironment(task, commandBldr, ports);

    bldr.setCommand(commandBldr);
  }

}
