package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.TaskState;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SlavePlacement;

public class SingularityOfferPerformanceTestRunner extends SingularitySchedulerTestBase {

  public SingularityOfferPerformanceTestRunner() {
    super(false);
  }

  @Test(timeout = 600000L)
  @Ignore
  public void testSchedulerPerformance() {
    long start = System.currentTimeMillis();

    int numRequests = 4500;
    int numOffers = 2500;

    Random r = new Random();
    Iterator<Double> cpuIterator = r.doubles(1, 5).iterator();
    Iterator<Double> memoryIterator = r.doubles(15, 20000).iterator();
    Iterator<Double> diskIterator = r.doubles(30, 50000).iterator();

    for (int i = 0; i < numRequests; i++) {
      SingularityRequestBuilder bldr = new SingularityRequestBuilder("request-" + i, RequestType.SERVICE);

      bldr.setInstances(Optional.of(5));
      bldr.setSlavePlacement(Optional.of(SlavePlacement.GREEDY));
      SingularityRequest request = bldr.build();
      saveRequest(request);
      deployRequest(request, cpuIterator.next(), memoryIterator.next());
    }

    List<Offer> offers = new ArrayList<>(numOffers);

    for (int i = 0; i < numOffers; i++) {
      offers.add(createOffer(cpuIterator.next(), memoryIterator.next(), diskIterator.next(), "slave-" + i, "host-" + i));
    }

    System.out.println("Starting after " + JavaUtils.duration(start));

    start = System.currentTimeMillis();

    sms.resourceOffers(offers);

    final long duration = System.currentTimeMillis() - start;

    int activeTasks = taskManager.getActiveTaskIds().size();

    System.out.println(String.format("Launched %s tasks on %s offers in %s", activeTasks, numOffers, JavaUtils.durationFromMillis(duration)));

    start = System.currentTimeMillis();

    ExecutorService statusUpadtes = Executors.newFixedThreadPool(100);
    CompletableFuture<Void>[] updateFutures = new CompletableFuture[taskManager.getActiveTaskIds().size()*5];
    AtomicInteger i = new AtomicInteger(0);
    for (SingularityTaskId taskId : taskManager.getActiveTaskIds()) {
      updateFutures[i.getAndIncrement()] = CompletableFuture.supplyAsync(() -> {
        statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_STAGING);
        return null;
      }, statusUpadtes);
      updateFutures[i.getAndIncrement()] = CompletableFuture.supplyAsync(() -> {
        statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_STARTING);
        return null;
      }, statusUpadtes);
      updateFutures[i.getAndIncrement()] = CompletableFuture.supplyAsync(() -> {
        statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_RUNNING);
        return null;
      }, statusUpadtes);
      updateFutures[i.getAndIncrement()] = CompletableFuture.supplyAsync(() -> {
        statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_FAILED);
        return null;
      }, statusUpadtes);
      updateFutures[i.getAndIncrement()] = CompletableFuture.supplyAsync(() -> {
        statusUpdate(taskManager.getTask(taskId).get(), TaskState.TASK_FAILED);
        return null;
      }, statusUpadtes);
    }
    CompletableFuture.allOf(updateFutures).join();

    System.out.println(String.format("Ran %s status updates in %s", activeTasks * 5, JavaUtils.durationFromMillis(System.currentTimeMillis() - start)));
  }


}
