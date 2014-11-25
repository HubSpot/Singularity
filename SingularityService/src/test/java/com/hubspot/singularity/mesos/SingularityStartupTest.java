package com.hubspot.singularity.mesos;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularitySchedulerTestBase;
import com.hubspot.singularity.SingularityTask;

public class SingularityStartupTest extends SingularitySchedulerTestBase {

  @Inject
  private SingularityStartup startup;

  @Test
  public void testFailuresInLaunchPath() {
    initRequest();
    initFirstDeploy();

    SingularityTask task = prepTask();

    taskManager.createTaskAndDeletePendingTask(task);

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    startup.checkSchedulerForInconsistentState();

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);

    taskManager.deleteActiveTask(task.getTaskId().getId());

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 0);

    startup.checkSchedulerForInconsistentState();

    resourceOffers();

    Assert.assertTrue(taskManager.getActiveTaskIds().size() == 1);
  }



  // test inside-deploy failure?

  // make sure scheduled task works
  // make sure oneoff task doesn't get rescheduled

  // make sure we don't override an existing request of a different type.

  // TODO def test unapsue obsoletes


}
