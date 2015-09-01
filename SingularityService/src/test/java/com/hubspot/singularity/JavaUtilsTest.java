package com.hubspot.singularity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.hubspot.mesos.JavaUtils;

import io.dropwizard.jackson.Jackson;

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

  @Test
  public void testSingularityTaskIdSerialization() throws Exception {
    ObjectMapper om = Jackson.newObjectMapper()
        .setSerializationInclusion(Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(new ProtobufModule());

    SingularityTaskId taskId = new SingularityTaskId("rid", "did", 100, 1, "host", "rack");
    String id = taskId.getId();
    SingularityTaskId fromId = SingularityTaskId.valueOf(id);
    SingularityTaskId fromJson = om.readValue(om.writeValueAsBytes(taskId), SingularityTaskId.class);

    assertEquals(taskId, fromId);
    assertEquals(taskId, fromJson);
    assertEquals(fromId, fromJson);
  }

  private void assertEquals(SingularityTaskId one, SingularityTaskId two) {

    Assert.assertEquals(one, two);

    Assert.assertEquals(one.getDeployId(), two.getDeployId());
    Assert.assertEquals(one.getRequestId(), two.getRequestId());
    Assert.assertEquals(one.getSanitizedHost(), two.getSanitizedHost());
    Assert.assertEquals(one.getSanitizedRackId(), two.getSanitizedRackId());
    Assert.assertEquals(one.getStartedAt(), two.getStartedAt());
    Assert.assertEquals(one.getInstanceNo(), two.getInstanceNo());
  }

}
