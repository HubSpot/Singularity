package com.hubspot.mesos;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.mesos.Protos.MasterInfo;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskInfo;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.Value;
import org.apache.mesos.Protos.Value.Range;
import org.apache.mesos.Protos.Value.Ranges;
import org.apache.mesos.Protos.Value.Type;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;

public class MesosUtils {

  public static final String CPUS = "cpus";
  public static final String MEMORY = "mem";
  public static final String PORTS = "ports";

  private MesosUtils() { }

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

  private static Ranges getRanges(TaskInfo taskInfo, String name) {
    for (Resource r: taskInfo.getResourcesList()) {
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
      long num = range.getEnd() - range.getBegin();
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

  public static List<Long> getAllPorts(TaskInfo taskInfo) {
    final List<Long> ports = Lists.newArrayList();

    final Ranges ranges = getRanges(taskInfo, PORTS);

    if (ranges != null) {
      for (Range range : ranges.getRangeList()) {
        for (long port = range.getBegin(); port < range.getEnd(); port++) {
          ports.add(port);
        }
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
      long rangeEnd = Math.min(numPorts - portsSoFar + range.getBegin(), range.getEnd());
      
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
        .setType(Type.RANGES)
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
  
  public static boolean isTaskFailed(TaskState state) {
    return state == TaskState.TASK_FAILED || state == TaskState.TASK_LOST || state == TaskState.TASK_KILLED;
  }
  
  public static boolean isTaskDone(TaskState state) {
    return state == TaskState.TASK_FAILED || state == TaskState.TASK_LOST || state == TaskState.TASK_KILLED || state == TaskState.TASK_FINISHED;
  }
  
  public static String getMasterHostAndPort(MasterInfo masterInfo) {
    byte[] fromIp = ByteBuffer.allocate(4).putInt(masterInfo.getIp()).array();
   
    try {
      return String.format("%s:%s", InetAddresses.fromLittleEndianByteArray(fromIp).getHostAddress(), masterInfo.getPort());
    } catch (UnknownHostException e) {
      throw Throwables.propagate(e);
    }
  }
  
  public static Optional<Long> getFirstPort(Offer offer) {
    for (Resource resource : offer.getResourcesList()) {
      if (resource.getName().equals(MesosUtils.PORTS)) {
        if (resource.getRanges().getRangeCount() > 0) {
          return Optional.of(resource.getRanges().getRange(0).getBegin());
        }
      }
    }

    return Optional.absent();
  }
}
