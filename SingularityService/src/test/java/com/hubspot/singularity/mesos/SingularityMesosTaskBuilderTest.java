package com.hubspot.singularity.mesos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.ContainerInfo.DockerInfo.PortMapping;
import org.apache.mesos.v1.Protos.ContainerInfo.Type;
import org.apache.mesos.v1.Protos.Environment.Variable;
import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.Parameter;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Volume.Mode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.hubspot.mesos.MesosUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityContainerType;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityDockerNetworkType;
import com.hubspot.mesos.SingularityDockerPortMapping;
import com.hubspot.mesos.SingularityDockerVolumeMode;
import com.hubspot.mesos.SingularityPortMappingType;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.config.NetworkConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.ExecutorIdGenerator;

public class SingularityMesosTaskBuilderTest {
  private final SingularityConfiguration configuration = new SingularityConfiguration();
  private SingularityMesosTaskBuilder builder;
  private Resources taskResources;
  private Resources executorResources;
  private Offer offer;
  private SingularityOfferHolder offerHolder;
  private SingularityPendingTask pendingTask;

  private final String user = "testUser";

  @Before
  public void createMocks() {
    pendingTask = new SingularityPendingTask(new SingularityPendingTaskId("test", "1", 0, 1, PendingType.IMMEDIATE, 0), Optional.<List<String>> absent(),
        Optional.of(user), Optional.<String> absent(), Optional.<Boolean> absent(), Optional.<String> absent(), Optional.<Resources>absent(), Optional.<String>absent());

    final SingularitySlaveAndRackHelper slaveAndRackHelper = mock(SingularitySlaveAndRackHelper.class);
    final ExecutorIdGenerator idGenerator = mock(ExecutorIdGenerator.class);

    when(idGenerator.getNextExecutorId()).then(new CreateFakeId());

    builder = new SingularityMesosTaskBuilder(new ObjectMapper(), idGenerator, configuration);

    taskResources = new Resources(1, 1, 0, 0);
    executorResources = new Resources(0.1, 1, 0, 0);

    when(slaveAndRackHelper.getRackId(offer)).thenReturn(Optional.absent());
    when(slaveAndRackHelper.getMaybeTruncatedHost(offer)).thenReturn("host");
    when(slaveAndRackHelper.getRackIdOrDefault(offer)).thenReturn("DEFAULT");

    offer = Offer.newBuilder()
        .setAgentId(AgentID.newBuilder().setValue("1"))
        .setId(OfferID.newBuilder().setValue("1"))
        .setFrameworkId(FrameworkID.newBuilder().setValue("1"))
        .setHostname("test")
        .build();
    offerHolder = new SingularityOfferHolder(
        Collections.singletonList(offer),
        1,
        "DEFAULT",
        offer.getAgentId().getValue(),
        offer.getHostname(),
        Collections.emptyMap(),
        Collections.emptyMap());
  }

