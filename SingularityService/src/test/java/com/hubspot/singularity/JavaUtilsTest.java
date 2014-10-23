package com.hubspot.singularity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.hubspot.mesos.JavaUtils;

public class JavaUtilsTest {

  @Test
  public void testFixedTimingOutThreadPool() throws Exception {
    int numMaxThreads = 5;
    long timeoutMillis = 2;

    ThreadPoolExecutor es = JavaUtils.newFixedTimingOutThreadPool(numMaxThreads, timeoutMillis, "test");

    Thread.sleep(timeoutMillis + 100);

    Assert.assertTrue(es.getPoolSize() == 0);

    final CountDownLatch block = new CountDownLatch(1);
    final CountDownLatch cdl = new CountDownLatch(numMaxThreads);

    for (int i = 0; i < numMaxThreads; i++) {
      es.submit(new Runnable() {

        @Override
        public void run() {
          try {
            cdl.countDown();
            cdl.await();
            block.await();
          } catch (Throwable t) {
            throw Throwables.propagate(t);
          }
        }
      });
    }

    cdl.await();
    // all threads are running:
    Assert.assertTrue(es.getPoolSize() == numMaxThreads);
    block.countDown();

    Thread.sleep(timeoutMillis + 100);
    Assert.assertTrue(es.getMaximumPoolSize() == numMaxThreads);
    Assert.assertTrue(es.getPoolSize() == 0);

    es.shutdown();
    es.awaitTermination(timeoutMillis + 1, TimeUnit.MILLISECONDS);
  }

}
