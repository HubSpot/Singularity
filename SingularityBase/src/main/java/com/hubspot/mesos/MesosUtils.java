package com.hubspot.mesos;

import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.mesos.v1.Protos.MasterInfo;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.Resource;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.Value.Range;
import org.apache.mesos.v1.Protos.Value.Ranges;
import org.apache.mesos.v1.Protos.Value.Scalar;
import org.apache.mesos.v1.Protos.Value.Type;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.net.InetAddresses;
import com.google.common.primitives.Longs;

public final class MesosUtils {

  public static final String CPUS = "cpus";
  public static final String MEMORY = "mem";
  public static final String PORTS = "ports";
  public static final String DISK = "disk";

  private MesosUtils() { }

  private static double getScalar(Resource r) {
    return r.getScalar().getValue();
  }

  private static double getScalar(List<Resource> resources, String name, Optional<String> requiredRole) {
    for (Resource r : resources) {
      if (r.hasName() && r.getName().equals(name) && r.hasScalar() && hasRequiredRole(r, requiredRole)) {
        return getScalar(r);
      }
    }

    return 0;
  }

  private static boolean hasRole(Resource r) {
    return r.hasRole() && !r.getRole().equals("*");
  }

  private static boolean hasRequiredRole(Resource r, Optional<String> requiredRole) {

    if (requiredRole.isPresent() && hasRole(r)) {
      // required role with a resource with role
      return requiredRole.get().equals(r.getRole());

    } else if (requiredRole.isPresent() && !hasRole(r)) {
      // required role with a resource for any role
      return false;

    } else if (!requiredRole.isPresent() && hasRole(r)) {
      // no required role but resource with role
      return false;

    } else if (!requiredRole.isPresent() && !hasRole(r)) {
      // no required role and resource for any role
      return true;
    } else {
      return false;
    }

  }

  private static Ranges getRanges(List<Resource> resources, String name) {
    for (Resource r : resources) {
      if (r.hasName() && r.getName().equals(name) && r.hasRanges()) {
        return r.getRanges();
      }
    }

    return Ranges.getDefaultInstance();
  }

  private static int getNumRanges(List<Resource> resources, String name) {
    int totalRanges = 0;

    for (Range range : getRanges(resources, name).getRangeList()) {
      totalRanges += (range.getEnd() - range.getBegin()) + 1;
    }

    return totalRanges;
  }

  public static Resource getCpuResource(double cpus, Optional<String> role) {
    return newScalar(CPUS, cpus, role);
  }

  public static Resource getMemoryResource(double memory, Optional<String> role) {
    return newScalar(MEMORY, memory, role);
  }

  public static Resource getDiskResource(double disk, Optional<String> role) {
    return newScalar(DISK, disk, role);
  }

  public static long[] getPorts(Resource portsResource, int numPorts) {
    long[] ports = new long[numPorts];
    if (numPorts == 0) {
      return ports;
    }

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

  public static Resource getPortRangeResource(long begin, long end) { return newRange(PORTS, begin, end); }

  public static List<Long> getAllPorts(List<Resource> resources) {
    Ranges ranges = getRanges(resources, PORTS);

    final List<Long> ports = Lists.newArrayList();
    if (ranges != null) {
      for (Range range : ranges.getRangeList()) {
        for (long port = range.getBegin(); port <= range.getEnd(); port++) {
          ports.add(port);
        }
      }
    }

    return ports;
  }

  public static Resource getPortsResource(int numPorts, Offer offer) {
    return getPortsResource(numPorts, offer.getResourcesList(), Collections.<Long>emptyList());
  }

  public static Resource getPortsResource(int numPorts, List<Resource> resources, List<Long> otherRequestedPorts) {
    List<Long> requestedPorts = new ArrayList<>(otherRequestedPorts);
    Ranges ranges = getRanges(resources, PORTS);

    Preconditions.checkState(ranges.getRangeCount() > 0, "Ports %s should have existed in resources %s", PORTS, formatForLogging(resources));

    Ranges.Builder rangesBldr = Ranges.newBuilder();

    int portsSoFar = 0;

    List<Range> offerRangeList = Lists.newArrayList(ranges.getRangeList());

    Random random = new Random();
    Collections.shuffle(offerRangeList, random);

    if (numPorts > 0) {
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

        List<Long> toRemove = new ArrayList<>();
        for (long port : requestedPorts) {
          if (rangeStartSelection >= port && rangeEndSelection <= port) {
            toRemove.add(port);
            portsSoFar--;
          }
        }
        requestedPorts.removeAll(toRemove);

        if (portsSoFar == numPorts) {
          break;
        }
      }
    }

    for (long port : requestedPorts) {
      rangesBldr.addRange(Range.newBuilder()
          .setBegin(port)
          .setEnd(port)
          .build());
    }

    return Resource.newBuilder()
        .setType(Type.RANGES)
        .setName(PORTS)
        .setRanges(rangesBldr)
        .build();
  }