  @Test
  public void testShellCommand() {
    final SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER).build();
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setCommand(Optional.of("/bin/echo hi"))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offerHolder, null, taskRequest, taskResources, executorResources);

    assertEquals("/bin/echo hi", task.getMesosTask().getCommand().getValue());
    assertEquals(0, task.getMesosTask().getCommand().getArgumentsCount());
    assertTrue(task.getMesosTask().getCommand().getShell());
  }

  @Test
  public void testJobUserPassedAsEnvironmentVariable() {
    final SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER)
        .build();
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setCommand(Optional.of("/bin/echo hi"))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offerHolder, null, taskRequest, taskResources, executorResources);

    List<Variable> environmentVariables = task.getMesosTask()
        .getCommand()
        .getEnvironment()
        .getVariablesList();

    boolean success = false;
    for (Variable environmentVariable : environmentVariables) {
      success = success || (environmentVariable.getName().equals("STARTED_BY_USER") && environmentVariable.getValue().equals(user));
    }

    assertTrue("Expected env variable STARTED_BY_USER to be set to " + user, success);
  }

  @Test
  public void testArgumentCommand() {
    final SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER).build();
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setCommand(Optional.of("/bin/echo"))
        .setArguments(Optional.of(Collections.singletonList("wat")))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offerHolder, null, taskRequest, taskResources, executorResources);

    assertEquals("/bin/echo", task.getMesosTask().getCommand().getValue());
    assertEquals(1, task.getMesosTask().getCommand().getArgumentsCount());
    assertEquals("wat", task.getMesosTask().getCommand().getArguments(0));
    assertFalse(task.getMesosTask().getCommand().getShell());
  }

  @Test
  public void testDockerTask() {
    taskResources = new Resources(1, 1, 1, 0);

    final Protos.Resource portsResource = Protos.Resource.newBuilder()
        .setName("ports")
        .setType(Protos.Value.Type.RANGES)
        .setRanges(Protos.Value.Ranges.newBuilder()
            .addRange(Protos.Value.Range.newBuilder()
                .setBegin(31000)
                .setEnd(31000).build()).build()).build();

    final SingularityDockerPortMapping literalMapping = new SingularityDockerPortMapping(Optional.<SingularityPortMappingType>absent(), 80, Optional.of(SingularityPortMappingType.LITERAL), 8080, Optional.<String>absent());
    final SingularityDockerPortMapping offerMapping = new SingularityDockerPortMapping(Optional.<SingularityPortMappingType>absent(), 81, Optional.of(SingularityPortMappingType.FROM_OFFER), 0, Optional.of("udp"));

    final SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER).build();
    final SingularityContainerInfo containerInfo = new SingularityContainerInfo(
        SingularityContainerType.DOCKER,
        Optional.of(Arrays.asList(
            new SingularityVolume("/container", Optional.of("/host"), SingularityDockerVolumeMode.RW),
            new SingularityVolume("/container/${TASK_REQUEST_ID}/${TASK_DEPLOY_ID}", Optional.of("/host/${TASK_ID}"), SingularityDockerVolumeMode.RO))),
        Optional.of(new SingularityDockerInfo("docker-image", true, SingularityDockerNetworkType.BRIDGE, Optional.of(Arrays.asList(literalMapping, offerMapping)), Optional.of(false), Optional.<Map<String, String>>of(
          ImmutableMap.of("env", "var=value")), Optional.absent())));
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setContainerInfo(Optional.of(containerInfo))
        .setCommand(Optional.of("/bin/echo"))
        .setArguments(Optional.of(Collections.singletonList("wat")))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offerHolder, Collections.singletonList(portsResource), taskRequest, taskResources, executorResources);

    assertEquals("/bin/echo", task.getMesosTask().getCommand().getValue());
    assertEquals(1, task.getMesosTask().getCommand().getArgumentsCount());
    assertEquals("wat", task.getMesosTask().getCommand().getArguments(0));
    assertFalse(task.getMesosTask().getCommand().getShell());

    assertEquals(Type.DOCKER, task.getMesosTask().getContainer().getType());
    assertEquals("docker-image", task.getMesosTask().getContainer().getDocker().getImage());
    assertTrue(task.getMesosTask().getContainer().getDocker().getPrivileged());

    assertEquals("/container", task.getMesosTask().getContainer().getVolumes(0).getContainerPath());
    assertEquals("/host", task.getMesosTask().getContainer().getVolumes(0).getHostPath());
    assertEquals(Mode.RW, task.getMesosTask().getContainer().getVolumes(0).getMode());

    Parameter envParameter = Parameter.newBuilder().setKey("env").setValue("var=value").build();
    assertTrue(task.getMesosTask().getContainer().getDocker().getParametersList().contains(envParameter));

    assertEquals(String.format("/container/%s/%s", task.getTaskRequest().getDeploy().getRequestId(), task.getTaskRequest().getDeploy().getId()), task.getMesosTask().getContainer().getVolumes(1).getContainerPath());
    assertEquals(String.format("/host/%s", task.getMesosTask().getTaskId().getValue()), task.getMesosTask().getContainer().getVolumes(1).getHostPath());
    assertEquals(Mode.RO, task.getMesosTask().getContainer().getVolumes(1).getMode());

    assertEquals(80, task.getMesosTask().getContainer().getDocker().getPortMappings(0).getContainerPort());
    assertEquals(8080, task.getMesosTask().getContainer().getDocker().getPortMappings(0).getHostPort());
    assertEquals("tcp", task.getMesosTask().getContainer().getDocker().getPortMappings(0).getProtocol());
    assertTrue(MesosUtils.getAllPorts(task.getMesosTask().getResources()).contains(8080L));

    assertEquals(81, task.getMesosTask().getContainer().getDocker().getPortMappings(1).getContainerPort());
    assertEquals(31000, task.getMesosTask().getContainer().getDocker().getPortMappings(1).getHostPort());
    assertEquals("udp", task.getMesosTask().getContainer().getDocker().getPortMappings(1).getProtocol());

    assertEquals(Protos.ContainerInfo.DockerInfo.Network.BRIDGE, task.getMesosTask().getContainer().getDocker().getNetwork());
  }

  @Test
  public void testDockerMinimalNetworking() {
    taskResources = new Resources(1, 1, 0, 0);

    final SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER).build();
    final SingularityContainerInfo containerInfo = new SingularityContainerInfo(
        SingularityContainerType.DOCKER,
        Optional.absent(),
        Optional.of(new SingularityDockerInfo("docker-image", true, SingularityDockerNetworkType.NONE,
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent())));
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setContainerInfo(Optional.of(containerInfo))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offerHolder, Collections.emptyList(), taskRequest, taskResources, executorResources);

    assertEquals(Type.DOCKER, task.getMesosTask().getContainer().getType());
    assertEquals(Protos.ContainerInfo.DockerInfo.Network.NONE, task.getMesosTask().getContainer().getDocker().getNetwork());
  }

  @Test
  public void testAutomaticPortMapping() {
    NetworkConfiguration netConf = new NetworkConfiguration();
    netConf.setDefaultPortMapping(true);
    configuration.setNetworkConfiguration(netConf);

    taskResources = new Resources(1, 1, 2);

    final SingularityRequest request = new SingularityRequestBuilder("test", RequestType.WORKER).build();
    final SingularityContainerInfo containerInfo = new SingularityContainerInfo(
        SingularityContainerType.DOCKER,
        Optional.absent(),
        Optional.of(new SingularityDockerInfo("docker-image", false, SingularityDockerNetworkType.BRIDGE,
            Optional.absent(),
            Optional.absent(),
            Optional.absent(),
            Optional.absent())));
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setContainerInfo(Optional.of(containerInfo))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offerHolder, Collections.singletonList(MesosUtils.getPortRangeResource(31010, 31011)), taskRequest, taskResources, executorResources);

    assertEquals(Type.DOCKER, task.getMesosTask().getContainer().getType());
    assertEquals(Protos.ContainerInfo.DockerInfo.Network.BRIDGE, task.getMesosTask().getContainer().getDocker().getNetwork());

    List<PortMapping> portMappings = task.getMesosTask().getContainer().getDocker().getPortMappingsList();
    assertEquals(2, portMappings.size());

    assertEquals(31010, portMappings.get(0).getHostPort());
    assertEquals(31010, portMappings.get(0).getContainerPort());

    assertEquals(31011, portMappings.get(1).getHostPort());
    assertEquals(31011, portMappings.get(1).getContainerPort());
  }

  private static class CreateFakeId implements Answer<String> {

    private final AtomicLong string = new AtomicLong();

    @Override
    public String answer(InvocationOnMock invocation) throws Throwable {
      return String.valueOf(string.incrementAndGet());
    }
  }
}
