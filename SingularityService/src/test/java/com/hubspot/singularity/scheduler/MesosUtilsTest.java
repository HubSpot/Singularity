package com.hubspot.singularity.scheduler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.mesos.v1.Protos.FrameworkID;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.AgentID;
import org.apache.mesos.v1.Protos.Value.Range;
import org.apache.mesos.v1.Protos.Value.Ranges;
import org.apache.mesos.v1.Protos.Value.Scalar;
import org.apache.mesos.v1.Protos.Value.Type;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;

public class MesosUtilsTest {

  private void test(int numPorts, String... ranges) {
    Resource resource = MesosUtils.getPortsResource(numPorts, buildOffer(ranges));

    Assert.assertEquals(numPorts, MesosUtils.getNumPorts(Collections.singletonList(resource)));
  }

  @Test
  public void testResourceAddition() {
    List<List<Resource>> toAdd = ImmutableList.of(
        ImmutableList.of(
            Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(1)).build(),
            Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(1024)).build()
        ),
        ImmutableList.of(
            Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(2)).build(),
            Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(1024)).build()
        ),
        ImmutableList.of(
            Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(3)).build(),
            Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(1024)).build()
        )
    );
    List<Resource> combined = MesosUtils.combineResources(toAdd);

    Assert.assertEquals(6, MesosUtils.getNumCpus(combined, Optional.absent()), 0.1);
    Assert.assertEquals(3072, MesosUtils.getMemory(combined, Optional.absent()), 0.1);
  }

  @Test
  public void testTaskOrdering() {
    final SingularityTaskId taskId = new SingularityTaskId("r", "d", System.currentTimeMillis(), 1, "h", "r");
    final Optional<String> msg = Optional.absent();

    SingularityTaskHistoryUpdate update1 = new SingularityTaskHistoryUpdate(taskId, 1L, ExtendedTaskState.TASK_LAUNCHED, msg, Optional.<String>absent());
    SingularityTaskHistoryUpdate update2 = new SingularityTaskHistoryUpdate(taskId, 2L, ExtendedTaskState.TASK_RUNNING, msg, Optional.<String>absent());
    SingularityTaskHistoryUpdate update3 = new SingularityTaskHistoryUpdate(taskId, 2L, ExtendedTaskState.TASK_FAILED, msg, Optional.<String>absent());

    List<SingularityTaskHistoryUpdate> list = Arrays.asList(update2, update1, update3);

    Collections.sort(list);

    Assert.assertTrue(list.get(0).getTaskState() == ExtendedTaskState.TASK_LAUNCHED);
    Assert.assertTrue(list.get(1).getTaskState() == ExtendedTaskState.TASK_RUNNING);
    Assert.assertTrue(list.get(2).getTaskState() == ExtendedTaskState.TASK_FAILED);
  }

  @Test
  public void testSubtractResources() {
    Assert.assertEquals(createResources(3, 60, "23:23", "100:175", "771:1000"),
        MesosUtils.subtractResources(createResources(5, 100, "23:23", "100:1000"), createResources(2, 40, "176:770")));

    List<Resource> subtracted = createResources(100, 1000, "1:100", "101:1000");

    subtracted = MesosUtils.subtractResources(subtracted, createResources(5, 100, "23:74", "101:120", "125:130", "750:756"));

    Assert.assertEquals(createResources(95, 900, "1:22", "75:100", "121:124", "131:749", "757:1000"), subtracted);

    subtracted = MesosUtils.subtractResources(subtracted, createResources(20, 20, "75:90", "121:121", "757:1000"));

    Assert.assertEquals(createResources(75, 880, "1:22", "91:100", "122:124", "131:749"), subtracted);
  }


  private List<Resource> createResources(int cpus, int memory, String... ranges) {
    List<Resource> resources = Lists.newArrayList();

    if (cpus > 0) {
      resources.add(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.CPUS).setScalar(Scalar.newBuilder().setValue(cpus).build()).build());
    }

    if (memory > 0) {
      resources.add(Resource.newBuilder().setType(Type.SCALAR).setName(MesosUtils.MEMORY).setScalar(Scalar.newBuilder().setValue(memory).build()).build());
    }

    if (ranges.length > 0) {
      resources.add(buildPortRanges(ranges));
    }

    return resources;
  }

  @Test
  public void testRangeSelection() {
    test(4, "23:24", "26:26", "28:28", "29:29", "31:32");
    test(2, "22:23");
    test(3, "22:22", "23:23", "24:24", "25:25");
    test(10, "100:10000");
    test(23, "90:100", "9100:9100", "185:1000");

  }

  @Test
  public void testLiteralHostPortSelection() {
    String[] rangesNotOverlappingRequestedPorts = {"23:24", "25:25", "31:32", "50:51"};
    int numPorts = 1;
    List<Long> requestedPorts = Arrays.asList(50L, 51L);
    Resource resource = MesosUtils.getPortsResource(numPorts, buildOffer(rangesNotOverlappingRequestedPorts).getResourcesList(), requestedPorts);
    Assert.assertTrue(MesosUtils.getAllPorts(Collections.singletonList(resource)).containsAll(requestedPorts));
    Assert.assertEquals(numPorts + requestedPorts.size(), MesosUtils.getNumPorts(Collections.singletonList(resource)));

    String[] rangesOverlappingRequestPorts = {"23:28"};
    numPorts = 4;
    requestedPorts = Arrays.asList(25L, 27L);
    resource = MesosUtils.getPortsResource(numPorts, buildOffer(rangesOverlappingRequestPorts).getResourcesList(), requestedPorts);
    Assert.assertTrue(MesosUtils.getAllPorts(Collections.singletonList(resource)).containsAll(requestedPorts));
    Assert.assertEquals(numPorts + requestedPorts.size(), MesosUtils.getNumPorts(Collections.singletonList(resource)));
  }

  @Test
  public void testGetZeroPortsFromResource() {
    String[] rangesOverlappingRequestPorts = {"23:28"};
    int numPorts = 0;
    List<Long> requestedPorts = Arrays.asList(25L, 27L);
    Resource resource = MesosUtils.getPortsResource(numPorts, buildOffer(rangesOverlappingRequestPorts).getResourcesList(), requestedPorts);
    Assert.assertEquals(0, MesosUtils.getPorts(resource, numPorts).length);
  }

  public static Resource buildPortRanges(String... ranges) {
    Resource.Builder resources = Resource.newBuilder()
        .setType(Type.RANGES)
        .setName(MesosUtils.PORTS);

    Ranges.Builder rangesBuilder = Ranges.newBuilder();

    for (String range : ranges) {
      String[] split = range.split("\\:");

      rangesBuilder.addRange(
          Range.newBuilder()
          .setBegin(Long.parseLong(split[0]))
          .setEnd(Long.parseLong(split[1])));
    }

    resources.setRanges(rangesBuilder);

    return resources.build();
  }

  private Offer buildOffer(String... ranges) {
    Offer.Builder offer = Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offerid").build())
        .setFrameworkId(FrameworkID.newBuilder().setValue("frameworkid").build())
        .setHostname("hostname")
        .setAgentId(AgentID.newBuilder().setValue("slaveid").build());

    offer.addResources(buildPortRanges(ranges));

    return offer.build();
  }


}
