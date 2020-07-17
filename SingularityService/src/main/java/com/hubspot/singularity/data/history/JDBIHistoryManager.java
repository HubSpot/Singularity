package com.hubspot.singularity.data.history;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityTranscoderException;
import com.hubspot.singularity.data.transcoders.Transcoder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBIHistoryManager implements HistoryManager {
  private static final Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);

  private final HistoryJDBI history;
  private final boolean fallBackToBytesFields;
  private final SingularityConfiguration configuration;
  private final Transcoder<SingularityTaskHistory> taskHistoryTranscoder;
  private final Transcoder<SingularityDeployHistory> deployHistoryTranscoder;
  private final AtomicBoolean historyBackfillRunning;

  @Inject
  public JDBIHistoryManager(
    HistoryJDBI history,
    SingularityConfiguration configuration,
    Transcoder<SingularityTaskHistory> taskHistoryTranscoder,
    Transcoder<SingularityDeployHistory> deployHistoryTranscoder
  ) {
    this.taskHistoryTranscoder = taskHistoryTranscoder;
    this.deployHistoryTranscoder = deployHistoryTranscoder;
    this.history = history;
    this.historyBackfillRunning = new AtomicBoolean(false);
    this.configuration = configuration;
    this.fallBackToBytesFields = configuration.isSqlFallBackToBytesFields();
  }

  @Override
  @Timed
  public List<SingularityTaskIdHistory> getTaskIdHistory(
    Optional<String> requestId,
    Optional<String> deployId,
    Optional<String> runId,
    Optional<String> host,
    Optional<ExtendedTaskState> lastTaskStatus,
    Optional<Long> startedBefore,
    Optional<Long> startedAfter,
    Optional<Long> updatedBefore,
    Optional<Long> updatedAfter,
    Optional<OrderDirection> orderDirection,
    Optional<Integer> limitStart,
    Integer limitCount
  ) {
    List<SingularityTaskIdHistory> taskIdHistoryList = history.getTaskIdHistory(
      requestId,
      deployId,
      runId,
      host,
      lastTaskStatus,
      startedBefore,
      startedAfter,
      updatedBefore,
      updatedAfter,
      orderDirection,
      limitStart,
      limitCount
    );
    if (LOG.isTraceEnabled()) {
      LOG.trace("getTaskIdHistory taskIdHistory {}", taskIdHistoryList);
    }

    return taskIdHistoryList;
  }

  @Override
  @Timed
  public int getTaskIdHistoryCount(
    Optional<String> requestId,
    Optional<String> deployId,
    Optional<String> runId,
    Optional<String> host,
    Optional<ExtendedTaskState> lastTaskStatus,
    Optional<Long> startedBefore,
    Optional<Long> startedAfter,
    Optional<Long> updatedBefore,
    Optional<Long> updatedAfter
  ) {
    int count = history.getTaskIdHistoryCount(
      requestId,
      deployId,
      runId,
      host,
      lastTaskStatus,
      startedBefore,
      startedAfter,
      updatedBefore,
      updatedAfter
    );
    if (LOG.isTraceEnabled()) {
      LOG.trace("getTaskIdHistoryCount {}", count);
    }

    return count;
  }

  private String getVarcharField(Optional<String> field, int maxLength) {
    if (!field.isPresent()) {
      return null;
    }

    if (field.get().length() > maxLength) {
      return field.get().substring(0, maxLength);
    }

    return field.get();
  }

  private String getMessageField(Optional<String> message) {
    return getVarcharField(message, 280);
  }

  private String getUserField(Optional<String> user) {
    return getVarcharField(user, 100);
  }

  @Override
  public void saveRequestHistoryUpdate(SingularityRequestHistory requestHistory) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("saveRequestHistoryUpdate requestHistory {}", requestHistory);
    }

    try {
      history.insertRequestHistory(
        requestHistory.getRequest().getId(),
        requestHistory.getRequest(),
        new Date(requestHistory.getCreatedAt()),
        requestHistory.getEventType().name(),
        getUserField(requestHistory.getUser()),
        getMessageField(requestHistory.getMessage())
      );
    } catch (Throwable t) {
      if (
        Throwables
          .getCausalChain(t)
          .stream()
          .anyMatch(exn -> exn instanceof SQLIntegrityConstraintViolationException)
      ) {
        LOG.info(
          "Entry for {} ({}) already existed, skipping save",
          requestHistory.getRequest(),
          requestHistory.getCreatedAt()
        );
      } else {
        throw t;
      }
    }
  }

  @Override
  public void saveDeployHistory(SingularityDeployHistory deployHistory) {
    if (LOG.isTraceEnabled()) {
      LOG.trace("saveDeployHistory {}", deployHistory);
    }

    try {
      history.insertDeployHistory(
        deployHistory.getDeployMarker().getRequestId(),
        deployHistory.getDeployMarker().getDeployId(),
        new Date(deployHistory.getDeployMarker().getTimestamp()),
        getUserField(deployHistory.getDeployMarker().getUser()),
        getMessageField(deployHistory.getDeployMarker().getMessage()),
        deployHistory.getDeployResult().isPresent()
          ? new Date(deployHistory.getDeployResult().get().getTimestamp())
          : new Date(deployHistory.getDeployMarker().getTimestamp()),
        deployHistory.getDeployResult().isPresent()
          ? deployHistory.getDeployResult().get().getDeployState().name()
          : DeployState.CANCELED.name(),
        deployHistory
      );
    } catch (Throwable t) {
      if (
        Throwables
          .getCausalChain(t)
          .stream()
          .anyMatch(exn -> exn instanceof SQLIntegrityConstraintViolationException)
      ) {
        LOG.info(
          "Entry for {} - {} already existed, skipping save",
          deployHistory.getDeployMarker().getRequestId(),
          deployHistory.getDeployMarker().getDeployId()
        );
      } else {
        throw t;
      }
    }
  }

  @Override
  public Optional<SingularityDeployHistory> getDeployHistory(
    String requestId,
    String deployId
  ) {
    Optional<SingularityDeployHistory> maybeHistory = Optional.ofNullable(
      history.getDeployHistoryForDeploy(requestId, deployId)
    );
    if (!maybeHistory.isPresent() && fallBackToBytesFields) {
      byte[] historyBytes = history.getDeployHistoryBytesForDeploy(requestId, deployId);
      Optional<SingularityDeployHistory> historyOptional = Optional.empty();
      if (historyBytes != null) {
        historyOptional = Optional.of(deployHistoryTranscoder.fromBytes(historyBytes));
      }
      return historyOptional;
    }
    return maybeHistory;
  }

  @Override
  public List<SingularityDeployHistory> getDeployHistoryForRequest(
    String requestId,
    Integer limitStart,
    Integer limitCount
  ) {
    List<SingularityDeployHistory> deployHistoryList = history.getDeployHistoryForRequest(
      requestId,
      limitStart,
      limitCount
    );
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "getDeployHistory requestId {}, limitStart {}, limitCount {} deployHistory {}",
        requestId,
        limitStart,
        limitCount,
        deployHistoryList
      );
    }
    return deployHistoryList;
  }

  @Override
  public int getDeployHistoryForRequestCount(String requestId) {
    int count = history.getDeployHistoryForRequestCount(requestId);
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "getDeployHistoryForRequestCount requestId {}, count {}",
        requestId,
        count
      );
    }
    return count;
  }

  private String getOrderDirection(Optional<OrderDirection> orderDirection) {
    return orderDirection.orElse(OrderDirection.DESC).name();
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistory(
    String requestId,
    Optional<Long> createdBefore,
    Optional<Long> createdAfter,
    Optional<OrderDirection> orderDirection,
    Integer limitStart,
    Integer limitCount
  ) {
    List<SingularityRequestHistory> singularityRequestHistoryList = history.getRequestHistory(
      requestId,
      createdBefore,
      createdAfter,
      getOrderDirection(orderDirection),
      limitStart,
      limitCount
    );
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "getRequestHistory requestId {}, createdBefore {}, createdAfter {}, orderDirection {}, limitStart {} , limitCount {}, requestHistory{}",
        requestId,
        createdBefore,
        createdAfter,
        orderDirection,
        limitStart,
        limitCount,
        singularityRequestHistoryList
      );
    }
    return singularityRequestHistoryList;
  }

  @Override
  public int getRequestHistoryCount(String requestId) {
    int count = history.getRequestHistoryCount(requestId);
    if (LOG.isTraceEnabled()) {
      LOG.trace("getRequestHistoryCount requestId {}, count {}", requestId, count);
    }
    return history.getRequestHistoryCount(requestId);
  }

  @Override
  public List<String> getRequestHistoryLike(
    String requestIdLike,
    Integer limitStart,
    Integer limitCount
  ) {
    List<String> list = history.getRequestHistoryLike(
      requestIdLike,
      limitStart,
      limitCount
    );
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "getRequestHistoryCountLike requestIdLike {}, limitStart {}, limitCount {}, requestIds {}",
        requestIdLike,
        limitStart,
        limitCount,
        list
      );
    }
    return list;
  }

  @Override
  public void saveTaskHistory(SingularityTaskHistory taskHistory) {
    if (
      history.getTaskHistoryForTask(taskHistory.getTask().getTaskId().getId()) != null
    ) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("saveTaskHistory -- existing taskHistory {}", taskHistory);
      }
      return;
    }

    SingularityTaskIdHistory taskIdHistory = SingularityTaskIdHistory.fromTaskIdAndTaskAndUpdates(
      taskHistory.getTask().getTaskId(),
      taskHistory.getTask(),
      taskHistory.getTaskUpdates()
    );

    String lastTaskStatus = null;
    if (taskIdHistory.getLastTaskState().isPresent()) {
      lastTaskStatus = taskIdHistory.getLastTaskState().get().name();
    }

    if (LOG.isTraceEnabled()) {
      LOG.trace("saveTaskHistory -- will insert taskHistory {}", taskHistory);
    }

    history.insertTaskHistory(
      taskIdHistory.getTaskId().getRequestId(),
      taskIdHistory.getTaskId().getId(),
      taskHistory,
      new Date(taskIdHistory.getUpdatedAt()),
      lastTaskStatus,
      taskHistory.getTask().getTaskRequest().getPendingTask().getRunId().orElse(null),
      taskIdHistory.getTaskId().getDeployId(),
      taskIdHistory.getTaskId().getHost(),
      new Date(taskIdHistory.getTaskId().getStartedAt())
    );
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistory(String taskId) {
    Optional<SingularityTaskHistory> maybeTaskHistory = Optional.ofNullable(
      history.getTaskHistoryForTask(taskId)
    );
    if (!maybeTaskHistory.isPresent() && fallBackToBytesFields) {
      return fromBytes(history.getTaskHistoryBytesForTask(taskId));
    }
    return maybeTaskHistory;
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistoryByRunId(
    String requestId,
    String runId
  ) {
    Optional<SingularityTaskHistory> maybeTaskHistory = Optional.ofNullable(
      history.getTaskHistoryForTaskByRunId(requestId, runId)
    );
    if (!maybeTaskHistory.isPresent() && fallBackToBytesFields) {
      return fromBytes(history.getTaskHistoryBytesForTaskByRunId(requestId, runId));
    }
    return maybeTaskHistory;
  }

  private Optional<SingularityTaskHistory> fromBytes(byte[] historyBytes) {
    Optional<SingularityTaskHistory> taskHistoryOptional = Optional.empty();
    if (historyBytes != null && historyBytes.length > 0) {
      taskHistoryOptional = Optional.of(taskHistoryTranscoder.fromBytes(historyBytes));
    }
    return taskHistoryOptional;
  }

  @Override
  public List<String> getRequestIdsInTaskHistory() {
    List<String> list = history.getRequestIdsInTaskHistory();
    if (LOG.isTraceEnabled()) {
      LOG.trace("getRequestIdsInTaskHistory requestIds {}", list);
    }
    return list;
  }

  @Override
  public int getUnpurgedTaskHistoryCountByRequestBefore(String requestId, Date before) {
    int count = history.getUnpurgedTaskHistoryCountByRequestBefore(requestId, before);
    if (LOG.isTraceEnabled()) {
      LOG.trace(
        "getUnpurgedTaskHistoryByRequestBeforeCount requestId {}, before {}, count {}",
        requestId,
        before,
        count
      );
    }

    return count;
  }

  @Override
  public void purgeTaskHistory(
    String requestId,
    int count,
    Optional<Integer> limit,
    Optional<Date> purgeBefore,
    boolean deleteRowInsteadOfUpdate,
    Integer maxPurgeCount
  ) {
    if (limit.isPresent() && count > limit.get()) {
      Date beforeBasedOnLimit = history.getMinUpdatedAtWithLimitForRequest(
        requestId,
        limit.get()
      );

      if (deleteRowInsteadOfUpdate) {
        LOG.debug(
          "Deleting task history for {} above {} items (before {})",
          requestId,
          limit.get(),
          beforeBasedOnLimit
        );

        history.deleteTaskHistoryForRequestBefore(
          requestId,
          beforeBasedOnLimit,
          maxPurgeCount
        );
      } else {
        LOG.debug(
          "Purging task history bytes for {} above {} items (before {})",
          requestId,
          limit.get(),
          beforeBasedOnLimit
        );

        history.updateTaskHistoryNullBytesForRequestBefore(
          requestId,
          beforeBasedOnLimit,
          maxPurgeCount
        );
      }
    }

    if (purgeBefore.isPresent()) {
      if (deleteRowInsteadOfUpdate) {
        LOG.debug("Deleting task history for {} before {}", requestId, purgeBefore.get());

        history.deleteTaskHistoryForRequestBefore(
          requestId,
          purgeBefore.get(),
          maxPurgeCount
        );
      } else {
        LOG.debug(
          "Purging task history bytes for {} before {}",
          requestId,
          purgeBefore.get()
        );

        history.updateTaskHistoryNullBytesForRequestBefore(
          requestId,
          purgeBefore.get(),
          maxPurgeCount
        );
      }
    }
  }

  @Override
  @SuppressFBWarnings("NP_NONNULL_PARAM_VIOLATION") // https://github.com/findbugsproject/findbugs/issues/79
  public CompletableFuture<Void> startHistoryBackfill(int batchSize) {
    if (!historyBackfillRunning.compareAndSet(false, true)) {
      LOG.warn("History backfill already running, will not restart");
      return CompletableFuture.completedFuture(null);
    }
    return CompletableFuture.runAsync(
      () -> {
        try {
          backfillTaskJson(batchSize);
          backfillRequestJson(batchSize);
          backfillDeployJson(batchSize);
        } catch (Throwable t) {
          LOG.error("While running history backfill", t);
        } finally {
          historyBackfillRunning.set(false);
        }
      }
    );
  }

  private void backfillTaskJson(int batchSize) {
    for (String requestId : history.getRequestIdsInTaskHistory()) {
      List<byte[]> taskHistories = history.getTasksWithBytes(requestId, batchSize);
      while (!taskHistories.isEmpty()) {
        taskHistories
          .stream()
          .map(
            bytes -> {
              try {
                return Optional.of(taskHistoryTranscoder.fromBytes(bytes));
              } catch (SingularityTranscoderException ste) {
                LOG.warn("Could not deserialize task", ste);
                return Optional.<SingularityTaskHistory>empty();
              }
            }
          )
          .filter(Optional::isPresent)
          .map(Optional::get)
          .forEach(t -> history.setTaskJson(t.getTask().getTaskId().getId(), t));
        LOG.debug("Converted {} task histories to json format", taskHistories.size());
        taskHistories = history.getTasksWithBytes(requestId, batchSize);
      }
    }
  }

  private void backfillRequestJson(int batchSize) {
    List<SingularityRequestAndTime> requests = history.getRequestsWithBytes(batchSize);
    while (!requests.isEmpty()) {
      requests.forEach(
        r ->
          history.setRequestJson(
            r.getRequest().getId(),
            new Date(r.getCreatedAt()),
            r.getRequest()
          )
      );
      LOG.debug("Converted {} request histories to json format", requests.size());
      requests = history.getRequestsWithBytes(batchSize);
    }
  }

  private void backfillDeployJson(int batchSize) {
    for (String requestId : history.getRequestIdsWithDeploys()) {
      List<byte[]> deployHistories = history.getDeploysWithBytes(requestId, batchSize);
      while (!deployHistories.isEmpty()) {
        deployHistories
          .stream()
          .map(
            bytes -> {
              try {
                return Optional.of(deployHistoryTranscoder.fromBytes(bytes));
              } catch (SingularityTranscoderException ste) {
                LOG.warn("Could not deserialize deploy", ste);
                return Optional.<SingularityDeployHistory>empty();
              }
            }
          )
          .filter(Optional::isPresent)
          .map(Optional::get)
          .forEach(
            d ->
              history.setDeployJson(
                d.getDeployMarker().getRequestId(),
                d.getDeployMarker().getDeployId(),
                d
              )
          );
        LOG.debug("Converted {} deploy histories to json format", deployHistories.size());
        deployHistories = history.getDeploysWithBytes(requestId, batchSize);
      }
    }
  }

  @Override
  public void purgeRequestHistory() {
    long threshold =
      System.currentTimeMillis() -
      TimeUnit.DAYS.toMillis(
        configuration.getHistoryPurgingConfiguration().getPurgeRequestHistoryAfterDays()
      );
    for (String requestId : history.getRequestIdsWithHistory()) {
      LOG.debug("Purging old request history for {}", requestId);
      int purged;
      do {
        purged =
          history.purgeRequestHistory(
            requestId,
            new Date(threshold),
            configuration.getHistoryPurgingConfiguration().getPurgeLimitPerQuery()
          );
      } while (purged > 0);
    }
  }

  @Override
  public void purgeDeployHistory() {
    long threshold =
      System.currentTimeMillis() -
      TimeUnit.DAYS.toMillis(
        configuration.getHistoryPurgingConfiguration().getPurgeDeployHistoryAfterDays()
      );
    for (String requestId : history.getRequestIdsWithDeploys()) {
      LOG.debug("Purging old deploy history for {}", requestId);
      int purged;
      do {
        purged =
          history.purgeDeployHistory(
            requestId,
            new Date(threshold),
            configuration.getHistoryPurgingConfiguration().getPurgeLimitPerQuery()
          );
      } while (purged > 0);
    }
  }
}
