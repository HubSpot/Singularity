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
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.history.SingularityRequestHistory.RequestState;

public class JDBIHistoryManager implements HistoryManager {

  private final static Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);
  
  private final HistoryJDBI history;
  private final ObjectMapper objectMapper;

  // TODO jdbi timeouts? should this be synchronous?
  
  @Inject
  public JDBIHistoryManager(HistoryJDBI history, ObjectMapper objectMapper) {
    this.history = history;
    this.objectMapper = objectMapper;
  }
  
  @Override
  public void saveTaskHistory(SingularityTask task, String driverStatus) {
    try {
      history.insertTaskHistory(task.getTaskRequest().getRequest().getName(),
          task.getTaskId().toString(),
          task.getTaskData(objectMapper),
          driverStatus,
          new Date());
    } catch (JsonProcessingException jpe) {
      LOG.warn(String.format("Couldn't insert task history for task %s due to json exception", task), jpe);
    }
  }
  

  @Override
  public void saveRequestHistoryUpdate(SingularityRequest request, RequestState state, Optional<String> user) {
    try {
      history.insertRequestHistory(request.getName(), request.getRequestData(objectMapper), new Date(), state.name(), user.orNull());
    } catch (JsonProcessingException jpe) {
      LOG.warn(String.format("Couldn't insert request history for request %s due to json exception", request), jpe);
    }
  }
  
  @Override
  public List<SingularityTaskId> getTaskHistoryForRequestLike(String requestNameLike) {
    return history.getTaskHistoryForRequestLike(requestNameLike);
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistory(String requestName) {
    return history.getRequestHistory(requestName);
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistoryLike(String requestNameLike) {
    return history.getRequestHistoryLike(requestNameLike);
  }

  @Override
  public void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message) {
    try {
      history.insertTaskUpdate(taskId, statusUpdate, message.orNull(), new Date());
    } catch (Throwable t) {
      LOG.warn(String.format("Error while inserting update to history for %s - %s", taskId, statusUpdate), t);
    }
  }

  @Override
  public List<SingularityTaskId> getTaskHistoryForRequest(String requestName) {
    return history.getTaskHistoryForRequest(requestName);
  }

  @Override
  public SingularityTaskHistory getTaskHistory(String taskId) {
    SingularityTaskHistoryHelper helper = history.getTaskHistoryForTask(taskId);
    
    List<SingularityTaskHistoryUpdate> updates = history.getTaskUpdates(taskId);
    
    try {
      return new SingularityTaskHistory(updates, helper.getTimestamp(), SingularityTask.getTaskFromData(helper.getTaskData(), objectMapper));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
}
