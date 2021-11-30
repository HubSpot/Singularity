package com.hubspot.singularity.scheduler;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTaskBuilder;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.helpers.TaskLagGuardrail;
import com.hubspot.singularity.mesos.SingularitySchedulerLock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularitySchedulerLockTest extends SingularitySchedulerTestBase {
  private final ExecutorService executor;

  @Inject
  private SingularitySchedulerLock lock;

  @Inject
  private TaskLagGuardrail lagGuardrail;

  private String name = SingularitySchedulerLockTest.class.getSimpleName();

  public SingularitySchedulerLockTest() {
    super(false);
    this.executor = Executors.newSingleThreadExecutor();
  }

  @Test
  public void testLowPriorityLockNormal() {
    TestRunnable runnable = new TestRunnable();
    lock.runWithRequestLock(
      runnable,
      requestId,
      name,
      SingularitySchedulerLock.Priority.LOW
    );
    Assertions.assertTrue(runnable.ran);
  }

  @Test
  public void testLowPriorityLockLaggedWithoutContention() {
    laggedRequest();
    TestRunnable runnable = new TestRunnable();
    lock.runWithRequestLock(
      runnable,
      requestId,
      name,
      SingularitySchedulerLock.Priority.LOW
    );
    Assertions.assertTrue(runnable.ran);
  }

  @Test
  public void testLowPriorityLockLaggedWithContention() {
    laggedRequest();

    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch finished = new CountDownLatch(1);
    hold(started, finished);

    TestRunnable runnable = new TestRunnable();
    lock.runWithRequestLock(
      runnable,
      requestId,
      name,
      SingularitySchedulerLock.Priority.LOW
    );
    Assertions.assertFalse(runnable.ran);
    finished.countDown();
  }

  private void hold(CountDownLatch started, CountDownLatch finished) {
    executor.submit(
      () -> {
        lock.runWithRequestLock(
          () -> {
            try {
              started.countDown();
              finished.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          },
          requestId,
          name
        );
      }
    );

    try {
      started.await(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private void laggedRequest() {
    taskManager.savePendingTask(
      new SingularityPendingTaskBuilder()
        .setPendingTaskId(
          new SingularityPendingTaskId(
            requestId,
            firstDeployId,
            0,
            0,
            SingularityPendingRequest.PendingType.NEW_DEPLOY,
            0
          )
        )
        .build()
    );
    lagGuardrail.updateLateTasksByRequestId();
  }

  private static class TestRunnable implements Runnable {
    boolean ran = false;

    @Override
    public void run() {
      ran = true;
    }
  }
}
