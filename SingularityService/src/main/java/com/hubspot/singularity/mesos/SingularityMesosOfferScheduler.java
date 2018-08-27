package com.hubspot.singularity.mesos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import org.apache.mesos.v1.Protos.Offer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.Resources;
import com.hubspot.mesos.json.MesosSlaveMetricsSnapshotObject;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SlaveMatchState;
import com.hubspot.singularity.async.AsyncSemaphore;
import com.hubspot.singularity.async.CompletableFutures;
import com.hubspot.singularity.config.CustomExecutorConfiguration;
import com.hubspot.singularity.config.MesosConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.UsageManager;
import com.hubspot.singularity.helpers.MesosUtils;
import com.hubspot.singularity.helpers.SingularityMesosTaskHolder;
import com.hubspot.singularity.mesos.SingularitySlaveUsageWithCalculatedScores.MaxProbableUsage;
import com.hubspot.singularity.scheduler.SingularityLeaderCache;
import com.hubspot.singularity.scheduler.SingularityScheduler;
import com.hubspot.singularity.scheduler.SingularityUsageHelper;

@Singleton
public class SingularityMesosOfferScheduler {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityMesosOfferScheduler.class);

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

  private final double normalizedCpuWeight;
  private final double normalizedMemWeight;
  private final double normalizedDiskWeight;

  private final AsyncSemaphore<Void> offerScoringSemaphore;
  private final ExecutorService offerScoringExecutor;

  @Inject
  public SingularityMesosOfferScheduler(MesosConfiguration mesosConfiguration,
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
                                        SingularitySchedulerLock lock) {
    this.defaultResources = new Resources(mesosConfiguration.getDefaultCpus(), mesosConfiguration.getDefaultMemory(), 0, mesosConfiguration.getDefaultDisk());
    this.defaultCustomExecutorResources = new Resources(customExecutorConfiguration.getNumCpus(), customExecutorConfiguration.getMemoryMb(), 0, customExecutorConfiguration.getDiskMb());
    this.taskManager = taskManager;
    this.scheduler = scheduler;
    this.configuration = configuration;
    this.mesosConfiguration = mesosConfiguration;
    this.mesosTaskBuilder = mesosTaskBuilder;
    this.slaveAndRackManager = slaveAndRackManager;
    this.taskSizeOptimizer = taskSizeOptimizer;
    this.leaderCache = leaderCache;
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

    this.offerScoringSemaphore = AsyncSemaphore.newBuilder(mesosConfiguration::getOffersConcurrencyLimit).setFlushQueuePeriodically(true).build();
    this.offerScoringExecutor = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("offer-scoring-%d").build());
  }

  public Collection<SingularityOfferHolder> checkOffers(final Collection<Offer> offers) {
    for (SingularityPendingTaskId taskId : taskManager.getPendingTasksMarkedForDeletion()) {
      lock.runWithRequestLock(() -> taskManager.deletePendingTask(taskId), taskId.getRequestId(), String.format("%s#%s", getClass().getSimpleName(), "checkOffers -> pendingTaskDeletes"));
    }

    scheduler.checkForDecomissions();
    scheduler.drainPendingQueue();

    if (offers.isEmpty()) {
      LOG.debug("No offers to check");
      return Collections.emptyList();
    }

    final List<SingularityTaskRequestHolder> sortedTaskRequestHolders = getSortedDueTaskRequests();
    final int numDueTasks = sortedTaskRequestHolders.size();
    Set<String> relevantRequestIds = new HashSet<>();

    final Map<String, SingularityOfferHolder> offerHolders = offers.stream()
        .collect(Collectors.groupingBy((o) -> o.getAgentId().getValue()))
        .entrySet().stream()
        .filter((e) -> e.getValue().size() > 0)
        .map((e) -> {
          List<Offer> offersList = e.getValue();
          String slaveId = e.getKey();
          return new SingularityOfferHolder(
              offersList,
              numDueTasks,
              slaveAndRackHelper.getRackIdOrDefault(offersList.get(0)),
              slaveId,
              offersList.get(0).getHostname(),
              slaveAndRackHelper.getTextAttributes(offersList.get(0)),
              slaveAndRackHelper.getReservedSlaveAttributes(offersList.get(0)));
        })
        .peek((offerHolder) -> {
          taskManager.getActiveTaskIds().forEach((t) -> {
            if (t.getSanitizedHost().equals(offerHolder.getSanitizedHost())) {
              relevantRequestIds.add(t.getRequestId());
            }
          });
        })
        .collect(Collectors.toMap(SingularityOfferHolder::getSlaveId, Function.identity()));

    if (sortedTaskRequestHolders.isEmpty()) {
      return offerHolders.values();
    }

    final AtomicInteger tasksScheduled = new AtomicInteger(0);
    Map<String, RequestUtilization> requestUtilizations = usageManager.getRequestUtilizations(false);
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIds();

    Map<String, SingularitySlaveUsageWithId> currentSlaveUsages = usageManager.getCurrentSlaveUsages(
        offerHolders.values()
            .stream()
            .map(SingularityOfferHolder::getSlaveId)
            .collect(Collectors.toList()))
        .stream()
        .collect(Collectors.toMap(SingularitySlaveUsageWithId::getSlaveId, Function.identity()));

    List<CompletableFuture<Void>> currentSlaveUsagesFutures = new ArrayList<>();
    for (SingularityOfferHolder offerHolder : offerHolders.values()) {
      currentSlaveUsagesFutures.add(offerScoringSemaphore.call(() -> CompletableFuture.runAsync(() -> {
        String slaveId = offerHolder.getSlaveId();
        Optional<SingularitySlaveUsageWithId> maybeSlaveUsage = Optional.fromNullable(currentSlaveUsages.get(slaveId));

        if (configuration.isReCheckMetricsForLargeNewTaskCount() && maybeSlaveUsage.isPresent()) {
          long newTaskCount = taskManager.getActiveTaskIds().stream()
              .filter((t) -> t.getStartedAt() > maybeSlaveUsage.get().getTimestamp() && t.getSanitizedHost().equals(offerHolder.getSanitizedHost()))
              .count();
          if (newTaskCount >= maybeSlaveUsage.get().getNumTasks() / 2) {
            try {
              MesosSlaveMetricsSnapshotObject metricsSnapshot = usageHelper.getMetricsSnapshot(offerHolder.getHostname());

              if (metricsSnapshot.getSystemLoad5Min() / metricsSnapshot.getSystemCpusTotal() > mesosConfiguration.getRecheckMetricsLoad1Threshold()
                  || metricsSnapshot.getSystemLoad1Min() / metricsSnapshot.getSystemCpusTotal() > mesosConfiguration.getRecheckMetricsLoad5Threshold()) {
                // Come back to this slave after we have collected more metrics
                LOG.info("Skipping evaluation of {} until new metrics are collected. Current load is load1: {}, load5: {}", offerHolder.getHostname(), metricsSnapshot.getSystemLoad1Min(), metricsSnapshot
                    .getSystemLoad5Min());
                currentSlaveUsages.remove(slaveId);
              }
            } catch (Throwable t) {
              LOG.warn("Could not check metrics for host {}, skipping", offerHolder.getHostname());
              currentSlaveUsages.remove(slaveId);
            }
          }

        }
      }, offerScoringExecutor)));
    }
    CompletableFutures.allOf(currentSlaveUsagesFutures).join();

    List<CompletableFuture<Void>> usagesWithScoresFutures = new ArrayList<>();
    Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId = new ConcurrentHashMap<>();
    for (SingularitySlaveUsageWithId usage : currentSlaveUsages.values()) {
      usagesWithScoresFutures.add(offerScoringSemaphore.call(() ->
          CompletableFuture.runAsync(() -> currentSlaveUsagesBySlaveId.put(usage.getSlaveId(),
              new SingularitySlaveUsageWithCalculatedScores(
                usage,
                mesosConfiguration.getScoreUsingSystemLoad(),
                getMaxProbableUsageForSlave(activeTaskIds, requestUtilizations, offerHolders.get(usage.getSlaveId()).getSanitizedHost()),
                mesosConfiguration.getLoad5OverloadedThreshold(),
                mesosConfiguration.getLoad1OverloadedThreshold(),
                usage.getTimestamp())),
              offerScoringExecutor))
      );
    }

    CompletableFutures.allOf(usagesWithScoresFutures).join();

    LOG.trace("Found slave usages {}", currentSlaveUsagesBySlaveId);

    Map<String, Integer> tasksPerOfferHost = new ConcurrentHashMap<>();

    for (SingularityTaskRequestHolder taskRequestHolder : sortedTaskRequestHolders) {
      lock.runWithRequestLock(() -> {
        Map<String, Double> scorePerOffer = new ConcurrentHashMap<>();
        List<SingularityTaskId> activeTaskIdsForRequest = leaderCache.getActiveTaskIdsForRequest(taskRequestHolder.getTaskRequest().getRequest().getId());

        List<CompletableFuture<Void>> scoringFutures = new ArrayList<>();
        AtomicReference<Throwable> scoringException = new AtomicReference<>(null);
        for (SingularityOfferHolder offerHolder : offerHolders.values()) {
          scoringFutures.add(offerScoringSemaphore.call(() ->
              CompletableFuture.supplyAsync(() -> {
                return calculateScore(requestUtilizations, currentSlaveUsagesBySlaveId, tasksPerOfferHost, taskRequestHolder, scorePerOffer, activeTaskIdsForRequest, scoringException, offerHolder);
              },
              offerScoringExecutor)));
        }

        CompletableFutures.allOf(scoringFutures).join();

        if (scoringException.get() != null) {
          LOG.warn("Exception caught in offer scoring futures, semaphore info: (concurrentRequests: {}, queueSize: {})",
              offerScoringSemaphore.getConcurrentRequests(), offerScoringSemaphore.getQueueSize());
          // This will be caught by either the LeaderOnlyPoller or resourceOffers uncaught exception code, causing an abort
          throw new RuntimeException(scoringException.get());
        }

        if (!scorePerOffer.isEmpty()) {
          SingularityOfferHolder bestOffer = offerHolders.get(Collections.max(scorePerOffer.entrySet(), Map.Entry.comparingByValue()).getKey());
          LOG.info("Best offer {}/1 is on {}", scorePerOffer.get(bestOffer.getSlaveId()), bestOffer.getSanitizedHost());
          SingularityMesosTaskHolder taskHolder = acceptTask(bestOffer, tasksPerOfferHost, taskRequestHolder);
          tasksScheduled.getAndIncrement();
          bestOffer.addMatchedTask(taskHolder);
          updateSlaveUsageScores(taskRequestHolder, currentSlaveUsagesBySlaveId, bestOffer.getSlaveId(), requestUtilizations);
        }
      }, taskRequestHolder.getTaskRequest().getRequest().getId(), String.format("%s#%s", getClass().getSimpleName(), "checkOffers"));
    }

    LOG.info("{} tasks scheduled, {} tasks remaining after examining {} offers", tasksScheduled, numDueTasks - tasksScheduled.get(), offers.size());

    return offerHolders.values();
  }

  private Void calculateScore(
      Map<String, RequestUtilization> requestUtilizations,
      Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId,
      Map<String, Integer> tasksPerOfferHost,
      SingularityTaskRequestHolder taskRequestHolder,
      Map<String, Double> scorePerOffer,
      List<SingularityTaskId> activeTaskIdsForRequest,
      AtomicReference<Throwable> scoringException,
      SingularityOfferHolder offerHolder) {
    if (isOfferFull(offerHolder)) {
      return null;
    }
    String slaveId = offerHolder.getSlaveId();

    try {
      double score = calculateScore(offerHolder, currentSlaveUsagesBySlaveId, tasksPerOfferHost, taskRequestHolder, activeTaskIdsForRequest, requestUtilizations.get(taskRequestHolder.getTaskRequest().getRequest().getId()));
      if (score != 0) {
        scorePerOffer.put(slaveId, score);
      }
    } catch (Throwable t) {
      LOG.error("Uncaught exception while scoring offers", t);
      scoringException.set(t);
    }
    return null;
  }

  private MaxProbableUsage getMaxProbableUsageForSlave(List<SingularityTaskId> activeTaskIds, Map<String, RequestUtilization> requestUtilizations, String sanitizedHostname) {
    double cpu = 0;
    double memBytes = 0;
    double diskBytes = 0;
    for (SingularityTaskId taskId : activeTaskIds) {
      if (taskId.getSanitizedHost().equals(sanitizedHostname)) {
        if (requestUtilizations.containsKey(taskId.getRequestId())) {
          RequestUtilization utilization = requestUtilizations.get(taskId.getRequestId());
          cpu += getEstimatedCpuUsageForRequest(utilization);
          memBytes += utilization.getMaxMemBytesUsed();
          diskBytes += utilization.getMaxDiskBytesUsed();
        } else {
          Optional<SingularityTask> maybeTask = taskManager.getTask(taskId);
          if (maybeTask.isPresent()) {
            Resources resources = maybeTask.get().getTaskRequest().getPendingTask().getResources()
                .or(maybeTask.get().getTaskRequest().getDeploy().getResources())
                .or(defaultResources);
            cpu += resources.getCpus();
            memBytes += resources.getMemoryMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE;
            diskBytes += resources.getDiskMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE;
          }
        }
      }
    }
    return new MaxProbableUsage(cpu, memBytes, diskBytes);
  }

  private boolean isOfferFull(SingularityOfferHolder offerHolder) {
    return configuration.getMaxTasksPerOffer() > 0 && offerHolder.getAcceptedTasks().size() >= configuration.getMaxTasksPerOffer();
  }

  private void updateSlaveUsageScores(SingularityTaskRequestHolder taskHolder, Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId, String slaveId, Map<String, RequestUtilization> requestUtilizations) {
    Optional<SingularitySlaveUsageWithCalculatedScores> maybeUsage = Optional.fromNullable(currentSlaveUsagesBySlaveId.get(slaveId));
    if (maybeUsage.isPresent() && !maybeUsage.get().isMissingUsageData()) {
      SingularitySlaveUsageWithCalculatedScores usage = maybeUsage.get();
      usage.addEstimatedCpuReserved(taskHolder.getTotalResources().getCpus());
      usage.addEstimatedMemoryReserved(taskHolder.getTotalResources().getMemoryMb());
      usage.addEstimatedDiskReserved(taskHolder.getTotalResources().getDiskMb());
      if (requestUtilizations.containsKey(taskHolder.getTaskRequest().getRequest().getId())) {
        RequestUtilization requestUtilization = requestUtilizations.get(taskHolder.getTaskRequest().getRequest().getId());
        usage.addEstimatedCpuUsage(requestUtilization.getMaxCpuUsed());
        usage.addEstimatedMemoryBytesUsage(requestUtilization.getMaxMemBytesUsed());
        usage.addEstimatedDiskBytesUsage(requestUtilization.getMaxDiskBytesUsed());
      } else {
        usage.addEstimatedCpuUsage(taskHolder.getTotalResources().getCpus());
        usage.addEstimatedMemoryBytesUsage(taskHolder.getTotalResources().getMemoryMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE);
        usage.addEstimatedDiskBytesUsage(taskHolder.getTotalResources().getDiskMb() * SingularitySlaveUsage.BYTES_PER_MEGABYTE);
      }
      usage.recalculateScores();
    }
  }

  private double calculateScore(SingularityOfferHolder offerHolder, Map<String, SingularitySlaveUsageWithCalculatedScores> currentSlaveUsagesBySlaveId, Map<String, Integer> tasksPerOffer,
                                SingularityTaskRequestHolder taskRequestHolder, List<SingularityTaskId> activeTaskIdsForRequest, RequestUtilization requestUtilization) {
    Optional<SingularitySlaveUsageWithCalculatedScores> maybeSlaveUsage = Optional.fromNullable(currentSlaveUsagesBySlaveId.get(offerHolder.getSlaveId()));
    double score = score(offerHolder, tasksPerOffer, taskRequestHolder, maybeSlaveUsage, activeTaskIdsForRequest, requestUtilization);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Scored {} | Task {} | Offer - mem {} - cpu {} | Slave {} | maybeSlaveUsage - {}", score, taskRequestHolder.getTaskRequest().getPendingTask().getPendingTaskId().getId(),
          MesosUtils.getMemory(offerHolder.getCurrentResources(), Optional.absent()), MesosUtils.getNumCpus(offerHolder.getCurrentResources(), Optional.absent()), offerHolder.getHostname(), maybeSlaveUsage);
    }
    return score;
  }

  private List<SingularityTaskRequestHolder> getSortedDueTaskRequests() {
    final List<SingularityTaskRequest> taskRequests = taskPrioritizer.getSortedDueTasks(scheduler.getDueTasks());

    taskRequests.forEach((taskRequest) -> LOG.trace("Task {} is due", taskRequest.getPendingTask().getPendingTaskId()));

    taskPrioritizer.removeTasksAffectedByPriorityFreeze(taskRequests);

    return taskRequests.stream()
        .map((taskRequest) -> new SingularityTaskRequestHolder(taskRequest, defaultResources, defaultCustomExecutorResources))
        .collect(Collectors.toList());
  }

  private double getEstimatedCpuUsageForRequest(RequestUtilization requestUtilization) {
    // To account for cpu bursts, tend towards max usage if the app is consistently over-utilizing cpu, tend towards avg if it is over-utilized in short bursts
    return (requestUtilization.getMaxCpuUsed() - requestUtilization.getAvgCpuUsed()) * requestUtilization.getCpuBurstRating() + requestUtilization.getAvgCpuUsed();
  }

  private double score(SingularityOfferHolder offerHolder, Map<String, Integer> tasksPerOffer, SingularityTaskRequestHolder taskRequestHolder,
                       Optional<SingularitySlaveUsageWithCalculatedScores> maybeSlaveUsage, List<SingularityTaskId> activeTaskIdsForRequest,
                       RequestUtilization requestUtilization) {

    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityPendingTaskId pendingTaskId = taskRequest.getPendingTask().getPendingTaskId();

    if (tooManyTasksPerOfferHostForRequest(tasksPerOffer, offerHolder.getHostname(), taskRequestHolder.getTaskRequest())) {
      LOG.debug("Skipping task request for request id {}, too many tasks already scheduled using offer {}", taskRequest.getRequest().getId(), offerHolder.getHostname());
      return 0;
    }

    if (isTooManyInstancesForRequest(taskRequest, activeTaskIdsForRequest)) {
      LOG.debug("Skipping pending task {}, too many instances already running", pendingTaskId);
      return 0;
    }

    double estimatedCpusToAdd = taskRequestHolder.getTotalResources().getCpus();
    if (requestUtilization != null) {
      estimatedCpusToAdd = getEstimatedCpuUsageForRequest(requestUtilization);
    }
    if (mesosConfiguration.isOmitOverloadedHosts() && maybeSlaveUsage.isPresent() && maybeSlaveUsage.get().isCpuOverloaded(estimatedCpusToAdd)) {
      LOG.debug("Slave {} is overloaded (load5 {}/{}, load1 {}/{}, estimated cpus to add: {}), ignoring offer",
          offerHolder.getHostname(),
          maybeSlaveUsage.get().getSlaveUsage().getSystemLoad5Min(), maybeSlaveUsage.get().getSlaveUsage().getSystemCpusTotal(),
          maybeSlaveUsage.get().getSlaveUsage().getSystemLoad1Min(), maybeSlaveUsage.get().getSlaveUsage().getSystemCpusTotal(),
          estimatedCpusToAdd);
      return 0;
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("Attempting to match task {} resources {} with required role '{}' ({} for task + {} for executor) with remaining offer resources {}",
          pendingTaskId, taskRequestHolder.getTotalResources(), taskRequest.getRequest().getRequiredRole().or("*"),
          taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources(), MesosUtils.formatForLogging(offerHolder.getCurrentResources()));
    }

    final boolean matchesResources = MesosUtils.doesOfferMatchResources(taskRequest.getRequest().getRequiredRole(),
        taskRequestHolder.getTotalResources(), offerHolder.getCurrentResources(), taskRequestHolder.getRequestedPorts());
    if (!matchesResources) {
      return 0;
    }
    final SlaveMatchState slaveMatchState = slaveAndRackManager.doesOfferMatch(offerHolder, taskRequest, activeTaskIdsForRequest, isPreemptibleTask(taskRequest));

    if (slaveMatchState.isMatchAllowed()) {
      return score(offerHolder.getHostname(), maybeSlaveUsage);
    } else if (LOG.isTraceEnabled()) {
      LOG.trace("Ignoring offer on host {} with roles {} on {} for task {}; matched resources: {}, slave match state: {}", offerHolder.getHostname(),
          offerHolder.getRoles(), offerHolder.getHostname(), pendingTaskId, matchesResources, slaveMatchState);
    }

    return 0;
  }

  private boolean isPreemptibleTask(SingularityTaskRequest taskRequest) {
    // A long running task can be replaced + killed easily
    if (taskRequest.getRequest().getRequestType().isLongRunning()) {
      return true;
    }

    // A short, non-long-running task
    Optional<SingularityDeployStatistics> deployStatistics = deployManager.getDeployStatistics(taskRequest.getRequest().getId(), taskRequest.getDeploy().getId());
    return deployStatistics.isPresent()
        && deployStatistics.get().getAverageRuntimeMillis().isPresent()
        && deployStatistics.get().getAverageRuntimeMillis().get() < configuration.getPreemptibleTaskMaxExpectedRuntimeMs();
  }

  @VisibleForTesting
  double score(String hostname, Optional<SingularitySlaveUsageWithCalculatedScores> maybeSlaveUsage) {
    if (!maybeSlaveUsage.isPresent() || maybeSlaveUsage.get().isMissingUsageData()) {
      if (mesosConfiguration.isOmitForMissingUsageData()) {
        LOG.info("Skipping slave {} with missing usage data ({})", hostname, maybeSlaveUsage);
        return 0.0;
      } else {
        LOG.info("Slave {} has missing usage data ({}). Will default to {}", hostname, maybeSlaveUsage, 0.5);
        return 0.5;
      }
    }

    SingularitySlaveUsageWithCalculatedScores slaveUsageWithScores = maybeSlaveUsage.get();

    return calculateScore(
        1 - slaveUsageWithScores.getMemAllocatedScore(), slaveUsageWithScores.getMemInUseScore(),
        1 - slaveUsageWithScores.getCpusAllocatedScore(), slaveUsageWithScores.getCpusInUseScore(),
        1 - slaveUsageWithScores.getDiskAllocatedScore(), slaveUsageWithScores.getDiskInUseScore(),
        mesosConfiguration.getInUseResourceWeight(), mesosConfiguration.getAllocatedResourceWeight());
  }

  private double calculateScore(double memAllocatedScore, double memInUseScore, double cpusAllocatedScore, double cpusInUseScore, double diskAllocatedScore, double diskInUseScore, double inUseResourceWeight, double allocatedResourceWeight) {
    double score = 0;

    score += (normalizedCpuWeight * allocatedResourceWeight) * cpusAllocatedScore;
    score += (normalizedMemWeight * allocatedResourceWeight) * memAllocatedScore;
    score += (normalizedDiskWeight * allocatedResourceWeight) * diskAllocatedScore;

    score += (normalizedCpuWeight * inUseResourceWeight) * cpusInUseScore;
    score += (normalizedMemWeight * inUseResourceWeight) * memInUseScore;
    score += (normalizedDiskWeight * inUseResourceWeight) * diskInUseScore;

    return score;
  }

  private SingularityMesosTaskHolder acceptTask(SingularityOfferHolder offerHolder, Map<String, Integer> tasksPerOffer, SingularityTaskRequestHolder taskRequestHolder) {
    final SingularityTaskRequest taskRequest = taskRequestHolder.getTaskRequest();
    final SingularityMesosTaskHolder taskHolder = mesosTaskBuilder.buildTask(offerHolder, offerHolder.getCurrentResources(), taskRequest, taskRequestHolder.getTaskResources(), taskRequestHolder.getExecutorResources());

    final SingularityTask zkTask = taskSizeOptimizer.getSizeOptimizedTask(taskHolder);

    LOG.trace("Accepted and built task {}", zkTask);
    LOG.info("Launching task {} slot on slave {} ({})", taskHolder.getTask().getTaskId(), offerHolder.getSlaveId(), offerHolder.getHostname());

    taskManager.createTaskAndDeletePendingTask(zkTask);

    addRequestToMapByOfferHost(tasksPerOffer, offerHolder.getHostname(), taskRequest.getRequest().getId());

    return taskHolder;
  }

  private void addRequestToMapByOfferHost(Map<String, Integer> tasksPerOffer, String hostname, String requestId) {
    if (tasksPerOffer.containsKey(hostname)) {
      int count = tasksPerOffer.get(hostname);
      tasksPerOffer.put(hostname, count + 1);
    } else {
      tasksPerOffer.put(hostname, 1);
    }
  }

  private boolean tooManyTasksPerOfferHostForRequest(Map<String, Integer> tasksPerOffer, String hostname, SingularityTaskRequest taskRequest) {
    if (!tasksPerOffer.containsKey(hostname)) {
      return false;
    }

    int maxPerOfferPerRequest = taskRequest.getRequest().getMaxTasksPerOffer().or(configuration.getMaxTasksPerOfferPerRequest());
    return maxPerOfferPerRequest > 0 && tasksPerOffer.get(hostname) > maxPerOfferPerRequest;
  }

  private boolean isTooManyInstancesForRequest(SingularityTaskRequest taskRequest, List<SingularityTaskId> activeTaskIdsForRequest) {
    if (taskRequest.getRequest().getRequestType() == RequestType.ON_DEMAND) {
      int maxActiveOnDemandTasks = taskRequest.getRequest().getInstances().or(configuration.getMaxActiveOnDemandTasksPerRequest());
      if (maxActiveOnDemandTasks > 0) {
        int activeTasksForRequest = activeTaskIdsForRequest.size();
        LOG.debug("Running {} instances for request {}. Max is {}", activeTasksForRequest, taskRequest.getRequest().getId(), maxActiveOnDemandTasks);
        if (activeTasksForRequest >= maxActiveOnDemandTasks) {
          return true;
        }
      }
    }

    return false;
  }
}
