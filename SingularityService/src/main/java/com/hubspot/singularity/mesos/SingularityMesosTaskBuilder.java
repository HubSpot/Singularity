package com.hubspot.singularity.mesos;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.inject.Singleton;

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
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExecutorDataBuilder;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ExecutorIdGenerator;

@Singleton
class SingularityMesosTaskBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosTaskBuilder.class);

  private final ObjectMapper objectMapper;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final ExecutorIdGenerator idGenerator;
  private final SingularityConfiguration configuration;

  @Inject
  SingularityMesosTaskBuilder(ObjectMapper objectMapper, SingularitySlaveAndRackManager slaveAndRackManager, ExecutorIdGenerator idGenerator, SingularityConfiguration configuration) {
    this.objectMapper = objectMapper;
    this.slaveAndRackManager = slaveAndRackManager;
    this.idGenerator = idGenerator;
    this.configuration = configuration;
  }

  public SingularityTask buildTask(Protos.Offer offer, List<Resource> availableResources, SingularityTaskRequest taskRequest, Resources desiredTaskResources, Resources desiredExecutorResources) {
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

    if (taskRequest.getDeploy().getCustomExecutorCmd().isPresent()) {
      prepareCustomExecutor(bldr, taskId, taskRequest, ports, desiredExecutorResources);
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

  private void setEnv(Environment.Builder envBldr, String key, Object value) {
    if (value == null) {
      return;
    }
    envBldr.addVariables(Variable.newBuilder().setName(key).setValue(value.toString()));
  }

  private void prepareEnvironment(final SingularityTaskRequest task, SingularityTaskId taskId, CommandInfo.Builder commandBuilder, final Optional<long[]> ports) {
    Environment.Builder envBldr = Environment.newBuilder();

    setEnv(envBldr, "INSTANCE_NO", task.getPendingTask().getPendingTaskId().getInstanceNo());
    setEnv(envBldr, "TASK_HOST", taskId.getHost());
    setEnv(envBldr, "TASK_REQUEST_ID", task.getPendingTask().getPendingTaskId().getRequestId());
    setEnv(envBldr, "TASK_DEPLOY_ID", taskId.getDeployId());
    setEnv(envBldr, "ESTIMATED_INSTANCE_COUNT", task.getRequest().getInstancesSafe());

    for (Entry<String, String> envEntry : task.getDeploy().getEnv().or(Collections.<String, String>emptyMap()).entrySet()) {
      setEnv(envBldr, envEntry.getKey(), envEntry.getValue());
    }

    if (ports.isPresent()) {
      for (int portNum = 0; portNum < ports.get().length; portNum++) {
        if (portNum == 0) {
          setEnv(envBldr, "PORT", ports.get()[portNum]);
        }

        setEnv(envBldr, String.format("PORT%s", portNum), ports.get()[portNum]);
      }
    }

    commandBuilder.setEnvironment(envBldr.build());
  }

  private Optional<DockerInfo.PortMapping> buildPortMapping(final SingularityDockerPortMapping singularityDockerPortMapping, long[] ports) {
    final int containerPort;
    switch (singularityDockerPortMapping.getContainerPortType()) {
      case LITERAL:
        containerPort = singularityDockerPortMapping.getContainerPort();
        break;
      case FROM_OFFER:
        containerPort = Ints.checkedCast(ports[singularityDockerPortMapping.getContainerPort()]);
        break;
      default:
        return Optional.absent();
    }

    final int hostPort;
    switch (singularityDockerPortMapping.getHostPortType()) {
      case LITERAL:
        hostPort = singularityDockerPortMapping.getHostPort();
        break;
      case FROM_OFFER:
        hostPort = Ints.checkedCast(ports[singularityDockerPortMapping.getHostPort()]);
        break;
      default:
        return Optional.absent();
    }

    return Optional.of(DockerInfo.PortMapping.newBuilder()
        .setContainerPort(containerPort)
        .setHostPort(hostPort)
        .setProtocol(singularityDockerPortMapping.getProtocol())
        .build());
  }

  private ContainerInfo prepareContainerInfo(final SingularityContainerInfo containerInfo, final Optional<long[]> ports) {
    ContainerInfo.Builder containerBuilder = ContainerInfo.newBuilder();
    containerBuilder.setType(containerInfo.getType());

    final Optional<SingularityDockerInfo> dockerInfo = containerInfo.getDocker();

    if (dockerInfo.isPresent()) {
      final DockerInfo.Builder dockerInfoBuilder = DockerInfo.newBuilder();
      dockerInfoBuilder.setImage(dockerInfo.get().getImage());

      if (ports.isPresent() && !dockerInfo.get().getPortMappings().isEmpty()) {
        for (SingularityDockerPortMapping singularityDockerPortMapping : dockerInfo.get().getPortMappings()) {
          final Optional<DockerInfo.PortMapping> maybePortMapping = buildPortMapping(singularityDockerPortMapping, ports.get());

          if (maybePortMapping.isPresent()) {
            dockerInfoBuilder.addPortMappings(maybePortMapping.get());
          }
        }

        if (dockerInfo.get().getNetwork().isPresent()) {
          dockerInfoBuilder.setNetwork(dockerInfo.get().getNetwork().get());
        }
      }

      dockerInfoBuilder.setPrivileged(dockerInfo.get().isPrivileged());

      containerBuilder.setDocker(dockerInfoBuilder);
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

    return containerBuilder.build();
  }

  private List<Resource> buildMesosResources(final Resources resources) {
    ImmutableList.Builder<Resource> builder = ImmutableList.builder();

    if (resources.getCpus() > 0) {
      builder.add(MesosUtils.getCpuResource(resources.getCpus()));
    }

    if (resources.getMemoryMb() > 0) {
      builder.add(MesosUtils.getMemoryResource(resources.getMemoryMb()));
    }

    return builder.build();
  }

  private void prepareCustomExecutor(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final Optional<long[]> ports, final Resources desiredExecutorResources) {
    CommandInfo.Builder commandBuilder = CommandInfo.newBuilder().setValue(task.getDeploy().getCustomExecutorCmd().get());

    prepareEnvironment(task, taskId, commandBuilder, ports);

    final ExecutorInfo.Builder executorInfoBuilder = ExecutorInfo.newBuilder()
            .setCommand(commandBuilder.build())
            .setExecutorId(ExecutorID.newBuilder().setValue(task.getDeploy().getCustomExecutorId().or(idGenerator.getNextExecutorId())))
            .setSource(task.getDeploy().getCustomExecutorSource().or(task.getPendingTask().getPendingTaskId().getId()))
            .addAllResources(buildMesosResources(desiredExecutorResources));

    if (task.getDeploy().getContainerInfo().isPresent()) {
      executorInfoBuilder.setContainer(prepareContainerInfo(task.getDeploy().getContainerInfo().get(), ports));
    }

    bldr.setExecutor(executorInfoBuilder.build());

    if (task.getDeploy().getExecutorData().isPresent()) {
      ExecutorData executorData = task.getDeploy().getExecutorData().get();

      if (!task.getPendingTask().getCmdLineArgsList().isEmpty()) {
        LOG.trace("Adding cmd line args {} to task {} executorData", task.getPendingTask().getCmdLineArgsList(), taskId.getId());

        ExecutorDataBuilder executorDataBldr = executorData.toBuilder();

        final ImmutableList.Builder<String> extraCmdLineArgsBuilder = ImmutableList.builder();
        if (executorDataBldr.getExtraCmdLineArgs() != null && !executorDataBldr.getExtraCmdLineArgs().isEmpty()) {
          extraCmdLineArgsBuilder.addAll(executorDataBldr.getExtraCmdLineArgs());
        }
        extraCmdLineArgsBuilder.addAll(task.getPendingTask().getCmdLineArgsList());
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
      bldr.setData(ByteString.copyFromUtf8(task.getDeploy().getCommand().get()));
    }
  }


  private void prepareCommand(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final Optional<long[]> ports) {
    CommandInfo.Builder commandBldr = CommandInfo.newBuilder();

    if (task.getDeploy().getCommand().isPresent()) {
      commandBldr.setValue(task.getDeploy().getCommand().get());
    }

    if (task.getDeploy().getArguments().isPresent()) {
      commandBldr.addAllArguments(task.getDeploy().getArguments().get());
    }

    commandBldr.addAllArguments(task.getPendingTask().getCmdLineArgsList());

    if (task.getDeploy().getArguments().isPresent() ||
        // Hopefully temporary workaround for
        // http://www.mail-archive.com/user@mesos.apache.org/msg01449.html
        task.getDeploy().getContainerInfo().isPresent()) {
      commandBldr.setShell(false);
    }

    for (String uri : task.getDeploy().getUris().or(Collections.<String> emptyList())) {
      commandBldr.addUris(URI.newBuilder().setValue(uri).build());
    }

    prepareEnvironment(task, taskId, commandBldr, ports);

    if (task.getDeploy().getContainerInfo().isPresent()) {
      bldr.setContainer(prepareContainerInfo(task.getDeploy().getContainerInfo().get(), ports));
    }

    bldr.setCommand(commandBldr);
  }

}
