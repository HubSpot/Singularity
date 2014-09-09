package com.hubspot.singularity.mesos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.SingularityContainerInfo;
import com.hubspot.mesos.SingularityDockerInfo;
import com.hubspot.mesos.SingularityVolume;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.data.ExecutorIdGenerator;

import org.apache.mesos.Protos.ContainerInfo.Type;
import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.Volume.Mode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SingularityMesosTaskBuilderTest {
  private SingularityMesosTaskBuilder builder;
  private Resources resources;
  private Offer offer;
  private SingularityPendingTask pendingTask;

  @Before
  public void createMocks() {
    pendingTask = new SingularityPendingTask(new SingularityPendingTaskId("test", "1", 0, 0, PendingType.IMMEDIATE), Optional.<String>absent());

    final SingularitySlaveAndRackManager rackManager = mock(SingularitySlaveAndRackManager.class);
    final ExecutorIdGenerator idGenerator = mock(ExecutorIdGenerator.class);

    when(idGenerator.getNextExecutorId()).then(new CreateFakeId());

    builder = new SingularityMesosTaskBuilder(new ObjectMapper(), rackManager, idGenerator);

    resources = new Resources(1, 1, 0);
    offer = Offer.newBuilder()
        .setSlaveId(SlaveID.newBuilder().setValue("1"))
        .setId(OfferID.newBuilder().setValue("1"))
        .setFrameworkId(FrameworkID.newBuilder().setValue("1"))
        .setHostname("test")
        .build();
  }

  @Test
  public void testShellCommand() {
    final SingularityRequest request = new SingularityRequestBuilder("test").build();
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setCommand(Optional.of("/bin/echo hi"))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offer, null, taskRequest, resources);

    assertEquals("/bin/echo hi", task.getMesosTask().getCommand().getValue());
    assertEquals(0, task.getMesosTask().getCommand().getArgumentsCount());
    assertTrue(task.getMesosTask().getCommand().getShell());
  }

  @Test
  public void testArgumentCommand() {
    final SingularityRequest request = new SingularityRequestBuilder("test").build();
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setCommand(Optional.of("/bin/echo"))
        .setArguments(Optional.of(Collections.singletonList("wat")))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offer, null, taskRequest, resources);

    assertEquals("/bin/echo", task.getMesosTask().getCommand().getValue());
    assertEquals(1, task.getMesosTask().getCommand().getArgumentsCount());
    assertEquals("wat", task.getMesosTask().getCommand().getArguments(0));
    assertFalse(task.getMesosTask().getCommand().getShell());
  }

  @Test
  public void testDockerTask() {
    final SingularityRequest request = new SingularityRequestBuilder("test").build();
    final SingularityContainerInfo containerInfo = new SingularityContainerInfo(
        Type.DOCKER,
        Optional.of(Collections.singletonList(new SingularityVolume("/container", Optional.of("/host"), Mode.RW))),
        Optional.of(new SingularityDockerInfo("docker-image")));
    final SingularityDeploy deploy = new SingularityDeployBuilder("test", "1")
        .setContainerInfo(Optional.of(containerInfo))
        .setCommand(Optional.of("/bin/echo"))
        .setArguments(Optional.of(Collections.singletonList("wat")))
        .build();
    final SingularityTaskRequest taskRequest = new SingularityTaskRequest(request, deploy, pendingTask);
    final SingularityTask task = builder.buildTask(offer, null, taskRequest, resources);

    assertEquals("/bin/echo", task.getMesosTask().getCommand().getValue());
    assertEquals(1, task.getMesosTask().getCommand().getArgumentsCount());
    assertEquals("wat", task.getMesosTask().getCommand().getArguments(0));
    assertFalse(task.getMesosTask().getCommand().getShell());

    assertEquals(Type.DOCKER, task.getMesosTask().getContainer().getType());
    assertEquals("docker-image", task.getMesosTask().getContainer().getDocker().getImage());
    assertEquals("/container", task.getMesosTask().getContainer().getVolumes(0).getContainerPath());
    assertEquals("/host", task.getMesosTask().getContainer().getVolumes(0).getHostPath());
    assertEquals(Mode.RW, task.getMesosTask().getContainer().getVolumes(0).getMode());
  }

  private static class CreateFakeId implements Answer<String> {

    private final AtomicLong string = new AtomicLong();

    @Override
    public String answer(InvocationOnMock invocation) throws Throwable {
      return String.valueOf(string.incrementAndGet());
    }
  }
}
