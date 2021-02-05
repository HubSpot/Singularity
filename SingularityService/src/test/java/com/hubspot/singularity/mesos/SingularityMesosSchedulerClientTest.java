package com.hubspot.singularity.mesos;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

public class SingularityMesosSchedulerClientTest {
  private SingularityMesosSchedulerClient client;
  private ExecutorService executorService;
  private Exception exception;
  private SingularityMesosScheduler scheduler;

  @BeforeEach
  public void setup() {
    executorService = Mockito.mock(ExecutorService.class);
    SingularityManagedThreadPoolFactory executorServiceFactory = Mockito.mock(
      SingularityManagedThreadPoolFactory.class
    );
    Mockito
      .when(executorServiceFactory.get("singularity-mesos-scheduler-client", 1))
      .thenReturn(executorService);

    client =
      new SingularityMesosSchedulerClient(
        Mockito.mock(SingularityConfiguration.class),
        "test",
        Mockito.mock(AtomicLong.class),
        executorServiceFactory
      );

    scheduler = Mockito.mock(SingularityMesosScheduler.class);
    exception = Mockito.mock(Exception.class);

    try {
      Field schedulerField =
        SingularityMesosSchedulerClient.class.getDeclaredField("scheduler");
      schedulerField.setAccessible(true);
      schedulerField.set(client, scheduler);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void itCheckAndReconnectThrowsException() {
    doAnswer(
        (InvocationOnMock invocation) -> {
          ((Runnable) invocation.getArguments()[0]).run();
          return null;
        }
      )
      .when(executorService)
      .execute(any(Runnable.class));

    Mockito
      .when(exception.getMessage())
      .thenReturn(
        "Error while trying to send request. Status: 403 Message: \'Framework is not subscribed\'"
      );
    client.checkAndReconnect(exception).join();

    verify(scheduler, times(1)).onUncaughtException(any());
  }

  @Test
  public void itCheckAndReconnectDoesNotThrowsException() {
    Mockito.when(exception.getMessage()).thenReturn(null);
    client.checkAndReconnect(exception).join();

    verify(scheduler, never()).onUncaughtException(any());
  }
}
