package com.hubspot.singularity.scheduler;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.singularity.CanaryDeploySettings;
import com.hubspot.singularity.CanaryDeploySettingsBuilder;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployBuilder;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployProgress;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingRequestBuilder;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskBuilder;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SingularityMesosStatusUpdateHandler;
import com.hubspot.singularity.mesos.SingularityMesosTaskPrioritizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SingularitySchedulerLoadTest extends SingularitySchedulerTestBase {
  @Inject
  private SingularityValidator validator;

  @Inject
  private SingularityDeployHealthHelper deployHealthHelper;

  @Inject
  private SingularityMesosTaskPrioritizer taskPrioritizer;

  @Inject
  private SingularitySchedulerPoller schedulerPoller;

  @Inject
  private OfferCache offerCache;

  @Inject
  private MesosProtosUtils mesosProtosUtils;

  @Inject
  SingularityMesosStatusUpdateHandler updateHandler;

  private int racks = 3;
  private int agents = 150;
  private int requests = 1000;
  private int maxTasksPerRequest = 50;
  private AtomicInteger requestId = new AtomicInteger();
  private AtomicInteger deployId = new AtomicInteger();
  private double cpuPerAgent = 24;
  private double memPerAgent = 1024 * 300; // mb
  private double dskPerAgent = 1024;
  private String[] portsPerAgent = new String[] { "1:1000" };
  private AtomicDouble cpu = new AtomicDouble(agents * cpuPerAgent);
  private AtomicDouble mem = new AtomicDouble(agents * memPerAgent);
  private Map<String, List<Protos.Resource>> cluster = new HashMap<>();
  private ExecutorService executor = Executors.newCachedThreadPool();

  public SingularitySchedulerLoadTest() {
    super(false);
  }

  @Test
  public void test() {
    configuration.setCacheOffers(true);
    configuration.setOfferCacheSize(1000);
    configuration.setMaxTasksPerOffer(15);
    configuration.setMaxTasksPerOfferPerRequest(5);
    configuration.getMesosConfiguration().setOfferLoopTimeoutMillis(10_000);

    initSchedulingLoad();

    Assertions.assertTrue(requests <= scheduler.getDueTasks().size());

    //    scheduler.drainPendingQueue();
    initResourceOffers();
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());
    Assertions.assertEquals(0, scheduler.getDueTasks().size());
  }

  private void initResourceOffers() {
    List<Offer> offers = new ArrayList<>(agents);
    for (int agentId = 0; agentId < agents; agentId++) {
      Optional<String> rack = Optional.of(String.valueOf(agentId % racks));
      Offer offer = createOffer(
        cpuPerAgent,
        memPerAgent,
        dskPerAgent,
        String.valueOf(agentId),
        String.valueOf(agentId),
        rack,
        Collections.emptyMap(),
        portsPerAgent
      );

      offers.add(offer);
      cluster.put(String.valueOf(agentId), offer.getResourcesList());
    }

    sms.resourceOffers(offers);
  }

  private void initSchedulingLoad() {
    // excluding scheduled/run-once to simplify things
    RequestType[] requestTypes = new RequestType[] {
      RequestType.SERVICE,
      RequestType.WORKER,
      RequestType.ON_DEMAND
    };

    List<CompletableFuture<Void>> futures = IntStream
      .range(0, requests)
      .mapToObj(
        i -> {
          RequestType type = requestTypes[i % requestTypes.length];
          Resources resources = generateResources();
          return CompletableFuture.runAsync(
            () -> generateRequest(type, resources),
            executor
          );
        }
      )
      .collect(Collectors.toList());

    CompletableFutures.allOf(futures).join();
  }

  private void generateRequest(RequestType type, Resources resources) {
    SingularityRequest request = new SingularityRequestBuilder(requestId(), type)
      .setInstances(
        Optional.of(ThreadLocalRandom.current().nextInt(1, maxTasksPerRequest))
      )
      .build();
    saveRequest(request);

    SingularityDeploy deploy = new SingularityDeployBuilder(request.getId(), deployId())
      .setCommand(Optional.of("sleep 1"))
      .setResources(Optional.of(resources))
      .build();

    saveDeploy(request, deploy);

    for (int i = 0; i < request.getInstances().orElse(1); i++) {
      createAndSchedulePendingTask(request.getId(), deploy.getId());
    }
  }

  private void saveDeploy(SingularityRequest request, SingularityDeploy deploy) {
    SingularityDeployMarker marker = new SingularityDeployMarker(
      deploy.getRequestId(),
      deploy.getId(),
      System.currentTimeMillis(),
      Optional.<String>empty(),
      Optional.<String>empty()
    );
    deployManager.saveDeploy(request, marker, deploy);
    deployManager.savePendingDeploy(
      new SingularityPendingDeploy(
        marker,
        DeployState.WAITING,
        SingularityDeployProgress.forNewDeploy(request.getInstancesSafe(), false),
        Optional.of(request)
      )
    );

    finishDeploy(marker, deploy);

    requestManager.addToPendingQueue(
      new SingularityPendingRequestBuilder()
        .setRequestId(request.getId())
        .setDeployId(deploy.getId())
        .setPendingType(PendingType.NEW_DEPLOY)
        .setRunAt(System.currentTimeMillis())
        .setResources(deploy.getResources())
        .build()
    );
  }

  private String requestId() {
    return String.valueOf(requestId.getAndIncrement());
  }

  private String deployId() {
    return String.valueOf(deployId.getAndIncrement());
  }

  private Resources generateResources() {
    double cpu = nextGaussian(1, 8);
    double mem = nextGaussian(1024, 1024 * 10);
    int ports = (int) nextGaussian(1, 10);
    return new Resources(cpu, mem, ports);
  }

  private void takeResources(String agentId, List<Protos.Resource> resources) {}

  private double nextGaussian(double origin, double bound) {
    return ThreadLocalRandom.current().nextGaussian() * (bound - origin) + origin;
  }

  private Resources offerToResources(Offer offer) {
    return MesosUtils.buildResourcesFromMesosResourceList(
      offer.getResourcesList(),
      Optional.empty()
    );
  }

  private SingularityPendingTask pendingTask(
    String requestId,
    String deployId,
    PendingType pendingType
  ) {
    return new SingularityPendingTaskBuilder()
      .setPendingTaskId(
        new SingularityPendingTaskId(
          requestId,
          deployId,
          System.currentTimeMillis(),
          1,
          pendingType,
          System.currentTimeMillis()
        )
      )
      .build();
  }

  private void runTest(RequestType requestType, Reason reason, boolean shouldRetry) {
    initRequestWithType(requestType, false);
    initFirstDeploy();

    SingularityTask task = startTask(firstDeploy);
    Assertions.assertEquals(0, taskManager.getPendingTaskIds().size());
    Assertions.assertEquals(0, requestManager.getPendingRequests().size());

    try {
      updateHandler
        .processStatusUpdateAsync(
          TaskStatus
            .newBuilder()
            .setState(TaskState.TASK_LOST)
            .setReason(reason)
            .setTaskId(TaskID.newBuilder().setValue(task.getTaskId().getId()))
            .build()
        )
        .get();
    } catch (InterruptedException | ExecutionException e) {
      Assertions.assertTrue(false);
    }

    if (shouldRetry) {
      Assertions.assertEquals(requestManager.getPendingRequests().size(), 1);
      Assertions.assertEquals(
        requestManager.getPendingRequests().get(0).getPendingType(),
        PendingType.RETRY
      );
    } else {
      if (requestManager.getPendingRequests().size() > 0) {
        Assertions.assertEquals(
          requestManager.getPendingRequests().get(0).getPendingType(),
          PendingType.TASK_DONE
        );
      }
    }
    scheduler.drainPendingQueue();
  }
}
