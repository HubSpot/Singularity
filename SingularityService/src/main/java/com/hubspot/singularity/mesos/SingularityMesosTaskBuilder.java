package com.hubspot.singularity.mesos;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nonnull;
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
import org.apache.mesos.Protos.Value;
import org.apache.mesos.Protos.Value.Scalar;
import org.apache.mesos.Protos.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.deploy.ExecutorDataBuilder;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityResourceRequest;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.ExecutorIdGenerator;

@Singleton
class SingularityMesosTaskBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosTaskBuilder.class);

  private final ObjectMapper objectMapper;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final ExecutorIdGenerator idGenerator;

  @Inject
  SingularityMesosTaskBuilder(ObjectMapper objectMapper, SingularitySlaveAndRackManager slaveAndRackManager, ExecutorIdGenerator idGenerator) {
    this.objectMapper = objectMapper;
    this.slaveAndRackManager = slaveAndRackManager;
    this.idGenerator = idGenerator;
  }

  public SingularityTask buildTask(Protos.Offer offer, List<Resource> availableResources, SingularityTaskRequest taskRequest, List<SingularityResourceRequest> desiredTaskResources) {
    checkNotNull(offer, "offer is null");
    checkNotNull(availableResources, "availableResources is null");
    checkNotNull(taskRequest, "taskRequest is null");
    checkNotNull(desiredTaskResources, "desiredTaskResources is null");

    final String rackId = slaveAndRackManager.getRackId(offer);
    final String host = slaveAndRackManager.getSlaveHost(offer);

    final SingularityTaskId taskId =
        new SingularityTaskId(taskRequest.getPendingTask().getPendingTaskId().getRequestId(), taskRequest.getDeploy().getId(), System.currentTimeMillis(), taskRequest.getPendingTask()
            .getPendingTaskId().getInstanceNo(), host, rackId);

    final TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId.toString()));

    // Do the easy stuff first.

    // CPU allocation
    final Number cpus = SingularityResourceRequest.findNumberResourceRequest(desiredTaskResources, SingularityResourceRequest.CPU_RESOURCE_NAME, -1);
    if (cpus.intValue() > 0) {
      bldr.addResources(MesosUtils.getCpuResource(cpus.doubleValue()));
    }

    // Memory allocation
    final Number memory = SingularityResourceRequest.findNumberResourceRequest(desiredTaskResources, SingularityResourceRequest.MEMORY_RESOURCE_NAME, -1);
    if (memory.intValue() > 0) {
      bldr.addResources(MesosUtils.getMemoryResource(memory.doubleValue()));
    }

    // Mesos related stuff.
    bldr.setSlaveId(offer.getSlaveId());
    bldr.setName(taskRequest.getRequest().getId());


    // Do the port allocation dance.
    final int portCount = SingularityResourceRequest.findNumberResourceRequest(desiredTaskResources, SingularityResourceRequest.PORT_COUNT_RESOURCE_NAME, 0).intValue();

    int[] ports = new int[0];

    if (portCount > 0) {
      Resource portsResource = MesosUtils.getPortsResource(portCount, availableResources);
      bldr.addResources(portsResource);

      ports = MesosUtils.getPorts(portsResource, portCount);
    }

    final Optional<SingularityContainerInfo> containerInfo = taskRequest.getDeploy().getContainerInfo();
    if (containerInfo.isPresent()) {
      prepareContainerInfo(taskId, bldr, containerInfo.get(), ports);
    }

    if (taskRequest.getDeploy().getCustomExecutorCmd().isPresent()) {
      prepareCustomExecutor(bldr, taskId, taskRequest, ports);
    } else {
      prepareCommand(bldr, taskId, taskRequest, ports);
    }

    Map<String, Resource> resourceMap = ImmutableMap.copyOf(Maps.uniqueIndex(availableResources, getResourceNameFunction()));

    for (SingularityResourceRequest request : Collections2.filter(desiredTaskResources, SingularityResourceRequest.getFilterStandardResourcesFunction())) {
      if (!resourceMap.containsKey(request.getName())) {
        LOG.warn("Requested {} resource, but not available in availableResources!", request.getName());
      } else {
        Resource resource = resourceMap.get(request.getName());
        Object value = request.getValue();

        Resource.Builder builder = Resource.newBuilder()
            .setName(request.getName())
            .setType(resource.getType());

        switch (resource.getType()) {
          case SCALAR:
            if (value instanceof Number) {
              builder.setScalar(Scalar.newBuilder().setValue(((Number) value).doubleValue()));
              bldr.addResources(builder);
            } else {
              LOG.warn(format("Assigned value %s to a scalar, ignoring!", value));
            }
            break;
          case RANGES:
            if (value instanceof Number) {
              builder.setRanges(MesosUtils.buildRangeResource(resource.getRanges(), ((Number) value).intValue()));
              bldr.addResources(builder);
            } else {
              LOG.warn(format("Assigned value %s to a range", value));
            }
            break;
          case SET:
            builder.setSet(Value.Set.newBuilder().addAllItem(request.getValueAsStringSet()));
            bldr.addResources(builder);
            break;
          default:
            LOG.warn(format("Unknown resource type: %s", resource.getType()));
            break;
        }
      }
    }

    TaskInfo task = bldr.build();

    return new SingularityTask(taskRequest, taskId, offer, task);
  }

  private Function<Resource, String> getResourceNameFunction() {
    return new Function<Resource, String>() {
      @Override
      public String apply(@Nonnull Resource resource) {
        return resource.getName();
      }
    };
  }

  private void prepareEnvironment(final SingularityTaskRequest task, CommandInfo.Builder commandBuilder, final int[] ports) {
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

    for (int portNum = 0; portNum < ports.length; portNum++) {
      if (portNum == 0) {
        envBldr.addVariables(Variable.newBuilder()
            .setName("PORT")
            .setValue(Integer.toString(ports[portNum]))
            .build());
      }

      envBldr.addVariables(Variable.newBuilder()
          .setName(String.format("PORT%s", portNum))
          .setValue(Integer.toString(ports[portNum]))
          .build());
    }

    commandBuilder.setEnvironment(envBldr.build());
  }

  private Optional<DockerInfo.PortMapping> buildPortMapping(final SingularityDockerPortMapping singularityDockerPortMapping, int[] ports) {
    final int containerPort;
    switch (singularityDockerPortMapping.getContainerPortType()) {
      case LITERAL:
        containerPort = singularityDockerPortMapping.getContainerPort();
        break;
      case FROM_OFFER:
        containerPort = ports[singularityDockerPortMapping.getContainerPort()];
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
        hostPort = ports[singularityDockerPortMapping.getHostPort()];
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

  private void prepareContainerInfo(final SingularityTaskId taskId, final TaskInfo.Builder bldr, final SingularityContainerInfo containerInfo, final int[] ports) {
    ContainerInfo.Builder containerBuilder = ContainerInfo.newBuilder();
    containerBuilder.setType(containerInfo.getType());

    final Optional<SingularityDockerInfo> dockerInfo = containerInfo.getDocker();

    if (dockerInfo.isPresent()) {
      final DockerInfo.Builder dockerInfoBuilder = DockerInfo.newBuilder();
      containerBuilder.setDocker(dockerInfoBuilder.setImage(dockerInfo.get().getImage()));

      if (ports.length > 0 && !dockerInfo.get().getPortMappings().isEmpty()) {
        for (SingularityDockerPortMapping singularityDockerPortMapping : dockerInfo.get().getPortMappings()) {
          final Optional<DockerInfo.PortMapping> maybePortMapping = buildPortMapping(singularityDockerPortMapping, ports);

          if (maybePortMapping.isPresent()) {
            dockerInfoBuilder.addPortMappings(maybePortMapping.get());
          }
        }

        if (dockerInfo.get().getNetwork().isPresent()) {
          dockerInfoBuilder.setNetwork(dockerInfo.get().getNetwork().get());
        }
      }

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

    bldr.setContainer(containerBuilder);
  }

  private void prepareCustomExecutor(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, int[] ports) {
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

  private void prepareCommand(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final int[] ports) {
    CommandInfo.Builder commandBldr = CommandInfo.newBuilder();

    if (task.getDeploy().getCommand().isPresent()) {
      commandBldr.setValue(getCommand(taskId, task));
    }

    if (task.getDeploy().getArguments().isPresent()) {
      commandBldr.addAllArguments(task.getDeploy().getArguments().get());
    }

    if (task.getDeploy().getArguments().isPresent() ||
        // Hopefully temporary workaround for
        // http://www.mail-archive.com/user@mesos.apache.org/msg01449.html
        task.getDeploy().getContainerInfo().isPresent()) {
      commandBldr.setShell(false);
    }

    for (String uri : task.getDeploy().getUris().or(Collections.<String>emptyList())) {
      commandBldr.addUris(URI.newBuilder().setValue(uri).build());
    }

    prepareEnvironment(task, commandBldr, ports);

    bldr.setCommand(commandBldr);
  }

}
