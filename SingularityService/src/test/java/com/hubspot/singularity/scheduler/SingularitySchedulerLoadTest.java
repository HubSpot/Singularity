package com.hubspot.singularity.scheduler;

import com.google.common.base.Stopwatch;
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
import com.hubspot.singularity.data.CuratorManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.helpers.MesosProtosUtils;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.mesos.OfferCache;
import com.hubspot.singularity.mesos.SingularityMesosStatusUpdateHandler;
import com.hubspot.singularity.mesos.SingularityMesosTaskPrioritizer;
import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.curator.framework.CuratorFramework;
import org.apache.mesos.v1.Protos;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.TaskID;
import org.apache.mesos.v1.Protos.TaskState;
import org.apache.mesos.v1.Protos.TaskStatus;
import org.apache.mesos.v1.Protos.TaskStatus.Reason;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingularitySchedulerLoadTest extends SingularitySchedulerTestBase {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularitySchedulerLoadTest.class
  );

  @Inject
  SingularitySchedulerPoller poller;

  //  @Inject
  //  CuratorFramework curator;

  private int racks = 3;
  private int agents = 150;
  private int requests = 50;
  private int maxTasksPerRequest = 10;
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
    super(
      false,
      cfg -> {
        cfg.getZooKeeperConfiguration().setCuratorFrameworkInstances(1);
        cfg.setCoreThreadpoolSize(8);
        cfg.setCacheOffers(true);
        cfg.setOfferCacheSize(1000);
        cfg.setMaxTasksPerOffer(1000);
        cfg.setMaxTasksPerOfferPerRequest(5);
        cfg.getMesosConfiguration().setOfferLoopTimeoutMillis(120_000);
        cfg.getMesosConfiguration().setOfferLoopRequestTimeoutMillis(30_000);
        return null;
      }
    );
  }

  @Test
  public void test() throws Exception {
    this.run();
  }

  private void run() throws IOException {
    time("Resource offers", this::initResourceOffers);
    time("Creating pending tasks", this::initSchedulingLoad);

    LOG.info("Due tasks: {}", scheduler.getDueTasks().size());
    Assertions.assertTrue(requests <= scheduler.getDueTasks().size());

    time(
      String.format("Scheduling %d tasks", scheduler.getDueTasks().size()),
      poller::runActionOnPoll
    );

    LOG.info("Remaining due tasks: {}", scheduler.getDueTasks().size());
    Assertions.assertEquals(0, scheduler.getDueTasks().size());
  }

  private void time(String message, Runnable runnable) {
    Stopwatch stopwatch = Stopwatch.createStarted();
    runnable.run();

    // may need to modify log level in test base classes
    LOG.info("{} took {}", message, stopwatch.elapsed());
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
      RequestType.WORKER
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
    SingularityRequestBuilder rb = new SingularityRequestBuilder(requestId(), type)
    .setInstances(
        Optional.of(ThreadLocalRandom.current().nextInt(1, maxTasksPerRequest))
      );

    //    if (type == RequestType.SERVICE) {
    //      rb.setRackSensitive(Optional.of(true));
    //    }

    SingularityRequest request = rb.build();
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

    SingularityPendingDeploy pending = new SingularityPendingDeploy(
      marker,
      DeployState.WAITING,
      SingularityDeployProgress.forNewDeploy(request.getInstancesSafe(), false),
      Optional.of(request)
    );
    deployManager.savePendingDeploy(pending);

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

  private double nextGaussian(double origin, double bound) {
    return ThreadLocalRandom.current().nextGaussian() * (bound - origin) + origin;
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
}
