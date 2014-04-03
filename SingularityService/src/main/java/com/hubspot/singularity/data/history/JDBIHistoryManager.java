package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployHistoryLite;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.transcoders.SingularityDeployHistoryTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryTranscoder;

public class JDBIHistoryManager implements HistoryManager {

  private final HistoryJDBI history;
  private final SingularityTaskHistoryTranscoder taskHistoryTranscoder;
  private final SingularityDeployHistoryTranscoder deployHistoryTranscoder;
  private final ObjectMapper objectMapper;

  // TODO jdbi timeouts / exceptions 
  
  @Inject
  public JDBIHistoryManager(HistoryJDBI history, ObjectMapper objectMapper, SingularityTaskHistoryTranscoder taskHistoryTranscoder, SingularityDeployHistoryTranscoder deployHistoryTranscoder) {
    this.taskHistoryTranscoder = taskHistoryTranscoder;
    this.deployHistoryTranscoder = deployHistoryTranscoder;
    this.history = history;
    this.objectMapper = objectMapper;
  }
  
  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return history.getTaskHistoryForRequest(requestId, limitStart, limitCount);
  }

  @Override
  public void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user) {
    history.insertRequestHistory(request.getId(), request.getAsBytes(objectMapper), new Date(), state.name(), user.orNull());
  }
  
  @Override
  public void saveDeployHistory(SingularityDeployHistory deployHistory) {
    history.insertDeployHistory(deployHistory.getDeployMarker().getRequestId(), deployHistory.getDeployMarker().getDeployId(), new Date(deployHistory.getDeployMarker().getTimestamp()), deployHistory.getDeployMarker().getUser().orNull(), 
        deployHistory.getDeployState().get().name(), deployHistoryTranscoder.toBytes(deployHistory));
  }

  @Override
  public Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId) {
    byte[] historyBytes = history.getDeployHistoryForDeploy(requestId, deployId);
    
    if (historyBytes == null) {
      return Optional.absent();
    }
    
    return Optional.of(deployHistoryTranscoder.transcode(historyBytes));
  }

  @Override
  public List<SingularityDeployHistoryLite> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return history.getDeployHistoryForRequest(requestId, limitStart, limitCount);
  }

  private String getOrderDirection(Optional<OrderDirection> orderDirection) {
    return orderDirection.or(OrderDirection.ASC).name();
  }
  
  @Override
  public List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<RequestHistoryOrderBy> orderBy, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount) {
    return history.getRequestHistory(requestId, orderBy.or(RequestHistoryOrderBy.createdAt).name(), getOrderDirection(orderDirection), limitStart, limitCount);
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistoryLike(String requestIdLike, Optional<RequestHistoryOrderBy> orderBy, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount) {
    return history.getRequestHistoryLike(requestIdLike, orderBy.or(RequestHistoryOrderBy.requestId).name(), getOrderDirection(orderDirection), limitStart, limitCount);
  }
  
  @Override
  public void saveTaskHistory(SingularityTaskHistory taskHistory) {
    if (history.getTaskHistoryForTask(taskHistory.getTask().getTaskId().getId()) != null) {
      return;
    }
    
    SingularityTaskIdHistory taskIdHistory = SingularityTaskIdHistory.fromTaskIdAndUpdates(taskHistory.getTask().getTaskId(), taskHistory.getTaskUpdates());
    
    String lastTaskStatus = null;
    if (taskIdHistory.getLastTaskState().isPresent()) {
      lastTaskStatus = taskIdHistory.getLastTaskState().get().name();
    }
    
    history.insertTaskHistory(taskIdHistory.getTaskId().getRequestId(), taskIdHistory.getTaskId().getId(), taskHistoryTranscoder.toBytes(taskHistory), new Date(taskIdHistory.getUpdatedAt()), lastTaskStatus);
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistory(String taskId) {
    byte[] historyBytes = history.getTaskHistoryForTask(taskId);
    
    if (historyBytes == null) {
      return Optional.absent();
    }
    
    return Optional.of(taskHistoryTranscoder.transcode(historyBytes));
  }
  
}
