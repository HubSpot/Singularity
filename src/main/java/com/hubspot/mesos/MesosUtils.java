package com.hubspot.mesos;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.Value;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Ranges;

public class MesosUtils {

  public static final String CPUS = "cpus";
  public static final String MEMORY = "mem";
  public static final String PORTS = "ports";

  private static int getScalar(Resource r) {
    return (int) r.getScalar().getValue();
  }

  private static int getScalar(Offer offer, String name) {
    for (Resource r : offer.getResourcesList()) {
      if (r.hasName() && r.getName().equals(name) && r.hasScalar()) {
        return getScalar(r);
      }
    }

    return 0;
  }
  
  private static Ranges getRanges(Offer offer, String name) {
    for (Resource r: offer.getResourcesList()) {
      if (r.hasName() && r.getName().equals(name) && r.hasRanges()) {
        return r.getRanges();
      }
    }
    
    return null;
  }
  
  private static int getNumRanges(Offer offer, String name) {
    int totalRanges = 0;
    
    Ranges ranges = getRanges(offer, name);
    
    if (ranges == null) {
      return 0;
    }
    
    for (Range range : ranges.getRangeList()) {
      long num = range.getEnd() - range.getEnd();
      totalRanges += num;
    }
    
    return totalRanges;
  }
  
  public static Resource getCpuResource(int cpus) {
    return newScalar(CPUS, cpus);
  }

  public static Resource getMemoryResource(int memory) {
    return newScalar(MEMORY, memory);
  }
  
  public static long[] getPorts(Resource portsResource, int numPorts) {
    long[] ports = new long[numPorts];
    int idx = 0;
    
    for (Range r : portsResource.getRanges().getRangeList()) {
      for (long port = r.getBegin(); port < r.getEnd(); port++) {
        ports[idx++] = port;
      }
    }
    
    return ports;
  }
  
  public static Resource getPortsResource(int numPorts, Offer offer) {
    Ranges ranges = getRanges(offer, PORTS);
    
    if (ranges == null) {
      throw new IllegalStateException(String.format("Ports %s should have existed in offer %s", PORTS, offer));
    }
    
    Ranges.Builder rangesBldr = Ranges.newBuilder();
    
    int portsSoFar = 0;
    
    for (Range range : ranges.getRangeList()) {
      long rangeEnd = Math.min(portsSoFar + range.getBegin(), range.getEnd());
      
      long numPortsInRange = rangeEnd - range.getBegin();
    
      rangesBldr.addRange(Range.newBuilder()
          .setBegin(range.getBegin())
          .setEnd(rangeEnd));
      
      portsSoFar += numPortsInRange;
      
      if (portsSoFar == numPorts) {
        break;
      }
    }
    
    return Resource.newBuilder()
        .setName(PORTS)
        .setRanges(rangesBldr)
        .build();
  }

  private static Resource newScalar(String name, int value) {
    return Resource.newBuilder().setName(name).setType(Value.Type.SCALAR).setScalar(Value.Scalar.newBuilder().setValue(value).build()).build();
  }

  public static int getNumCpus(Offer offer) {
    return getScalar(offer, CPUS);
  }

  public static int getMemory(Offer offer) {
    return getScalar(offer, MEMORY);
  }

  public static int getNumPorts(Offer offer) {
    return getNumRanges(offer, PORTS);
  }
  
  public static boolean doesOfferMatchResources(Resources resources, Offer offer) {
    int numCpus = getNumCpus(offer);

    if (numCpus < resources.getCpus()) {
      return false;
    }

    int memory = getMemory(offer);

    if (memory < resources.getMemoryMb()) {
      return false;
    }
    
    int numPorts = getNumPorts(offer);
    
    if (numPorts < resources.getNumPorts()) {
      return false;
    }
    
    return true;
  }
  
  public static boolean isTaskDone(TaskState state) {
    return state == TaskState.TASK_FAILED || state == TaskState.TASK_LOST || state == TaskState.TASK_KILLED || state == TaskState.TASK_FINISHED;
  }
  
  
}
