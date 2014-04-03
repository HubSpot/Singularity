package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryTranscoder;

public class JDBIHistoryManager implements HistoryManager {

  private final static Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);
  
  private final HistoryJDBI history;
  private final SingularityTaskHistoryTranscoder taskHistoryTranscoder;
  private final ObjectMapper objectMapper;

  // TODO jdbi timeouts? 
  
  @Inject
  public JDBIHistoryManager(HistoryJDBI history, ObjectMapper objectMapper, SingularityTaskHistoryTranscoder taskHistoryTranscoder) {
    this.taskHistoryTranscoder = taskHistoryTranscoder;
    this.history = history;
    this.objectMapper = objectMapper;
  }
  
  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return history.getTaskHistoryForRequest(requestId, limitStart, limitCount);
  }

  @Override
  public void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user) {
    try {
      history.insertRequestHistory(request.getId(), request.getAsBytes(objectMapper), new Date(), state.name(), user.orNull());
    } catch (SingularityJsonException jpe) {
      LOG.warn(String.format("Couldn't insert request history for request %s due to json exception", request), jpe);
    }
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
    
    try {
      String lastTaskStatus = null;
      if (taskIdHistory.getLastTaskState().isPresent()) {
        lastTaskStatus = taskIdHistory.getLastTaskState().get().name();
      }
      history.insertTaskHistory(taskIdHistory.getTaskId().getRequestId(), taskIdHistory.getTaskId().getId(), taskHistoryTranscoder.toBytes(taskHistory), new Date(taskIdHistory.getTaskId().getStartedAt()), new Date(taskIdHistory.getUpdatedAt()), lastTaskStatus);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
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
