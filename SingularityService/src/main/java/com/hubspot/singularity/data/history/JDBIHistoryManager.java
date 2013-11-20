package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityJsonObject.SingularityJsonException;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;

public class JDBIHistoryManager implements HistoryManager {

  private final static Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);
  
  private final HistoryJDBI history;
  private final ObjectMapper objectMapper;

  // TODO jdbi timeouts? should this be synchronous?
  // TODO review exception handling
  
  @Inject
  public JDBIHistoryManager(HistoryJDBI history, ObjectMapper objectMapper) {
    this.history = history;
    this.objectMapper = objectMapper;
  }
  
  @Override
  public void saveTaskHistory(SingularityTask task, String driverStatus) {
    try {
      history.insertTaskHistory(task.getTaskRequest().getRequest().getId(),
          task.getTaskId().toString(),
          task.getAsBytes(objectMapper),
          driverStatus,
          new Date());
    } catch (SingularityJsonException jpe) {
      LOG.warn(String.format("Couldn't insert task history for task %s due to json exception", task), jpe);
    }
  }
  
  @Override
  public void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user) {
    try {
      history.insertRequestHistory(request.getId(), request.getAsBytes(objectMapper), new Date(), state.name(), user.orNull());
    } catch (SingularityJsonException jpe) {
      LOG.warn(String.format("Couldn't insert request history for request %s due to json exception", request), jpe);
    }
  }
  
  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequestLike(String requestIdLike) {
    return history.getTaskHistoryForRequestLike(requestIdLike);
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistory(String requestId) {
    return history.getRequestHistory(requestId);
  }

  @Override
  public void updateTaskHistory(String taskId, String statusUpdate, Date timestamp) {
    try {
      history.updateTaskStatus(taskId, statusUpdate,timestamp);
    } catch (Throwable t) {
      LOG.warn(String.format("Error while updating task status %s for %s", statusUpdate, taskId), t);
    }
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistoryLike(String requestIdLike) {
    return history.getRequestHistoryLike(requestIdLike);
  }

  @Override
  public void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message, Date timestamp) {
    try {
      history.insertTaskUpdate(taskId, statusUpdate, message.orNull(), timestamp);
    } catch (Throwable t) {
      LOG.warn(String.format("Error while inserting update to history for %s - %s", taskId, statusUpdate), t);
    }
  }

  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId) {
    return history.getTaskHistoryForRequest(requestId);
  }

  @Override
  public SingularityTaskHistory getTaskHistory(String taskId) {
    SingularityTaskHistoryHelper helper = history.getTaskHistoryForTask(taskId);
    
    List<SingularityTaskHistoryUpdate> updates = history.getTaskUpdates(taskId);
    
    try {
      return new SingularityTaskHistory(updates, helper.getTimestamp(), SingularityTask.fromBytes(helper.getTaskData(), objectMapper));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
}
