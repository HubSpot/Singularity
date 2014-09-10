package com.hubspot.singularity.data.history;

import static com.google.common.base.Preconditions.checkState;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.transcoders.SingularityDeployHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryTranscoder;

public class JDBIHistoryManager implements HistoryManager {

  private final Optional<HistoryJDBI> history;
  private final SingularityTaskHistoryTranscoder taskHistoryTranscoder;
  private final SingularityDeployHistoryTranscoder deployHistoryTranscoder;
  private final ObjectMapper objectMapper;

  // TODO jdbi timeouts / exceptions

  @Inject
  public JDBIHistoryManager(Optional<HistoryJDBI> history, ObjectMapper objectMapper, SingularityTaskHistoryTranscoder taskHistoryTranscoder, SingularityDeployHistoryTranscoder deployHistoryTranscoder) {
    this.taskHistoryTranscoder = taskHistoryTranscoder;
    this.deployHistoryTranscoder = deployHistoryTranscoder;
    this.history = history;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    checkState(history.isPresent(), "no DBI connection available!");
    return history.get().getTaskHistoryForRequest(requestId, limitStart, limitCount);
  }

  @Override
  public void saveRequestHistoryUpdate(SingularityRequestHistory requestHistory) {
    checkState(history.isPresent(), "no DBI connection available!");
    history.get().insertRequestHistory(requestHistory.getRequest().getId(), requestHistory.getRequest().getAsBytes(objectMapper), new Date(requestHistory.getCreatedAt()), requestHistory.getEventType().name(), requestHistory.getUser().orNull());
  }

  @Override
  public void saveDeployHistory(SingularityDeployHistory deployHistory) {
    checkState(history.isPresent(), "no DBI connection available!");
    history.get().insertDeployHistory(deployHistory.getDeployMarker().getRequestId(),
        deployHistory.getDeployMarker().getDeployId(),
        new Date(deployHistory.getDeployMarker().getTimestamp()),
        deployHistory.getDeployMarker().getUser().orNull(),
        deployHistory.getDeployResult().isPresent() ? new Date(deployHistory.getDeployResult().get().getTimestamp()) : new Date(deployHistory.getDeployMarker().getTimestamp()),
        deployHistory.getDeployResult().isPresent() ? deployHistory.getDeployResult().get().getDeployState().name() : DeployState.CANCELED.name(),
        deployHistoryTranscoder.toBytes(deployHistory));
  }

  @Override
  public Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId) {
    checkState(history.isPresent(), "no DBI connection available!");
    byte[] historyBytes = history.get().getDeployHistoryForDeploy(requestId, deployId);

    if (historyBytes == null) {
      return Optional.absent();
    }

    return Optional.of(deployHistoryTranscoder.transcode(historyBytes));
  }

  @Override
  public List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    checkState(history.isPresent(), "no DBI connection available!");
    return history.get().getDeployHistoryForRequest(requestId, limitStart, limitCount);
  }

  private String getOrderDirection(Optional<OrderDirection> orderDirection) {
    return orderDirection.or(OrderDirection.DESC).name();
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount) {
    return history.get().getRequestHistory(requestId, getOrderDirection(orderDirection), limitStart, limitCount);
  }

  @Override
  public List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount) {
    return history.get().getRequestHistoryLike(requestIdLike, limitStart, limitCount);
  }

  @Override
  public void saveTaskHistory(SingularityTaskHistory taskHistory) {
    checkState(history.isPresent(), "no DBI connection available!");
    if (history.get().getTaskHistoryForTask(taskHistory.getTask().getTaskId().getId()) != null) {
      return;
    }

    SingularityTaskIdHistory taskIdHistory = SingularityTaskIdHistory.fromTaskIdAndUpdates(taskHistory.getTask().getTaskId(), taskHistory.getTaskUpdates());

    String lastTaskStatus = null;
    if (taskIdHistory.getLastTaskState().isPresent()) {
      lastTaskStatus = taskIdHistory.getLastTaskState().get().name();
    }

    history.get().insertTaskHistory(taskIdHistory.getTaskId().getRequestId(), taskIdHistory.getTaskId().getId(), taskHistoryTranscoder.toBytes(taskHistory), new Date(taskIdHistory.getUpdatedAt()), lastTaskStatus);
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistory(String taskId) {
    checkState(history.isPresent(), "no DBI connection available!");
    byte[] historyBytes = history.get().getTaskHistoryForTask(taskId);

    if (historyBytes == null) {
      return Optional.absent();
    }

    return Optional.of(taskHistoryTranscoder.transcode(historyBytes));
  }

}