  private static Resource newScalar(String name, double value, Optional<String> role) {
    Resource.Builder builder = Resource.newBuilder().setName(name).setType(Type.SCALAR).setScalar(Scalar.newBuilder().setValue(value).build());
    if (role.isPresent()) {
      builder.setRole(role.get());
    }

    return builder.build();
  }

  private static Resource newRange(String name, long begin, long end) {
    return Resource.newBuilder().setName(name).setType(Type.RANGES).setRanges(Ranges.newBuilder().addRange(Range.newBuilder().setBegin(begin).setEnd(end).build()).build()).build();
  }

  public static Set<String> getRoles(Offer offer) {
    Set<String> roles = Sets.newHashSet();

    for (Resource r : offer.getResourcesList()) {
      roles.add(r.getRole());
    }

    return roles;
  }

  public static double getNumCpus(Offer offer) {
    return getNumCpus(offer.getResourcesList(), Optional.<String>absent());
  }

  public static double getMemory(Offer offer) {
    return getMemory(offer.getResourcesList(), Optional.<String>absent());
  }

  public static double getDisk(Offer offer) {
    return getDisk(offer.getResourcesList(), Optional.<String>absent());
  }

  public static double getNumCpus(List<Resource> resources, Optional<String> requiredRole) {
    return getScalar(resources, CPUS, requiredRole);
  }

  public static double getMemory(List<Resource> resources, Optional<String> requiredRole) {
    return getScalar(resources, MEMORY, requiredRole);
  }

  public static double getDisk(List<Resource> resources, Optional<String> requiredRole) {
    return getScalar(resources, DISK, requiredRole);
  }

  public static int getNumPorts(List<Resource> resources) {
    return getNumRanges(resources, PORTS);
  }

  public static int getNumPorts(Offer offer) {
    return getNumPorts(offer.getResourcesList());
  }

  public static boolean doesOfferMatchResources(Optional<String> requiredRole, Resources resources, List<Resource> offerResources, List<Long> otherRequestedPorts) {
    double numCpus = getNumCpus(offerResources, requiredRole);

    if (numCpus < resources.getCpus()) {
      return false;
    }

    double memory = getMemory(offerResources, requiredRole);

    if (memory < resources.getMemoryMb()) {
      return false;
    }

    double disk = getDisk(offerResources, requiredRole);

    if (disk < resources.getDiskMb()) {
      return false;
    }

    int numPorts = getNumPorts(offerResources);

    if (numPorts < resources.getNumPorts()) {
      return false;
    }

    if (resources.getNumPorts() > 0 && !getAllPorts(offerResources).containsAll(otherRequestedPorts)) {
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
          throw new IllegalStateException(String.format("Can't subtract non-scalar or range resources %s", formatForLogging(resource)));
        }

        remaining.add(resourceBuilder.build());
      }
    }

    return remaining;
  }

  public static List<Resource> combineResources(List<List<Resource>> resourcesList) {
    List<Resource> resources = new ArrayList<>();
    for (List<Resource> resourcesToAdd : resourcesList) {
      for (Resource resource : resourcesToAdd) {
        Optional<Resource> matched = getMatchingResource(resource, resources);
        if (!matched.isPresent()) {
          resources.add(resource);
        } else {
          int index = resources.indexOf(matched.get());
          Resource.Builder resourceBuilder = resource.toBuilder().clone();
          if (resource.hasScalar()) {
            resourceBuilder.setScalar(resource.toBuilder().getScalarBuilder().setValue(resource.getScalar().getValue() + matched.get().getScalar().getValue()).build());
            resources.set(index, resourceBuilder.build());
          } else if (resource.hasRanges()) {
            Ranges.Builder newRanges = Ranges.newBuilder();
            resource.getRanges().getRangeList().forEach(newRanges::addRange);
            matched.get().getRanges().getRangeList().forEach(newRanges::addRange);
            resourceBuilder.setRanges(newRanges);
            resources.set(index, resourceBuilder.build());
          } else {
            throw new IllegalStateException(String.format("Can't subtract non-scalar or range resources %s", formatForLogging(resource)));
          }
        }
      }
    }
    return resources;
  }

  public static Resources buildResourcesFromMesosResourceList(List<Resource> resources, Optional<String> requiredRole) {
    return new Resources(getNumCpus(resources, requiredRole), getMemory(resources, requiredRole), getNumPorts(resources), getDisk(resources, requiredRole));
  }

  public static Path getTaskDirectoryPath(String taskId) {
    return Paths.get(getSafeTaskIdForDirectory(taskId)).toAbsolutePath();
  }

  public static String getSafeTaskIdForDirectory(String taskId) {
    return taskId.replace(":", "_");
  }

  public static String formatForLogging(Object object) {
    return object.toString().replace("\n", "").replaceAll("( )+", " ");
  }
}
