package com.hubspot.singularity;

import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public interface SingularityLeaderOnlyPoller {

  void start(final SingularityMesosSchedulerDelegator mesosScheduler);

  void stop();

}
