package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.CommandInfo;
import org.apache.mesos.v1.Protos.CommandInfo.URI;
import org.apache.mesos.v1.Protos.ContainerInfo;
import org.apache.mesos.v1.Protos.ContainerInfo.DockerInfo;
import org.apache.mesos.v1.Protos.Environment;
import org.apache.mesos.v1.Protos.Environment.Variable;
import org.apache.mesos.v1.Protos.ExecutorID;
import org.apache.mesos.v1.Protos.ExecutorInfo;
import org.apache.mesos.v1.Protos.Label;
import org.apache.mesos.v1.Protos.Labels;
import org.apache.mesos.v1.Protos.Labels.Builder;
import org.apache.mesos.v1.Protos.Parameter;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskInfo;
import org.apache.mesos.v1.Protos.Volume;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.hubspot.deploy.ExecutorDataBuilder;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerNetworkType;
import com.hubspot.mesos.SingularityDockerParameter;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityMesosArtifact;
import com.hubspot.singularity.helpers.SingularityMesosTaskHolder;
import com.hubspot.mesos.SingularityMesosTaskLabel;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.SingularityS3UploaderFile;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskExecutorData;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ExecutorIdGenerator;

@Singleton
class SingularityMesosTaskBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosTaskBuilder.class);

  private final ObjectMapper objectMapper;
  private final ExecutorIdGenerator idGenerator;
  private final SingularityConfiguration configuration;
  private final MesosProtosUtils mesosProtosUtils;

  @Inject
  SingularityMesosTaskBuilder(ObjectMapper objectMapper, ExecutorIdGenerator idGenerator, SingularityConfiguration configuration, MesosProtosUtils mesosProtosUtils) {
    this.objectMapper = objectMapper;
    this.idGenerator = idGenerator;
    this.configuration = configuration;
    this.mesosProtosUtils = mesosProtosUtils;
  }

  public SingularityMesosTaskHolder buildTask(SingularityOfferHolder offerHolder, List<Resource> availableResources, SingularityTaskRequest taskRequest, Resources desiredTaskResources, Resources desiredExecutorResources) {
    final String sanitizedRackId = offerHolder.getSanitizedRackId();
    final String sanitizedHost = offerHolder.getSanitizedHost();

    final SingularityTaskId taskId = new SingularityTaskId(taskRequest.getPendingTask().getPendingTaskId().getRequestId(), taskRequest.getDeploy().getId(), System.currentTimeMillis(),
        taskRequest.getPendingTask().getPendingTaskId().getInstanceNo(), sanitizedHost, sanitizedRackId);

    final TaskInfo.Builder bldr = TaskInfo.newBuilder()
        .setTaskId(TaskID.newBuilder().setValue(taskId.toString()));

    Optional<long[]> ports = Optional.absent();
    Optional<Resource> portsResource = Optional.absent();

    final Optional<SingularityContainerInfo> containerInfo = taskRequest.getDeploy().getContainerInfo();
    if (desiredTaskResources.getNumPorts() > 0 || hasLiteralPortMapping(containerInfo)) {
      List<Long> requestedPorts = new ArrayList<>();
      if (hasLiteralPortMapping(containerInfo)) {
        requestedPorts.addAll(containerInfo.get().getDocker().get().getLiteralHostPorts());
      }
      portsResource = Optional.of(MesosUtils.getPortsResource(desiredTaskResources.getNumPorts(), availableResources, requestedPorts));
      ports = Optional.of(MesosUtils.getPorts(portsResource.get(), desiredTaskResources.getNumPorts()));
    }

    if (containerInfo.isPresent()) {
      prepareContainerInfo(offerHolder, taskId, bldr, containerInfo.get(), ports);
    }

    if (taskRequest.getDeploy().getCustomExecutorCmd().isPresent()) {
      prepareCustomExecutor(bldr, taskId, taskRequest, offerHolder, ports, desiredExecutorResources);
    } else {
      prepareCommand(bldr, taskId, taskRequest, offerHolder, ports);
    }

    if (portsResource.isPresent()) {
      bldr.addResources(portsResource.get());
    }


    Optional<String> requiredRole = taskRequest.getRequest().getRequiredRole();
    bldr.addResources(MesosUtils.getCpuResource(desiredTaskResources.getCpus(), requiredRole));
    bldr.addResources(MesosUtils.getMemoryResource(desiredTaskResources.getMemoryMb(), requiredRole));

    if (desiredTaskResources.getDiskMb() > 0) {
      bldr.addResources(MesosUtils.getDiskResource(desiredTaskResources.getDiskMb(), requiredRole));
    }

    bldr.setAgentId(offerHolder.getOffers().get(0).getAgentId());

    bldr.setName(taskRequest.getRequest().getId());

    final Builder labelsBuilder = Labels.newBuilder();
    // apply request-specific labels, if any
    if (taskRequest.getDeploy().getMesosLabels().isPresent() && !taskRequest.getDeploy().getMesosLabels().get().isEmpty()) {
      for (SingularityMesosTaskLabel label : taskRequest.getDeploy().getMesosLabels().get()) {
        org.apache.mesos.v1.Protos.Label.Builder labelBuilder = Label.newBuilder();
        labelBuilder.setKey(label.getKey());
        if ((label.getValue().isPresent())) {
          labelBuilder.setValue(label.getValue().get());
        }
        labelsBuilder.addLabels(labelBuilder.build());
      }
    }

    // apply task-specific labels, if any
    final int taskInstanceNo = taskRequest.getPendingTask().getPendingTaskId().getInstanceNo();
    if (taskRequest.getDeploy().getMesosTaskLabels().isPresent() && taskRequest.getDeploy().getMesosTaskLabels().get().containsKey(taskInstanceNo) && !taskRequest.getDeploy().getMesosTaskLabels().get().get(taskInstanceNo).isEmpty()) {
      for (SingularityMesosTaskLabel label : taskRequest.getDeploy().getMesosTaskLabels().get().get(taskInstanceNo)) {
        org.apache.mesos.v1.Protos.Label.Builder labelBuilder = Label.newBuilder();
        labelBuilder.setKey(label.getKey());
        if ((label.getValue().isPresent())) {
          labelBuilder.setValue(label.getValue().get());
        }
        labelsBuilder.addLabels(labelBuilder.build());
      }
    }
    bldr.setLabels(labelsBuilder);

    TaskInfo task = bldr.build();

    return new SingularityMesosTaskHolder(
        new SingularityTask(taskRequest,
            taskId,
            offerHolder.getOffers().stream().map((o) -> mesosProtosUtils.offerFromProtos(o)).collect(Collectors.toList()),
            mesosProtosUtils.taskFromProtos(task),
            Optional.of(offerHolder.getRackId())),
        task);
  }

  private boolean hasLiteralPortMapping(Optional<SingularityContainerInfo> maybeContainerInfo) {
    return maybeContainerInfo.isPresent() && maybeContainerInfo.get().getDocker().isPresent() && !maybeContainerInfo.get().getDocker().get().getLiteralHostPorts().isEmpty();
  }

  private void setEnv(Environment.Builder envBldr, String key, Object value) {
    if (value == null) {
      return;
    }
    envBldr.addVariables(Variable.newBuilder().setName(key).setValue(value.toString()));
  }

  private void prepareEnvironment(final SingularityTaskRequest task, SingularityTaskId taskId, CommandInfo.Builder commandBuilder, final SingularityOfferHolder offerHolder, final Optional<long[]> ports) {
    Environment.Builder envBldr = Environment.newBuilder();

    setEnv(envBldr, "INSTANCE_NO", task.getPendingTask().getPendingTaskId().getInstanceNo());
    setEnv(envBldr, "TASK_HOST", offerHolder.getHostname());

    setEnv(envBldr, "TASK_RACK_ID", offerHolder.getRackId());

    setEnv(envBldr, "TASK_REQUEST_ID", task.getPendingTask().getPendingTaskId().getRequestId());
    setEnv(envBldr, "TASK_DEPLOY_ID", taskId.getDeployId());
    setEnv(envBldr, "TASK_ID", taskId.getId());
    setEnv(envBldr, "ESTIMATED_INSTANCE_COUNT", task.getRequest().getInstancesSafe());

    if (task.getPendingTask().getUser().isPresent()) {
      setEnv(envBldr, "STARTED_BY_USER", task.getPendingTask().getUser().get());
    }

    for (Entry<String, String> envEntry : task.getDeploy().getEnv().or(Collections.<String, String>emptyMap()).entrySet()) {
      setEnv(envBldr, envEntry.getKey(), fillInTaskIdValues(envEntry.getValue(), offerHolder, taskId));
    }

    if (task.getDeploy().getTaskEnv().isPresent() && task.getDeploy().getTaskEnv().get().containsKey(taskId.getInstanceNo()) && !task.getDeploy().getTaskEnv().get().get(taskId.getInstanceNo()).isEmpty()) {
      for (Entry<String, String> envEntry : task.getDeploy().getTaskEnv().get().get(taskId.getInstanceNo()).entrySet()) {
        setEnv(envBldr, envEntry.getKey(), fillInTaskIdValues(envEntry.getValue(), offerHolder, taskId));
      }
    }

    if (ports.isPresent()) {
      for (int portNum = 0; portNum < ports.get().length; portNum++) {
        if (portNum == 0) {
          setEnv(envBldr, "PORT", ports.get()[portNum]);
        }

        setEnv(envBldr, String.format("PORT%s", portNum), ports.get()[portNum]);
      }
    }

    if (task.getPendingTask().getResources().isPresent()) {
      Resources override = task.getPendingTask().getResources().get();

      if (override.getCpus() != 0) {
        setEnv(envBldr, "DEPLOY_CPUS", ((long) override.getCpus()));
      }

      if (override.getMemoryMb() != 0) {
        setEnv(envBldr, "DEPLOY_MEM", ((long) override.getMemoryMb()));
      }

    }

    commandBuilder.setEnvironment(envBldr.build());
  }

  private Optional<DockerInfo.PortMapping> buildPortMapping(final SingularityDockerPortMapping singularityDockerPortMapping, final Optional<long[]> ports) {
    final int containerPort;
    switch (singularityDockerPortMapping.getContainerPortType()) {
      case LITERAL:
        containerPort = singularityDockerPortMapping.getContainerPort();
        break;
      case FROM_OFFER:
        containerPort = Ints.checkedCast(ports.get()[singularityDockerPortMapping.getContainerPort()]);
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
        hostPort = Ints.checkedCast(ports.get()[singularityDockerPortMapping.getHostPort()]);
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

  private String fillInTaskIdValues(String string, SingularityOfferHolder offerHolder, SingularityTaskId taskId) {
    if (!Strings.isNullOrEmpty(string)) {
      string = string.replace("${TASK_REQUEST_ID}", taskId.getRequestId())
          .replace("${TASK_DEPLOY_ID}", taskId.getDeployId())
          .replace("${TASK_STARTED_AT}", Long.toString(taskId.getStartedAt()))
          .replace("${TASK_INSTANCE_NO}", Integer.toString(taskId.getInstanceNo()))
          .replace("${TASK_HOST}", offerHolder.getHostname())
          .replace("${TASK_RACK_ID}", offerHolder.getRackId())
          .replace("${TASK_ID}", taskId.getId());
    }

    return string;
  }

  private void prepareContainerInfo(final SingularityOfferHolder offerHolder, final SingularityTaskId taskId, final TaskInfo.Builder bldr, final SingularityContainerInfo containerInfo, final Optional<long[]> ports) {
    ContainerInfo.Builder containerBuilder = ContainerInfo.newBuilder();
    containerBuilder.setType(ContainerInfo.Type.valueOf(containerInfo.getType().toString()));

    final Optional<SingularityDockerInfo> dockerInfo = containerInfo.getDocker();

    if (dockerInfo.isPresent()) {
      final DockerInfo.Builder dockerInfoBuilder = DockerInfo.newBuilder();
      dockerInfoBuilder.setImage(dockerInfo.get().getImage());

      if (dockerInfo.get().getNetwork().isPresent()) {
        dockerInfoBuilder.setNetwork(DockerInfo.Network.valueOf(dockerInfo.get().getNetwork().get().toString()));
      }

      final List<SingularityDockerPortMapping> portMappings = dockerInfo.get().getPortMappings();
      final boolean isBridged = SingularityDockerNetworkType.BRIDGE.equals(dockerInfo.get().getNetwork().orNull());

      if ((dockerInfo.get().hasAllLiteralHostPortMappings() || ports.isPresent()) && !portMappings.isEmpty()) {
        for (SingularityDockerPortMapping singularityDockerPortMapping : portMappings) {
          final Optional<DockerInfo.PortMapping> maybePortMapping = buildPortMapping(singularityDockerPortMapping, ports);

          if (maybePortMapping.isPresent()) {
            dockerInfoBuilder.addPortMappings(maybePortMapping.get());
          }
        }
      } else if (configuration.getNetworkConfiguration().isDefaultPortMapping() && isBridged && portMappings.isEmpty() && ports.isPresent()) {
        for (long longPort : ports.get()) {
          int port = Ints.checkedCast(longPort);
          dockerInfoBuilder.addPortMappings(DockerInfo.PortMapping.newBuilder()
              .setHostPort(port)
              .setContainerPort(port)
              .build());
        }
      }

      if (!dockerInfo.get().getDockerParameters().isEmpty()) {
        List<Parameter> parameters = new ArrayList<>();
        for (SingularityDockerParameter parameter : dockerInfo.get().getDockerParameters()) {
          parameters.add(Parameter.newBuilder().setKey(parameter.getKey()).setValue(parameter.getValue()).build());
        }
        dockerInfoBuilder.addAllParameters(parameters);
      }

      dockerInfoBuilder.setPrivileged(dockerInfo.get().isPrivileged());

      dockerInfoBuilder.setForcePullImage(dockerInfo.get().isForcePullImage());

      containerBuilder.setDocker(dockerInfoBuilder);
    }

    for (SingularityVolume volumeInfo : containerInfo.getVolumes().or(Collections.<SingularityVolume>emptyList())) {
      final Volume.Builder volumeBuilder = Volume.newBuilder();
      volumeBuilder.setContainerPath(fillInTaskIdValues(volumeInfo.getContainerPath(), offerHolder, taskId));
      if (volumeInfo.getHostPath().isPresent()) {
        volumeBuilder.setHostPath(fillInTaskIdValues(volumeInfo.getHostPath().get(), offerHolder, taskId));
      }
      if (volumeInfo.getMode().isPresent()) {
        volumeBuilder.setMode(Volume.Mode.valueOf(volumeInfo.getMode().get().toString()));
      } else {
        volumeBuilder.setMode(Volume.Mode.RO);
      }
      containerBuilder.addVolumes(volumeBuilder);
    }

    bldr.setContainer(containerBuilder);
  }

  private List<Resource> buildMesosResources(final Resources resources, Optional<String> role) {
    ImmutableList.Builder<Resource> builder = ImmutableList.builder();

    if (resources.getCpus() > 0) {
      builder.add(MesosUtils.getCpuResource(resources.getCpus(), role));
    }

    if (resources.getMemoryMb() > 0) {
      builder.add(MesosUtils.getMemoryResource(resources.getMemoryMb(), role));
    }

    if (resources.getDiskMb() > 0) {
      builder.add(MesosUtils.getDiskResource(resources.getDiskMb(), role));
    }

    return builder.build();
  }

  private void prepareCustomExecutor(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final SingularityOfferHolder offerHolder,
      final Optional<long[]> ports, final Resources desiredExecutorResources) {
    CommandInfo.Builder commandBuilder = CommandInfo.newBuilder().setValue(task.getDeploy().getCustomExecutorCmd().get());

    prepareEnvironment(task, taskId, commandBuilder, offerHolder, ports);

    if (task.getDeploy().getUser().isPresent()) {
      commandBuilder.setUser(task.getDeploy().getUser().get());
    }

    bldr.setExecutor(ExecutorInfo.newBuilder()
        .setCommand(commandBuilder.build())
        .setExecutorId(ExecutorID.newBuilder().setValue(task.getDeploy().getCustomExecutorId().or(idGenerator.getNextExecutorId())))
        .setSource(task.getDeploy().getCustomExecutorSource().or(taskId.getId())) // set source to taskId for use in statistics endpoint, TODO: remove
        .setLabels(Labels.newBuilder().addLabels(Label.newBuilder().setKey("taskId").setValue(taskId.getId())))
        .addAllResources(buildMesosResources(desiredExecutorResources, task.getRequest().getRequiredRole()))
        .build()
        );

    if (task.getDeploy().getExecutorData().isPresent()) {
      final ExecutorDataBuilder executorDataBldr = task.getDeploy().getExecutorData().get().toBuilder();

      String defaultS3Bucket = "";
      String s3UploaderKeyPattern = "";
      if (configuration.getS3ConfigurationOptional().isPresent()) {
        if (task.getRequest().getGroup().isPresent() && configuration.getS3ConfigurationOptional().get().getGroupOverrides().containsKey(task.getRequest().getGroup().get())) {
          defaultS3Bucket = configuration.getS3ConfigurationOptional().get().getGroupOverrides().get(task.getRequest().getGroup().get()).getS3Bucket();
          LOG.trace("Setting defaultS3Bucket to {} for task {} executorData", defaultS3Bucket, taskId.getId());
        } else {
          defaultS3Bucket = configuration.getS3ConfigurationOptional().get().getS3Bucket();
        }
        s3UploaderKeyPattern = configuration.getS3ConfigurationOptional().get().getS3KeyFormat();
      }

      if (task.getPendingTask().getCmdLineArgsList().isPresent() && !task.getPendingTask().getCmdLineArgsList().get().isEmpty()) {
        LOG.trace("Adding cmd line args {} to task {} executorData", task.getPendingTask().getCmdLineArgsList(), taskId.getId());

        final ImmutableList.Builder<String> extraCmdLineArgsBuilder = ImmutableList.builder();
        if (executorDataBldr.getExtraCmdLineArgs() != null && !executorDataBldr.getExtraCmdLineArgs().isEmpty()) {
          extraCmdLineArgsBuilder.addAll(executorDataBldr.getExtraCmdLineArgs());
        }
        extraCmdLineArgsBuilder.addAll(task.getPendingTask().getCmdLineArgsList().get());
        executorDataBldr.setExtraCmdLineArgs(extraCmdLineArgsBuilder.build());
      }

      List<SingularityS3UploaderFile> uploaderAdditionalFiles = configuration.getS3ConfigurationOptional().isPresent() ? configuration.getS3ConfigurationOptional().get().getS3UploaderAdditionalFiles() : Collections.<SingularityS3UploaderFile>emptyList();
      Optional<String> maybeS3StorageClass = configuration.getS3ConfigurationOptional().isPresent() ? configuration.getS3ConfigurationOptional().get().getS3StorageClass() : Optional.<String>absent();
      Optional<Long> maybeApplyAfterBytes = configuration.getS3ConfigurationOptional().isPresent() ? configuration.getS3ConfigurationOptional().get().getApplyS3StorageClassAfterBytes() : Optional.<Long>absent();

      final SingularityTaskExecutorData executorData = new SingularityTaskExecutorData(executorDataBldr.build(), uploaderAdditionalFiles, defaultS3Bucket, s3UploaderKeyPattern,
          configuration.getCustomExecutorConfiguration().getServiceLog(), configuration.getCustomExecutorConfiguration().getServiceFinishedTailLog(), task.getRequest().getGroup(),
          maybeS3StorageClass, maybeApplyAfterBytes);

      try {
        bldr.setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(executorData)));
      } catch (JsonProcessingException e) {
        LOG.warn("Unable to process executor data {} for task {} as json (trying as string)", executorData, taskId.getId(), e);

        bldr.setData(ByteString.copyFromUtf8(executorData.toString()));
      }
    } else if (task.getDeploy().getCommand().isPresent()) {
      bldr.setData(ByteString.copyFromUtf8(task.getDeploy().getCommand().get()));
    }
  }


  private void prepareCommand(final TaskInfo.Builder bldr, final SingularityTaskId taskId, final SingularityTaskRequest task, final SingularityOfferHolder offerHolder, final Optional<long[]> ports) {
    CommandInfo.Builder commandBldr = CommandInfo.newBuilder();

    if (task.getDeploy().getUser().isPresent()) {
      commandBldr.setUser(task.getDeploy().getUser().get());
    }

    if (task.getDeploy().getCommand().isPresent()) {
      commandBldr.setValue(task.getDeploy().getCommand().get());
    }

    if (task.getDeploy().getArguments().isPresent()) {
      commandBldr.addAllArguments(task.getDeploy().getArguments().get());
    }

    if (task.getPendingTask().getCmdLineArgsList().isPresent()) {
      commandBldr.addAllArguments(task.getPendingTask().getCmdLineArgsList().get());
    }

    if (task.getDeploy().getShell().isPresent()){
      commandBldr.setShell(task.getDeploy().getShell().get());
    } else if ((task.getDeploy().getArguments().isPresent() && !task.getDeploy().getArguments().get().isEmpty()) ||
        // Hopefully temporary workaround for
        // http://www.mail-archive.com/user@mesos.apache.org/msg01449.html
        task.getDeploy().getContainerInfo().isPresent() ||
        (task.getPendingTask().getCmdLineArgsList().isPresent() && !task.getPendingTask().getCmdLineArgsList().get().isEmpty())) {
      commandBldr.setShell(false);
    }

    for (SingularityMesosArtifact artifact : task.getDeploy().getUris().or(Collections.<SingularityMesosArtifact> emptyList())) {
      commandBldr.addUris(URI.newBuilder()
          .setValue(artifact.getUri())
          .setCache(artifact.isCache())
          .setExecutable(artifact.isExecutable())
          .setExtract(artifact.isExtract())
          .build());
    }

    prepareEnvironment(task, taskId, commandBldr, offerHolder, ports);

    bldr.setCommand(commandBldr);
  }

}
