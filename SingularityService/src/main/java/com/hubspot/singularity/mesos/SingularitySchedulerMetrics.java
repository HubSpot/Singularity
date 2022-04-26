package com.hubspot.singularity.mesos;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SingularitySchedulerMetrics {

  private final Histogram offerLoopTime;
  private final Counter tasksScheduled;
  private final Histogram offerLoopTasksRemaining;
  private final Histogram deployPollerTime;
  private final Histogram lbUpdateTime;
  private final Histogram offerLoopOverLoadedHosts;
  private final Histogram offerLoopNoMatches;

  @Inject
  SingularitySchedulerMetrics(MetricRegistry metricRegistry) {
    this.offerLoopTime = metricRegistry.histogram("offer-loop.time");
    this.tasksScheduled = metricRegistry.counter("tasks-scheduled");
    this.offerLoopTasksRemaining = metricRegistry.histogram("offer-loop.tasks-remaining");
    this.deployPollerTime = metricRegistry.histogram("deploy-poller-time");
    this.lbUpdateTime = metricRegistry.histogram("lb-update-time");
    this.offerLoopOverLoadedHosts =
      metricRegistry.histogram("offer-loop.overloaded-hosts");
    this.offerLoopNoMatches = metricRegistry.histogram("offer-loop.no-matches");
  }

  public Histogram getOfferLoopTime() {
    return offerLoopTime;
  }

  public Counter getTasksScheduled() {
    return tasksScheduled;
  }

  public Histogram getOfferLoopTasksRemaining() {
    return offerLoopTasksRemaining;
  }

  public Histogram getDeployPollerTime() {
    return deployPollerTime;
  }

  public Histogram getLbUpdateTime() {
    return lbUpdateTime;
  }

  public Histogram getOfferLoopOverLoadedHosts() {
    return offerLoopOverLoadedHosts;
  }

  public Histogram getOfferLoopNoMatches() {
    return offerLoopNoMatches;
  }
}
