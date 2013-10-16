package com.hubspot.singularity.mesos;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

import java.util.List;

public class SingularityScheduler implements Scheduler {
  public static final String FRAMEWORK_NAME = "Singularity-0.0.1";
  private final List<Protos.Resource> desiredResources;
  private final Protos.CommandInfo commandInfo;

  private boolean taskLaunched = false;

  @Inject
  public SingularityScheduler(List<Protos.Resource> desiredResources, Protos.CommandInfo commandInfo) {
    this.desiredResources = desiredResources;
    this.commandInfo = commandInfo;
  }

  @Override
  public void registered(SchedulerDriver driver, Protos.FrameworkID frameworkId, Protos.MasterInfo masterInfo) {
    System.out.println("Registered.");
  }

  @Override
  public void reregistered(SchedulerDriver driver, Protos.MasterInfo masterInfo) {
    System.out.println("Re-registered.");
  }

  private boolean offerSatisfiesDesiredResources(Protos.Offer offer) {
    if (desiredResources.size() == 0) {
      return true;
    }

    for (Protos.Resource desiredResource : desiredResources) {
      boolean matchingOffer = false;

      for (Protos.Resource offeredResource : offer.getResourcesList()) {
        if (offeredResource.getName().equals(desiredResource.getName()) && offeredResource.getType().equals(desiredResource.getType())) {
          matchingOffer = true;

          if (offeredResource.getType() == Protos.Value.Type.SCALAR) {
            if (offeredResource.getScalar().getValue() < desiredResource.getScalar().getValue()) {
              return false;
            }
          } else {
            return false;
          }
        }
      }

      if (!matchingOffer) {  // all offer resources need a matching desired resource
        return false;
      }
    }

    return true;
  }

  @Override
  public void resourceOffers(SchedulerDriver driver, List<Protos.Offer> offers) {
    System.out.println("Got an offer!");
    for (Protos.Offer offer : offers) {
      if (!taskLaunched && offerSatisfiesDesiredResources(offer)) {
        System.out.println(String.format("Accepting slot on slave %s (%s)", offer.getSlaveId(), offer.getHostname()));

        driver.launchTasks(offer.getId(), ImmutableList.of(Protos.TaskInfo.newBuilder()
            .setCommand(commandInfo)
            .build()
        ));
        taskLaunched = true;
      } else {
        driver.declineOffer(offer.getId());
      }
    }
  }

  @Override
  public void offerRescinded(SchedulerDriver driver, Protos.OfferID offerId) {
    System.out.println("Offer rescinded");
  }

  @Override
  public void statusUpdate(SchedulerDriver driver, Protos.TaskStatus status) {
    System.out.println("status update");
  }

  @Override
  public void frameworkMessage(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, byte[] data) {
    System.out.println("framework message");
  }

  @Override
  public void disconnected(SchedulerDriver driver) {
    System.out.println("disconnected");
  }

  @Override
  public void slaveLost(SchedulerDriver driver, Protos.SlaveID slaveId) {
    System.out.println("lost slave");
  }

  @Override
  public void executorLost(SchedulerDriver driver, Protos.ExecutorID executorId, Protos.SlaveID slaveId, int status) {
    System.out.println("lost executor");
  }

  @Override
  public void error(SchedulerDriver driver, String message) {
    System.out.println("error");
  }
}
