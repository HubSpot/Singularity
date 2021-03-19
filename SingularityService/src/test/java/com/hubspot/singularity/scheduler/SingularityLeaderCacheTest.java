package com.hubspot.singularity.scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularityLeaderCacheTest {

  @Test
  public void testBlockWhileBootstrapping() throws Exception {
    SingularityLeaderCache leaderCache = new SingularityLeaderCache();
    AtomicBoolean reachedTheEnd = new AtomicBoolean(false);
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Runnable testRun = () -> {
      if (leaderCache.active()) {
        reachedTheEnd.set(true);
      }
    };

    // Should now block anything calling leaderCache.active() until bootstrap done
    leaderCache.startBootstrap();
    CompletableFuture.runAsync(testRun, executorService);
    Assertions.assertFalse(reachedTheEnd.get());
    Thread.sleep(200); // just in case
    Assertions.assertFalse(reachedTheEnd.get());

    // should notify any waiting and unblock
    leaderCache.activate();
    Thread.sleep(200);
    Assertions.assertTrue(reachedTheEnd.get());
  }
}
