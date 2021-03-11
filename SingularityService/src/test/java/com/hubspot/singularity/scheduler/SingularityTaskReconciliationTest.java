package com.hubspot.singularity.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.async.ExecutorAndQueue;
import com.hubspot.singularity.mesos.SingularityMesosStatusUpdateHandler;
import com.hubspot.singularity.scheduler.SingularityTaskReconciliation.ReconciliationState;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.junit.jupiter.api.Test;

public class SingularityTaskReconciliationTest extends SingularitySchedulerTestBase {
  @Inject
  private SingularityMesosStatusUpdateHandler updateHandler;

  public SingularityTaskReconciliationTest() {
    super(
      false,
      configuration -> {
        configuration.getMesosConfiguration().setMaxStatusUpdateQueueSize(200);
        return null;
      }
    );
  }

  @Test
  public void itTestStartReconciliation() {
    initWithTasks(10);
    ReconciliationState state = taskReconciliation.startReconciliation();
    assertThat(state).isEqualTo(ReconciliationState.STARTED);
  }

  @Test
  public void itTestReconciliationAlreadyRunning() {
    initWithTasks(10);
    taskReconciliation.startReconciliation();
    ReconciliationState state = taskReconciliation.startReconciliation();

    assertThat(state).isEqualTo(ReconciliationState.ALREADY_RUNNING);
  }

  @Test
  public void itTestBlockedReconciliation()
    throws NoSuchFieldException, IllegalAccessException {
    initWithTasks(190);

    startTaskStatusUpdates();
    Class<?> updateHandlerClass = updateHandler.getClass();
    Field executorField = updateHandlerClass.getDeclaredField("statusUpdatesExecutor");
    executorField.setAccessible(true);
    ExecutorAndQueue executor = (ExecutorAndQueue) executorField.get(updateHandler);
    executor.getExecutorService();

    ReconciliationState state = taskReconciliation.startReconciliation();
    assertThat(state).isEqualTo(ReconciliationState.NO_DRIVER);
  }

  private void startTaskStatusUpdates() {
    CompletableFuture.runAsync(
      () -> {
        for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
          updateHandler.processStatusUpdateAsync(
            TaskStatus
              .newBuilder()
              .setState(TaskState.TASK_RUNNING)
              .setTaskId(TaskID.newBuilder().setValue(taskId.getId()))
              .build()
          );
        }
      }
    );
  }
}
