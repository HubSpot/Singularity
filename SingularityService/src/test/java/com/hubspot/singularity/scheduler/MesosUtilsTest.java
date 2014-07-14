package com.hubspot.singularity.scheduler;

import org.apache.mesos.Protos.FrameworkID;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.SlaveID;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Ranges;
import org.apache.mesos.Protos.Value.Type;
import org.junit.Assert;
import org.junit.Test;

import com.hubspot.mesos.MesosUtils;

public class MesosUtilsTest {

  private void assertFound(int numPorts, Resource resource) {
    int portsFound = 0;
    for (Range r : resource.getRanges().getRangeList()) {
      portsFound += (r.getEnd() - r.getBegin()) + 1;
    }
    Assert.assertEquals(numPorts, portsFound);
  }
  
  private void test(int numPorts, String... ranges) {
    Resource resource = MesosUtils.getPortsResource(numPorts, buildOffer(ranges));
    
    assertFound(numPorts, resource);
  }
  
  @Test
  public void testRangeSelection() {
    test(4, "23:24", "26:26", "28:28", "29:29", "31:32");
    test(2, "22:23");
    test(3, "22:22", "23:23", "24:24", "25:25");
    test(10, "100:10000");
    test(23, "90:100", "9100:9100", "185:1000");
    
  }

  private Offer buildOffer(String... ranges) {
    Offer.Builder offer = Offer.newBuilder()
        .setId(OfferID.newBuilder().setValue("offerid").build())
        .setFrameworkId(FrameworkID.newBuilder().setValue("frameworkid").build())
        .setHostname("hostname")
        .setSlaveId(SlaveID.newBuilder().setValue("slaveid").build());
    
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
    
    offer.addResources(resources);
    
    return offer.build();
  }
  
  
}
