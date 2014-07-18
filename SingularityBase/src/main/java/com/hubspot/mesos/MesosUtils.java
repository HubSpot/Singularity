package com.hubspot.mesos;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

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
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;

public class MesosUtils {

  public static final String CPUS = "cpus";
  public static final String MEMORY = "mem";
  public static final String PORTS = "ports";

  private MesosUtils() { }

  private static int getScalar(Resource r) {
    return (int) r.getScalar().getValue();
  }

  private static int getScalar(List<Resource> resources, String name) {
    for (Resource r : resources) {
      if (r.hasName() && r.getName().equals(name) && r.hasScalar()) {
        return getScalar(r);
      }
    }

    return 0;
  }
  
  private static Ranges getRanges(List<Resource> resources, String name) {
    for (Resource r : resources) {
      if (r.hasName() && r.getName().equals(name) && r.hasRanges()) {
        return r.getRanges();
      }
    }
    
    return null;
  }

  private static Ranges getRanges(TaskInfo taskInfo, String name) {
    return getRanges(taskInfo.getResourcesList(), name);
  }
    
  private static int getNumRanges(List<Resource> resources, String name) {
    int totalRanges = 0;
    
    Ranges ranges = getRanges(resources, name);
    
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
      for (long port = r.getBegin(); port <= r.getEnd(); port++) {
        ports[idx++] = port;

        if (idx >= numPorts) {
          return ports;
        }
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
    return getPortsResource(numPorts, offer.getResourcesList());
  }
  
  public static Resource getPortsResource(int numPorts, List<Resource> resources) {
    Ranges ranges = getRanges(resources, PORTS);
    
    Preconditions.checkState(ranges != null, "Ports %s should have existed in resources %s", PORTS, resources);
    
    Ranges.Builder rangesBldr = Ranges.newBuilder();
    
    int portsSoFar = 0;
    
    List<Range> offerRangeList = Lists.newArrayList(ranges.getRangeList());
    
    Random random = new Random();
    Collections.shuffle(offerRangeList, random);
    
    for (Range range : offerRangeList) {
      long rangeStartSelection = Math.max(range.getBegin(), range.getEnd() - (numPorts - portsSoFar + 1));
      
      if (rangeStartSelection != range.getBegin()) {
        int rangeDelta = (int) (rangeStartSelection - range.getBegin()) + 1;
        rangeStartSelection = random.nextInt(rangeDelta) + range.getBegin();
      }
      
      long rangeEndSelection = Math.min(range.getEnd(), rangeStartSelection + (numPorts - portsSoFar - 1));
    
      rangesBldr.addRange(Range.newBuilder()
          .setBegin(rangeStartSelection)
          .setEnd(rangeEndSelection));
    
      portsSoFar += (rangeEndSelection - rangeStartSelection) + 1;
      
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
    return getNumCpus(offer.getResourcesList());
  }

  public static int getMemory(Offer offer) {
    return getMemory(offer.getResourcesList());
  }
  
  public static int getNumCpus(List<Resource> resources) {
    return getScalar(resources, CPUS);
  }

  public static int getMemory(List<Resource> resources) {
    return getScalar(resources, MEMORY);
  }

  public static int getNumPorts(List<Resource> resources) {
    return getNumRanges(resources, PORTS);
  }
  
  public static int getNumPorts(Offer offer) {
    return getNumPorts(offer.getResourcesList());
  }
  
  public static boolean doesOfferMatchResources(Resources resources, List<Resource> offerResources) {
    int numCpus = getNumCpus(offerResources);

    if (numCpus < resources.getCpus()) {
      return false;
    }

    int memory = getMemory(offerResources);

    if (memory < resources.getMemoryMb()) {
      return false;
    }
    
    int numPorts = getNumPorts(offerResources);
    
    if (numPorts < resources.getNumPorts()) {
      return false;
    }
    
    return true;
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
  
  private static Optional<Resource> getMatchingResource(Resource toMatch, List<Resource> resources) {
    for (Resource resource : resources) {
      if (toMatch.getName().equals(resource.getName())) {
        return Optional.of(resource);
      }
    }
    
    return Optional.absent();
  }
  
  private static final Comparator<Range> RANGE_COMPARATOR = new Comparator<Range>() {
    @Override
    public int compare(Range o1, Range o2) {
      return Longs.compare(o1.getBegin(), o2.getBegin());
    }
  };
  
  private static Ranges subtractRanges(Ranges ranges, Ranges toSubtract) {
    Ranges.Builder newRanges = Ranges.newBuilder();

    List<Range> sortedRanges = Lists.newArrayList(ranges.getRangeList());
    Collections.sort(sortedRanges, RANGE_COMPARATOR);
    
    List<Range> subtractRanges = Lists.newArrayList(toSubtract.getRangeList());
    Collections.sort(subtractRanges, RANGE_COMPARATOR);
    
    int s = 0;
    
    for (Range range : ranges.getRangeList()) {
      Range.Builder currentRange = range.toBuilder();
      
      for (int i = s; i < subtractRanges.size(); i++) {
        Range matchedRange = subtractRanges.get(i);
      
        if (matchedRange.getBegin() < currentRange.getBegin() || matchedRange.getEnd() > currentRange.getEnd()) {
          s = i;
          break;
        }
        
        currentRange.setEnd(matchedRange.getBegin() - 1);
        if (currentRange.getEnd() >= currentRange.getBegin()) {
          newRanges.addRange(currentRange.build());
        }
        currentRange = Range.newBuilder();
        currentRange.setBegin(matchedRange.getEnd() + 1);
        currentRange.setEnd(range.getEnd());
      }
    
      if (currentRange.getEnd() >= currentRange.getBegin()) {
        newRanges.addRange(currentRange.build());
      }
    }
      
    return newRanges.build();
  }
  
  public static List<Resource> subtractResources(List<Resource> resources, List<Resource> subtract) {
    List<Resource> remaining = Lists.newArrayListWithCapacity(resources.size());
    
    for (Resource resource : resources) {
      Optional<Resource> matched = getMatchingResource(resource, subtract);
      
      if (!matched.isPresent()) {
        remaining.add(resource.toBuilder().clone().build());
      } else {
        Resource.Builder resourceBuilder = resource.toBuilder().clone();
        if (resource.hasScalar()) {
          resourceBuilder.setScalar(resource.toBuilder().getScalarBuilder().setValue(resource.getScalar().getValue() - matched.get().getScalar().getValue()).build());
        } else if (resource.hasRanges()) {
          resourceBuilder.setRanges(subtractRanges(resource.getRanges(), matched.get().getRanges()));
        } else {
          throw new IllegalStateException(String.format("Can't subtract non-scalar or range resources %s", resource));
        }
        
        remaining.add(resourceBuilder.build());
      }
    }
    
    return remaining;
  }
  
}
