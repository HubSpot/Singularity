package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class JDBIHistoryManager implements HistoryManager {

  private static final Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);

  private final HistoryJDBI history;
  private final Transcoder<SingularityTaskHistory> taskHistoryTranscoder;
  private final Transcoder<SingularityDeployHistory> deployHistoryTranscoder;
  private final Transcoder<SingularityRequest> singularityRequestTranscoder;

  @Inject
  public JDBIHistoryManager(HistoryJDBI history, Transcoder<SingularityTaskHistory> taskHistoryTranscoder, Transcoder<SingularityDeployHistory> deployHistoryTranscoder,
      Transcoder<SingularityRequest> singularityRequestTranscoder) {
    this.taskHistoryTranscoder = taskHistoryTranscoder;
    this.deployHistoryTranscoder = deployHistoryTranscoder;
    this.singularityRequestTranscoder = singularityRequestTranscoder;
    this.history = history;
  }

  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return history.getTaskHistoryForRequest(requestId, limitStart, limitCount);
  }

  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForDeploy(String requestId, String deployId, Integer limitStart, Integer limitCount) {
    return history.getTaskHistoryForDeploy(requestId, deployId, limitStart, limitCount);
  }

  @Override
  public void saveRequestHistoryUpdate(SingularityRequestHistory requestHistory) {
    history.insertRequestHistory(requestHistory.getRequest().getId(), singularityRequestTranscoder.toBytes(requestHistory.getRequest()), new Date(requestHistory.getCreatedAt()),
        requestHistory.getEventType().name(), requestHistory.getUser().orNull());
  }

  @Override
  public void saveDeployHistory(SingularityDeployHistory deployHistory) {
    history.insertDeployHistory(deployHistory.getDeployMarker().getRequestId(),
        deployHistory.getDeployMarker().getDeployId(),
        new Date(deployHistory.getDeployMarker().getTimestamp()),
        deployHistory.getDeployMarker().getUser().orNull(),
        deployHistory.getDeployResult().isPresent() ? new Date(deployHistory.getDeployResult().get().getTimestamp()) : new Date(deployHistory.getDeployMarker().getTimestamp()),
            deployHistory.getDeployResult().isPresent() ? deployHistory.getDeployResult().get().getDeployState().name() : DeployState.CANCELED.name(),
                deployHistoryTranscoder.toBytes(deployHistory));
  }

  @Override
  public Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId) {
    byte[] historyBytes = history.getDeployHistoryForDeploy(requestId, deployId);

    if (historyBytes == null) {
      return Optional.absent();
    }

    return Optional.of(deployHistoryTranscoder.fromBytes(historyBytes));
  }

  @Override
  public List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return history.getDeployHistoryForRequest(requestId, limitStart, limitCount);
  }

  private String getOrderDirection(Optional<OrderDirection> orderDirection) {
    return orderDirection.or(OrderDirection.DESC).name();
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount) {
    return history.getRequestHistory(requestId, getOrderDirection(orderDirection), limitStart, limitCount);
  }

  @Override
  public List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount) {
    return history.getRequestHistoryLike(requestIdLike, limitStart, limitCount);
  }

  @Override
  public void saveTaskHistory(SingularityTaskHistory taskHistory) {
    if (history.getTaskHistoryForTask(taskHistory.getTask().getTaskId().getId()) != null) {
      return;
    }

    SingularityTaskIdHistory taskIdHistory = SingularityTaskIdHistory.fromTaskIdAndTaskAndUpdates(taskHistory.getTask().getTaskId(), taskHistory.getTask(), taskHistory.getTaskUpdates());

    String lastTaskStatus = null;
    if (taskIdHistory.getLastTaskState().isPresent()) {
      lastTaskStatus = taskIdHistory.getLastTaskState().get().name();
    }

    history.insertTaskHistory(taskIdHistory.getTaskId().getRequestId(), taskIdHistory.getTaskId().getId(), taskHistoryTranscoder.toBytes(taskHistory), new Date(taskIdHistory.getUpdatedAt()),
        lastTaskStatus, taskHistory.getTask().getTaskRequest().getPendingTask().getRunId().orNull(), taskIdHistory.getTaskId().getDeployId());
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistory(String taskId) {
    byte[] historyBytes = history.getTaskHistoryForTask(taskId);

    if (historyBytes == null || historyBytes.length == 0) {
      return Optional.absent();
    }

    return Optional.of(taskHistoryTranscoder.fromBytes(historyBytes));
  }

  @Override
  public List<SingularityRequestIdCount> getRequestIdCounts(Date before) {
    return history.getRequestIdCounts(before);
  }

  @Override
  public void purgeTaskHistory(String requestId, int count, Optional<Integer> limit, Optional<Date> purgeBefore, boolean deleteRowInsteadOfUpdate) {
    if (limit.isPresent() && count > limit.get()) {
      Date beforeBasedOnLimit = history.getMinUpdatedAtWithLimitForRequest(requestId, limit.get());

      if (deleteRowInsteadOfUpdate) {
        LOG.debug("Deleting task history for {} above {} items (before {})", requestId, limit.get(), beforeBasedOnLimit);

        history.deleteTaskHistoryForRequestBefore(requestId, beforeBasedOnLimit);
      } else {
        LOG.debug("Purging task history bytes for {} above {} items (before {})", requestId, limit.get(), beforeBasedOnLimit);

        history.updateTaskHistoryNullBytesForRequestBefore(requestId, beforeBasedOnLimit);
      }
    }

    if (purgeBefore.isPresent()) {
      if (deleteRowInsteadOfUpdate) {
        LOG.debug("Deleting task history for {} before {}", requestId, purgeBefore.get());

        history.deleteTaskHistoryForRequestBefore(requestId, purgeBefore.get());
      } else {
        LOG.debug("Purging task history bytes for {} before {}", requestId, purgeBefore.get());

        history.updateTaskHistoryNullBytesForRequestBefore(requestId, purgeBefore.get());
      }
    }
  }

}
