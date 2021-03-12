package com.hubspot.singularity.helpers;

import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.async.ExecutorAndQueue;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityBlockingThreadPoolTest {

  @Test
  public void testBoundedQueueBlocksWhenFull() {
    SingularityManagedThreadPoolFactory threadPoolFactory = new SingularityManagedThreadPoolFactory(
      new SingularityConfiguration()
    );
    Assertions.assertThrows(
      RejectedExecutionException.class,
      () -> {
        ExecutorAndQueue executorAndQueue = threadPoolFactory.get("test", 2, 5, false);
        IntStream
          .range(0, 10)
          .forEach(
            i ->
              executorAndQueue
                .getExecutorService()
                .submit(
                  () -> {
                    try {
                      Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                      // didn't see that...
                    }
                  }
                )
          );
      }
    );

    Assertions.assertDoesNotThrow(
      () -> {
        ExecutorAndQueue executorAndQueue = threadPoolFactory.get("test", 2, 5, true);
        IntStream
          .range(0, 10)
          .forEach(
            i ->
              executorAndQueue
                .getExecutorService()
                .submit(
                  () -> {
                    try {
                      Thread.sleep(2000);
                    } catch (InterruptedException ie) {
                      // didn't see that...
                    }
                  }
                )
          );
      }
    );
  }
}
