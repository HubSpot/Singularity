package com.hubspot.singularity.mesos;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityManagedThreadPoolFactory;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.helpers.SingularityMesosTaskHolder;
import com.hubspot.singularity.mesos.SingularityOfferCache.CachedOffer;
import com.hubspot.singularity.mesos.SingularitySlaveAndRackManager.CheckResult;
import com.hubspot.singularity.mesos.SingularitySlaveUsageWithCalculatedScores.MaxProbableUsage;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularityUsageHelper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Singleton;
import org.apache.mesos.v1.Protos.Offer;
import org.apache.mesos.v1.Protos.OfferID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityMesosOfferScheduler {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityMesosOfferScheduler.class
  );

  private final Resources defaultResources;
  private final Resources defaultCustomExecutorResources;
  private final TaskManager taskManager;
  private final SingularityMesosTaskPrioritizer taskPrioritizer;
  private final SingularityScheduler scheduler;
  private final SingularityConfiguration configuration;
  private final MesosConfiguration mesosConfiguration;
  private final SingularityMesosTaskBuilder mesosTaskBuilder;
  private final SingularitySlaveAndRackManager slaveAndRackManager;
  private final SingularitySlaveAndRackHelper slaveAndRackHelper;
  private final SingularityTaskSizeOptimizer taskSizeOptimizer;
  private final SingularityUsageHelper usageHelper;
  private final UsageManager usageManager;
  private final DeployManager deployManager;
  private final SingularitySchedulerLock lock;
  private final SingularityLeaderCache leaderCache;
  private final boolean offerCacheEnabled;
  private final DisasterManager disasterManager;
  private final SingularityMesosSchedulerClient mesosSchedulerClient;
  private final OfferCache offerCache;

  private final double normalizedCpuWeight;
  private final double normalizedMemWeight;
  private final double normalizedDiskWeight;
  private final ExecutorService offerScoringExecutor;

  @Inject
  public SingularityMesosOfferScheduler(
    MesosConfiguration mesosConfiguration,
    CustomExecutorConfiguration customExecutorConfiguration,
    TaskManager taskManager,
    SingularityMesosTaskPrioritizer taskPrioritizer,
    SingularityScheduler scheduler,
    SingularityConfiguration configuration,
    SingularityMesosTaskBuilder mesosTaskBuilder,
    SingularitySlaveAndRackManager slaveAndRackManager,
    SingularityTaskSizeOptimizer taskSizeOptimizer,
    SingularitySlaveAndRackHelper slaveAndRackHelper,
    SingularityLeaderCache leaderCache,
    SingularityUsageHelper usageHelper,
    UsageManager usageManager,
    DeployManager deployManager,
    SingularitySchedulerLock lock,
    SingularityManagedThreadPoolFactory threadPoolFactory,
    DisasterManager disasterManager,
    SingularityMesosSchedulerClient mesosSchedulerClient,
    OfferCache offerCache
  ) {
    this.defaultResources =
      new Resources(
        mesosConfiguration.getDefaultCpus(),
        mesosConfiguration.getDefaultMemory(),
        0,
        mesosConfiguration.getDefaultDisk()
      );
    this.defaultCustomExecutorResources =
      new Resources(
        customExecutorConfiguration.getNumCpus(),
        customExecutorConfiguration.getMemoryMb(),
        0,
        customExecutorConfiguration.getDiskMb()
      );
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.mesosConfiguration = mesosConfiguration;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.slaveAndRackManager = slaveAndRackManager;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.leaderCache = leaderCache;
    this.offerCacheEnabled = configuration.isCacheOffers();
    this.disasterManager = disasterManager;
    this.mesosSchedulerClient = mesosSchedulerClient;
    this.offerCache = offerCache;
    this.usageHelper = usageHelper;
    this.slaveAndRackHelper = slaveAndRackHelper;
    this.taskPrioritizer = taskPrioritizer;
    this.usageManager = usageManager;
    this.deployManager = deployManager;
    this.lock = lock;

    double cpuWeight = mesosConfiguration.getCpuWeight();
    double memWeight = mesosConfiguration.getMemWeight();
    double diskWeight = mesosConfiguration.getDiskWeight();
    if (cpuWeight + memWeight + diskWeight != 1) {
      this.normalizedCpuWeight = cpuWeight / (cpuWeight + memWeight + diskWeight);
      this.normalizedMemWeight = memWeight / (cpuWeight + memWeight + diskWeight);
      this.normalizedDiskWeight = diskWeight / (cpuWeight + memWeight + diskWeight);
    } else {
      this.normalizedCpuWeight = cpuWeight;
      this.normalizedMemWeight = memWeight;
      this.normalizedDiskWeight = diskWeight;
    }
    this.offerScoringExecutor =
      threadPoolFactory.get("offer-scoring", configuration.getCoreThreadpoolSize());
  }

  public void resourceOffers(List<Offer> uncached) {
    final long start = System.currentTimeMillis();
    LOG.info("Received {} offer(s)", uncached.size());
    scheduler.checkForDecomissions();
    boolean declineImmediately = false;
    if (disasterManager.isDisabled(SingularityAction.PROCESS_OFFERS)) {
      LOG.info(
        "Processing offers is currently disabled, declining {} offers",
        uncached.size()
      );
      declineImmediately = true;
    }

    if (declineImmediately) {
      mesosSchedulerClient.decline(
        uncached.stream().map(Offer::getId).collect(Collectors.toList())
      );
      return;
    }

    if (offerCacheEnabled) {
      if (disasterManager.isDisabled(SingularityAction.CACHE_OFFERS)) {
        offerCache.disableOfferCache();
      } else {
        offerCache.enableOfferCache();
      }
    }

    Map<String, Offer> offersToCheck = uncached
      .stream()
      .filter(
        o -> {
          if (!isValidOffer(o)) {
            if (o.getId() != null && o.getId().getValue() != null) {
              LOG.warn("Got invalid offer {}", o);
              mesosSchedulerClient.decline(Collections.singletonList(o.getId()));
            } else {
              LOG.warn(
                "Offer {} was not valid, but we can't decline it because we have no offer ID!",
                o
              );
            }
            return false;
          }
          return true;
        }
      )
      .collect(
        Collectors.toConcurrentMap(o -> o.getId().getValue(), Function.identity())
      );

    List<CachedOffer> cachedOfferList = offerCache.checkoutOffers();
    Map<String, CachedOffer> cachedOffers = new ConcurrentHashMap<>();
    for (CachedOffer cachedOffer : cachedOfferList) {
      if (isValidOffer(cachedOffer.getOffer())) {
        cachedOffers.put(cachedOffer.getOfferId(), cachedOffer);
        offersToCheck.put(cachedOffer.getOfferId(), cachedOffer.getOffer());
      } else if (
        cachedOffer.getOffer().getId() != null &&
        cachedOffer.getOffer().getId().getValue() != null
      ) {
        mesosSchedulerClient.decline(
          Collections.singletonList(cachedOffer.getOffer().getId())
        );
        offerCache.rescindOffer(cachedOffer.getOffer().getId());
      } else {
        LOG.warn(
          "Offer {} was not valid, but we can't decline it because we have no offer ID!",
          cachedOffer
        );
      }
    }

    List<CompletableFuture<Void>> slaveCheckFutures = new ArrayList<>();
    uncached.forEach(
      offer ->
        slaveCheckFutures.add(runAsync(() -> checkOfferAndSlave(offer, offersToCheck)))
    );
    CompletableFutures.allOf(slaveCheckFutures).join();

    final Set<OfferID> acceptedOffers = Sets.newHashSetWithExpectedSize(
      offersToCheck.size()
    );

    try {
      Collection<SingularityOfferHolder> offerHolders = checkOffers(offersToCheck, start);

      for (SingularityOfferHolder offerHolder : offerHolders) {
        if (!offerHolder.getAcceptedTasks().isEmpty()) {
          List<Offer> leftoverOffers = offerHolder.launchTasksAndGetUnusedOffers(
            mesosSchedulerClient
          );

          leftoverOffers.forEach(
            o -> {
              if (cachedOffers.containsKey(o.getId().getValue())) {
                offerCache.returnOffer(cachedOffers.remove(o.getId().getValue()));
              } else {
                offerCache.cacheOffer(start, o);
              }
            }
          );

          List<Offer> offersAcceptedFromSlave = offerHolder.getOffers();
          offersAcceptedFromSlave.removeAll(leftoverOffers);
          offersAcceptedFromSlave
            .stream()
            .filter(offer -> cachedOffers.containsKey(offer.getId().getValue()))
            .map(o -> cachedOffers.remove(o.getId().getValue()))
            .forEach(offerCache::useOffer);
          acceptedOffers.addAll(
            offersAcceptedFromSlave
              .stream()
              .map(Offer::getId)
              .collect(Collectors.toList())
          );
        } else {
          offerHolder
            .getOffers()
            .forEach(
              o -> {
                if (cachedOffers.containsKey(o.getId().getValue())) {
                  offerCache.returnOffer(cachedOffers.remove(o.getId().getValue()));
                } else {
                  offerCache.cacheOffer(start, o);
                }
              }
            );
        }
      }

      LOG.info(
        "{} remaining offers not accounted for in offer check",
        cachedOffers.size()
      );
      cachedOffers.values().forEach(offerCache::returnOffer);
    } catch (Throwable t) {
      LOG.error(
        "Received fatal error while handling offers - will decline all available offers",
        t
      );

      mesosSchedulerClient.decline(
        offersToCheck
          .values()
          .stream()
          .filter(
            o -> {
              if (o == null || o.getId() == null || o.getId().getValue() == null) {
                LOG.warn("Got bad offer {} while trying to decline offers!", o);
                return false;
              }

              return true;
            }
          )
          .filter(
            o ->
              !acceptedOffers.contains(o.getId()) &&
              !cachedOffers.containsKey(o.getId().getValue())
          )
          .map(Offer::getId)
          .collect(Collectors.toList())
      );

      offersToCheck.forEach(
        (id, o) -> {
          if (cachedOffers.containsKey(id)) {
            offerCache.returnOffer(cachedOffers.get(id));
          }
        }
      );

      throw t;
    }

    LOG.info(
      "Finished handling {} new offer(s) {} from cache ({}), {} accepted, {} declined/cached",
      uncached.size(),
      cachedOffers.size(),
      JavaUtils.duration(start),
      acceptedOffers.size(),
      uncached.size() + cachedOffers.size() - acceptedOffers.size()
    );
  }

  private void checkOfferAndSlave(Offer offer, Map<String, Offer> offersToCheck) {
    String rolesInfo = MesosUtils.getRoles(offer).toString();
    LOG.debug(
      "Received offer ID {} with roles {} from {} ({}) for {} cpu(s), {} memory, {} ports, and {} disk",
      offer.getId().getValue(),
      rolesInfo,
      offer.getHostname(),
      offer.getAgentId().getValue(),
      MesosUtils.getNumCpus(offer),
      MesosUtils.getMemory(offer),
      MesosUtils.getNumPorts(offer),
      MesosUtils.getDisk(offer)
    );

    CheckResult checkResult = slaveAndRackManager.checkOffer(offer);
    if (checkResult == CheckResult.NOT_ACCEPTING_TASKS) {
      mesosSchedulerClient.decline(Collections.singletonList(offer.getId()));
      offersToCheck.remove(offer.getId().getValue());
      LOG.debug(
        "Will decline offer {}, slave {} is not currently in a state to launch tasks",
        offer.getId().getValue(),
        offer.getHostname()
      );
    }
  }

  private boolean isValidOffer(Offer offer) {
    if (offer.getId() == null || offer.getId().getValue() == null) {
      LOG.warn("Received offer with null ID, skipping ({})", offer);
      return false;
    }
    if (offer.getAgentId() == null || offer.getAgentId().getValue() == null) {
      LOG.warn("Received offer with null agent ID, skipping ({})", offer);
      return false;
    }
    return true;
  }

  Collection<SingularityOfferHolder> checkOffers(
    final Map<String, Offer> offers,
    long start
  ) {
    if (offers.isEmpty()) {
      LOG.debug("No offers to check");
      return Collections.emptyList();
    }

    final List<SingularityTaskRequestHolder> sortedTaskRequestHolders = getSortedDueTaskRequests();
    final int numDueTasks = sortedTaskRequestHolders.size();

    final Map<String, SingularityOfferHolder> offerHolders = offers
      .values()
      .stream()
      .collect(Collectors.groupingBy(o -> o.getAgentId().getValue()))
      .entrySet()
      .stream()
      .filter(e -> e.getValue().size() > 0)
      .map(
        e -> {
          List<Offer> offersList = e.getValue();
          String slaveId = e.getKey();
          return new SingularityOfferHolder(
            offersList,
            numDueTasks,
            slaveAndRackHelper.getRackIdOrDefault(offersList.get(0)),
            slaveId,
            offersList.get(0).getHostname(),
            slaveAndRackHelper.getTextAttributes(offersList.get(0)),
            slaveAndRackHelper.getReservedSlaveAttributes(offersList.get(0))
          );
        }
      )
      .collect(Collectors.toMap(SingularityOfferHolder::getSlaveId, Function.identity()));

    if (sortedTaskRequestHolders.isEmpty()) {
      return offerHolders.values();
    }

    final AtomicInteger tasksScheduled = new AtomicInteger(0);
    Map<String, RequestUtilization> requestUtilizations = usageManager.getRequestUtilizations(
      false
    );
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    Map<String, SingularitySlaveUsageWithId> currentSlaveUsages = usageManager.getAllCurrentSlaveUsage();

    List<CompletableFuture<Void>> currentSlaveUsagesFutures = new ArrayList<>();
    for (SingularityOfferHolder offerHolder : offerHolders.values()) {
      currentSlaveUsagesFutures.add(
        runAsync(
          () -> {
            String slaveId = offerHolder.getSlaveId();
            Optional<SingularitySlaveUsageWithId> maybeSlaveUsage = Optional.ofNullable(
              currentSlaveUsages.get(slaveId)
            );

            if (
              configuration.isReCheckMetricsForLargeNewTaskCount() &&
              maybeSlaveUsage.isPresent()
            ) {
              long newTaskCount = taskManager
                .getActiveTaskIds()
                .stream()
                .filter(
                  t ->
                    t.getStartedAt() > maybeSlaveUsage.get().getTimestamp() &&
                    t.getSanitizedHost().equals(offerHolder.getSanitizedHost())
                )
                .count();
              if (newTaskCount >= maybeSlaveUsage.get().getNumTasks() / 2) {
                try {
                  MesosSlaveMetricsSnapshotObject metricsSnapshot = usageHelper.getMetricsSnapshot(
                    offerHolder.getHostname()
                  );

                  if (
                    metricsSnapshot.getSystemLoad5Min() /
                    metricsSnapshot.getSystemCpusTotal() >
                    mesosConfiguration.getRecheckMetricsLoad1Threshold() ||
                    metricsSnapshot.getSystemLoad1Min() /
                    metricsSnapshot.getSystemCpusTotal() >
                    mesosConfiguration.getRecheckMetricsLoad5Threshold()
                  ) {
                    // Come back to this slave after we have collected more metrics
                    LOG.info(
                      "Skipping evaluation of {} until new metrics are collected. Current load is load1: {}, load5: {}",
                      offerHolder.getHostname(),
                      metricsSnapshot.getSystemLoad1Min(),
                      metricsSnapshot.getSystemLoad5Min()
                    );
                    currentSlaveUsages.remove(slaveId);
                  }
                } catch (Throwable t) {
                  LOG.warn(
                    "Could not check metrics for host {}, skipping",
                    offerHolder.getHostname()
                  );
                  currentSlaveUsages.remove(slaveId);
                }
              }
            }
          }
        )
      );
    }
    CompletableFutures.allOf(currentSlaveUsagesFutures).join();

    List<CompletableFuture<Void>> usagesWithScoresFutures = new ArrayList<>();
    Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId = new ConcurrentHashMap<>();
    for (SingularitySlaveUsageWithId usage : currentSlaveUsages.values()) {
      if (offerHolders.containsKey(usage.getSlaveId())) {
        usagesWithScoresFutures.add(
          runAsync(
            () ->
              currentSlaveUsagesBySlaveId.put(
                usage.getSlaveId(),
                new SingularitySlaveUsageWithCalculatedScores(
                  usage,
                  mesosConfiguration.getScoreUsingSystemLoad(),
                  getMaxProbableUsageForSlave(
                    activeTaskIds,
                    requestUtilizations,
                    offerHolders.get(usage.getSlaveId()).getSanitizedHost()
                  ),
                  mesosConfiguration.getLoad5OverloadedThreshold(),
                  mesosConfiguration.getLoad1OverloadedThreshold(),
                  usage.getTimestamp()
                )
              )
          )
        );
      }
    }

    CompletableFutures.allOf(usagesWithScoresFutures).join();

    long startCheck = System.currentTimeMillis();
    LOG.debug("Found slave usages and scores after {}ms", startCheck - start);

    Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache = new ConcurrentHashMap<>();
    Set<String> overloadedHosts = Sets.newConcurrentHashSet();
    AtomicInteger noMatches = new AtomicInteger();

    // We spend much of the offer check loop for request level locks. Wait for the locks in parallel, but ensure that actual offer checks
    // are done in serial to not over commit a single offer
    ReentrantLock offerCheckTempLock = new ReentrantLock(true);
    CompletableFutures
      .allOf(
        sortedTaskRequestHolders
          .stream()
          .collect(Collectors.groupingBy(t -> t.getTaskRequest().getRequest().getId()))
          .entrySet()
          .stream()
          .map(
            entry ->
              runAsync(
                () -> {
                  lock.runWithRequestLock(
                    () -> {
                      offerCheckTempLock.lock();
                      try {
                        for (SingularityTaskRequestHolder taskRequestHolder : entry.getValue()) {
                          List<SingularityTaskId> activeTaskIdsForRequest = leaderCache.getActiveTaskIdsForRequest(
                            taskRequestHolder.getTaskRequest().getRequest().getId()
                          );
                          if (
                            isTooManyInstancesForRequest(
                              taskRequestHolder.getTaskRequest(),
                              activeTaskIdsForRequest
                            )
                          ) {
                            LOG.debug(
                              "Skipping pending task {}, too many instances already running",
                              taskRequestHolder
                                .getTaskRequest()
                                .getPendingTask()
                                .getPendingTaskId()
                            );
                            continue;
                          }

                          Map<String, Double> scorePerOffer = new ConcurrentHashMap<>();

                          for (SingularityOfferHolder offerHolder : offerHolders.values()) {
                            if (!isOfferFull(offerHolder)) {
                              if (
                                calculateScore(
                                  requestUtilizations,
                                  currentSlaveUsagesBySlaveId,
                                  taskRequestHolder,
                                  scorePerOffer,
                                  activeTaskIdsForRequest,
                                  offerHolder,
                                  deployStatsCache,
                                  overloadedHosts
                                ) >
                                mesosConfiguration.getGoodEnoughScoreThreshold()
                              ) {
                                break;
                              }
                            }
                          }

                          if (!scorePerOffer.isEmpty()) {
                            SingularityOfferHolder bestOffer = offerHolders.get(
                              Collections
                                .max(
                                  scorePerOffer.entrySet(),
                                  Map.Entry.comparingByValue()
                                )
                                .getKey()
                            );
                            LOG.info(
                              "Best offer {}/1 is on {}",
                              scorePerOffer.get(bestOffer.getSlaveId()),
                              bestOffer.getSanitizedHost()
                            );
                            SingularityMesosTaskHolder taskHolder = acceptTask(
                              bestOffer,
                              taskRequestHolder
                            );
                            tasksScheduled.getAndIncrement();
                            bestOffer.addMatchedTask(taskHolder);
                            updateSlaveUsageScores(
                              taskRequestHolder,
                              currentSlaveUsagesBySlaveId,
                              bestOffer.getSlaveId(),
                              requestUtilizations
                            );
                          } else {
                            noMatches.getAndIncrement();
                          }
                        }
                      } finally {
                        offerCheckTempLock.unlock();
                      }
                    },
                    entry.getKey(),
                    String.format("%s#%s", getClass().getSimpleName(), "checkOffers")
                  );
                }
              )
          )
          .collect(Collectors.toList())
      )
      .join();

    LOG.info(
      "{} tasks scheduled, {} tasks remaining after examining {} offers ({} overloaded hosts, {} had no offer matches)",
      tasksScheduled,
      numDueTasks - tasksScheduled.get(),
      offers.size(),
      overloadedHosts.size(),
      noMatches.get()
    );

    return offerHolders.values();
  }

  private CompletableFuture<Void> runAsync(Runnable runnable) {
    return CompletableFuture.runAsync(runnable, offerScoringExecutor);
  }

  private double calculateScore(
    Map<String, RequestUtilization> requestUtilizations,
    Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId,
    SingularityTaskRequestHolder taskRequestHolder,
    Map<String, Double> scorePerOffer,
    List<SingularityTaskId> activeTaskIdsForRequest,
    SingularityOfferHolder offerHolder,
    Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache,
    Set<String> overloadedHosts
  ) {
    String slaveId = offerHolder.getSlaveId();

    double score = calculateScore(
      offerHolder,
      currentSlaveUsagesBySlaveId,
      taskRequestHolder,
      activeTaskIdsForRequest,
      requestUtilizations.get(taskRequestHolder.getTaskRequest().getRequest().getId()),
      deployStatsCache,
      overloadedHosts
    );
    if (score != 0) {
      scorePerOffer.put(slaveId, score);
    }
    return score;
  }

  private MaxProbableUsage getMaxProbableUsageForSlave(
    List<SingularityTaskId> activeTaskIds,
    Map<String, RequestUtilization> requestUtilizations,
    String sanitizedHostname
  ) {
    double cpu = 0;
    double memBytes = 0;
    double diskBytes = 0;
    for (SingularityTaskId taskId : activeTaskIds) {
      if (taskId.getSanitizedHost().equals(sanitizedHostname)) {
        if (requestUtilizations.containsKey(taskId.getRequestId())) {
          RequestUtilization utilization = requestUtilizations.get(taskId.getRequestId());
          cpu += slaveAndRackHelper.getEstimatedCpuUsageForRequest(utilization);
          memBytes += utilization.getMaxMemBytesUsed();
          diskBytes += utilization.getMaxDiskBytesUsed();
        } else {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
          if (maybeTask.isPresent()) {
            Resources resources = maybeTask
              .get()
              .getTaskRequest()
              .getPendingTask()
              .getResources()
              .orElse(
                maybeTask
                  .get()
                  .getTaskRequest()
                  .getDeploy()
                  .getResources()
                  .orElse(defaultResources)
              );
            cpu += resources.getCpus();
            memBytes +=
              resources.getMemoryMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE;
            diskBytes += resources.getDiskMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE;
          }
        }
      }
    }
    return new MaxProbableUsage(cpu, memBytes, diskBytes);
  }

  private boolean isOfferFull(SingularityOfferHolder offerHolder) {
    return (
      configuration.getMaxTasksPerOffer() > 0 &&
      offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer()
    );
  }

  private void updateSlaveUsageScores(
    SingularityTaskRequestHolder taskHolder,
    Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId,
    String slaveId,
    Map<String, RequestUtilization> requestUtilizations
  ) {
    Optional<SingularitySlaveUsageWithCalculatedScores> maybeUsage = Optional.ofNullable(
      currentSlaveUsagesBySlaveId.get(slaveId)
    );
    if (maybeUsage.isPresent() && !maybeUsage.get().isMissingUsageData()) {
      SingularitySlaveUsageWithCalculatedScores usage = maybeUsage.get();
      usage.addEstimatedCpuReserved(taskHolder.getTotalResources().getCpus());
      usage.addEstimatedMemoryReserved(taskHolder.getTotalResources().getMemoryMb());
      usage.addEstimatedDiskReserved(taskHolder.getTotalResources().getDiskMb());
      if (
        requestUtilizations.containsKey(taskHolder.getTaskRequest().getRequest().getId())
      ) {
        RequestUtilization requestUtilization = requestUtilizations.get(
          taskHolder.getTaskRequest().getRequest().getId()
        );
        usage.addEstimatedCpuUsage(requestUtilization.getMaxCpuUsed());
        usage.addEstimatedMemoryBytesUsage(requestUtilization.getMaxMemBytesUsed());
        usage.addEstimatedDiskBytesUsage(requestUtilization.getMaxDiskBytesUsed());
      } else {
        usage.addEstimatedCpuUsage(taskHolder.getTotalResources().getCpus());
        usage.addEstimatedMemoryBytesUsage(
          taskHolder.getTotalResources().getMemoryMb() *
          SingularitySlaveUsage.BYTES_PER_MEGABYTE
        );
        usage.addEstimatedDiskBytesUsage(
          taskHolder.getTotalResources().getDiskMb() *
          SingularitySlaveUsage.BYTES_PER_MEGABYTE
        );
      }
      usage.recalculateScores();
    }
  }

  private double calculateScore(
    SingularityOfferHolder offerHolder,
    Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId,
    SingularityTaskRequestHolder taskRequestHolder,
    List<SingularityTaskId> activeTaskIdsForRequest,
    RequestUtilization requestUtilization,
    Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache,
    Set<String> overloadedHosts
  ) {
    Optional<SingularitySlaveUsageWithCalculatedScores> maybeSlaveUsage = Optional.ofNullable(
      currentSlaveUsagesBySlaveId.get(offerHolder.getSlaveId())
    );
    double score = score(
      offerHolder,
      taskRequestHolder,
      maybeSlaveUsage,
      activeTaskIdsForRequest,
      requestUtilization,
      deployStatsCache,
      overloadedHosts
    );
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "Scored {} | Task {} | Offer - mem {} - cpu {} | Slave {} | maybeSlaveUsage - {}",
        score,
        taskRequestHolder.getTaskRequest().getPendingTask().getPendingTaskId().getId(),
        MesosUtils.getMemory(offerHolder.getCurrentResources(), Optional.empty()),
        MesosUtils.getNumCpus(offerHolder.getCurrentResources(), Optional.empty()),
        offerHolder.getHostname(),
        maybeSlaveUsage
      );
    }
    return score;
  }

  private List<SingularityTaskRequestHolder> getSortedDueTaskRequests() {
    final List<SingularityTaskRequest> taskRequests = taskPrioritizer.getSortedDueTasks(
      scheduler.getDueTasks()
    );

    taskRequests.forEach(
      taskRequest ->
        LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId())
    );

    taskPrioritizer.removeTasksAffectedByPriorityFreeze(taskRequests);

    return taskRequests
      .stream()
      .map(
        taskRequest ->
          new SingularityTaskRequestHolder(
            taskRequest,
            defaultResources,
            defaultCustomExecutorResources
          )
      )
      .collect(Collectors.toList());
  }

  private double score(
    SingularityOfferHolder offerHolder,
    SingularityTaskRequestHolder taskRequestHolder,
    Optional<SingularitySlaveUsageWithCalculatedScores> maybeSlaveUsage,
    List<SingularityTaskId> activeTaskIdsForRequest,
    RequestUtilization requestUtilization,
    Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache,
    Set<String> overloadedHosts
  ) {
    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityPendingTaskId pendingTaskId = taskRequest
      .getPendingTask()
      .getPendingTaskId();

    double estimatedCpusToAdd = taskRequestHolder.getTotalResources().getCpus();
    if (requestUtilization != null) {
      estimatedCpusToAdd =
        slaveAndRackHelper.getEstimatedCpuUsageForRequest(requestUtilization);
    }
    if (
      mesosConfiguration.isOmitOverloadedHosts() &&
      maybeSlaveUsage.isPresent() &&
      maybeSlaveUsage.get().isCpuOverloaded(estimatedCpusToAdd)
    ) {
      overloadedHosts.add(offerHolder.getHostname());
      LOG.debug(
        "Slave {} is overloaded (load5 {}/{}, load1 {}/{}, estimated cpus to add: {}, already committed cpus: {}), ignoring offer",
        offerHolder.getHostname(),
        maybeSlaveUsage.get().getSlaveUsage().getSystemLoad5Min(),
        maybeSlaveUsage.get().getSlaveUsage().getSystemCpusTotal(),
        maybeSlaveUsage.get().getSlaveUsage().getSystemLoad1Min(),
        maybeSlaveUsage.get().getSlaveUsage().getSystemCpusTotal(),
        estimatedCpusToAdd,
        maybeSlaveUsage.get().getEstimatedAddedCpusUsage()
      );
      return 0;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}",
        pendingTaskId,
        taskRequestHolder.getTotalResources(),
        taskRequest.getRequest().getRequiredRole().orElse("*"),
        taskRequestHolder.getTaskResources(),
        taskRequestHolder.getExecutorResources(),
        MesosUtils.formatForLogging(offerHolder.getCurrentResources())
      );
    }

    final boolean matchesResources = MesosUtils.doesOfferMatchResources(
      taskRequest.getRequest().getRequiredRole(),
      taskRequestHolder.getTotalResources(),
      offerHolder.getCurrentResources(),
      taskRequestHolder.getRequestedPorts()
    );
    if (!matchesResources) {
      return 0;
    }
    final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(
      offerHolder,
      taskRequest,
      activeTaskIdsForRequest,
      isPreemptibleTask(taskRequest, deployStatsCache),
      requestUtilization
    );

    if (slaveMatchState.isMatchAllowed()) {
      return score(offerHolder.getHostname(), maybeSlaveUsage, slaveMatchState);
    } else if (LOG.isTraceEnabled()) {
      LOG.trace(
        "Ignoring offer on host {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}",
        offerHolder.getHostname(),
        offerHolder.getRoles(),
        offerHolder.getHostname(),
        pendingTaskId,
        matchesResources,
        slaveMatchState
      );
    }

    return 0;
  }

  private boolean isPreemptibleTask(
    SingularityTaskRequest taskRequest,
    Map<SingularityDeployKey, Optional<SingularityDeployStatistics>> deployStatsCache
  ) {
    // A long running task can be replaced + killed easily
    if (taskRequest.getRequest().getRequestType().isLongRunning()) {
      return true;
    }

    // A short, non-long-running task
    Optional<SingularityDeployStatistics> deployStatistics = deployStatsCache.computeIfAbsent(
      new SingularityDeployKey(
        taskRequest.getRequest().getId(),
        taskRequest.getDeploy().getId()
      ),
      key ->
        deployManager.getDeployStatistics(
          taskRequest.getRequest().getId(),
          taskRequest.getDeploy().getId()
        )
    );
    return (
      deployStatistics.isPresent() &&
      deployStatistics.get().getAverageRuntimeMillis().isPresent() &&
      deployStatistics.get().getAverageRuntimeMillis().get() <
      configuration.getPreemptibleTaskMaxExpectedRuntimeMs()
    );
  }

  @VisibleForTesting
  double score(
    String hostname,
    Optional<SingularitySlaveUsageWithCalculatedScores> maybeSlaveUsage,
    SlaveMatchState slaveMatchState
  ) {
    if (!maybeSlaveUsage.isPresent() || maybeSlaveUsage.get().isMissingUsageData()) {
      if (mesosConfiguration.isOmitForMissingUsageData()) {
        LOG.info(
          "Skipping slave {} with missing usage data ({})",
          hostname,
          maybeSlaveUsage
        );
        return 0.0;
      } else {
        LOG.info(
          "Slave {} has missing usage data ({}). Will default to {}",
          hostname,
          maybeSlaveUsage,
          0.5
        );
        return 0.5;
      }
    }

    SingularitySlaveUsageWithCalculatedScores slaveUsageWithScores = maybeSlaveUsage.get();

    double calculatedScore = calculateScore(
      1 - slaveUsageWithScores.getMemAllocatedScore(),
      slaveUsageWithScores.getMemInUseScore(),
      1 - slaveUsageWithScores.getCpusAllocatedScore(),
      slaveUsageWithScores.getCpusInUseScore(),
      1 - slaveUsageWithScores.getDiskAllocatedScore(),
      slaveUsageWithScores.getDiskInUseScore(),
      mesosConfiguration.getInUseResourceWeight(),
      mesosConfiguration.getAllocatedResourceWeight()
    );

    if (slaveMatchState == SlaveMatchState.PREFERRED_SLAVE) {
      LOG.debug(
        "Slave {} is preferred, will scale score by {}",
        hostname,
        configuration.getPreferredSlaveScaleFactor()
      );
      calculatedScore *= configuration.getPreferredSlaveScaleFactor();
    }

    return calculatedScore;
  }

  private double calculateScore(
    double memAllocatedScore,
    double memInUseScore,
    double cpusAllocatedScore,
    double cpusInUseScore,
    double diskAllocatedScore,
    double diskInUseScore,
    double inUseResourceWeight,
    double allocatedResourceWeight
  ) {
    double score = 0;

    score += (normalizedCpuWeight * allocatedResourceWeight) * cpusAllocatedScore;
    score += (normalizedMemWeight * allocatedResourceWeight) * memAllocatedScore;
    score += (normalizedDiskWeight * allocatedResourceWeight) * diskAllocatedScore;

    score += (normalizedCpuWeight * inUseResourceWeight) * cpusInUseScore;
    score += (normalizedMemWeight * inUseResourceWeight) * memInUseScore;
    score += (normalizedDiskWeight * inUseResourceWeight) * diskInUseScore;

    return score;
  }

  private SingularityMesosTaskHolder acceptTask(
    SingularityOfferHolder offerHolder,
    SingularityTaskRequestHolder taskRequestHolder
  ) {
    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityMesosTaskHolder taskHolder = mesosTaskBuilder.buildTask(
      offerHolder,
      offerHolder.getCurrentResources(),
      taskRequest,
      taskRequestHolder.getTaskResources(),
      taskRequestHolder.getExecutorResources()
    );

    final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(taskHolder);

    LOG.trace("Accepted and built task {}", zkTask);
    LOG.info(
      "Launching task {} slot on slave {} ({})",
      taskHolder.getTask().getTaskId(),
      offerHolder.getSlaveId(),
      offerHolder.getHostname()
    );

    taskManager.createTaskAndDeletePendingTask(zkTask);
    return taskHolder;
  }

  private boolean isTooManyInstancesForRequest(
    SingularityTaskRequest taskRequest,
    List<SingularityTaskId> activeTaskIdsForRequest
  ) {
    if (taskRequest.getRequest().getRequestType() == RequestType.ON_DEMAND) {
      int maxActiveOnDemandTasks = taskRequest
        .getRequest()
        .getInstances()
        .orElse(configuration.getMaxActiveOnDemandTasksPerRequest());
      if (maxActiveOnDemandTasks > 0) {
        int activeTasksForRequest = activeTaskIdsForRequest.size();
        LOG.debug(
          "Running {} instances for request {}. Max is {}",
          activeTasksForRequest,
          taskRequest.getRequest().getId(),
          maxActiveOnDemandTasks
        );
        if (activeTasksForRequest >= maxActiveOnDemandTasks) {
          return true;
        }
      }
    }

    return false;
  }
}
