package com.hubspot.mesos;

import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.Resource;
import org.apache.mesos.Protos.TaskState;
import org.apache.mesos.Protos.Value;

public class MesosUtils {
  
  public static final String CPUS = "cpus";
  public static final String MEMORY = "mem";

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

  public static Resource getCpuResource(int cpus) {
    return newScalar(CPUS, cpus);
  }

  public static Resource getMemoryResource(int memory) {
    return newScalar(MEMORY, memory);
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

  public static boolean doesOfferMatchResources(Resources resources, Offer offer) {
    int numCpus = getNumCpus(offer);

    if (numCpus < resources.getCpus()) {
      return false;
    }

    int memory = getMemory(offer);

    if (memory < resources.getMemoryMb()) {
      return false;
    }

    return true;
  }
  
  public static boolean isTaskDone(TaskState state) {
    return state == TaskState.TASK_FAILED || state == TaskState.TASK_LOST || state == TaskState.TASK_KILLED || state == TaskState.TASK_FINISHED;
  }
}
