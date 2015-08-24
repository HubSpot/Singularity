package com.hubspot.singularity.mesos;

import java.util.List;

import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.scheduler.SingularitySchedulerStateCache;
import org.apache.mesos.Protos;
import org.apache.mesos.SchedulerDriver;

public interface SingularityResourceScheduler {
  public List<SingularityOfferHolder> processOffers(SingularitySchedulerStateCache stateCache, List<SingularityTaskRequest> dueTasks, SchedulerDriver driver, List<Protos.Offer> offers);
}
