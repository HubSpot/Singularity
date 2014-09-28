package com.hubspot.singularity;

import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public interface SingularityLeaderOnlyPoller {

  public void start(final SingularityMesosSchedulerDelegator mesosScheduler);

  public void stop();

}
