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
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class JDBIHistoryManager implements HistoryManager {

  private final static Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);
  
  private final HistoryJDBI history;
  private final ObjectMapper objectMapper;

  @Inject
  public JDBIHistoryManager(HistoryJDBI history, ObjectMapper objectMapper) {
    this.history = history;
    this.objectMapper = objectMapper;
  }
  
  // TODO notify on persister failures?
  // TODO handle DB failures
  
  @Override
  public void saveTaskHistory(SingularityTask task, String driverStatus) {
    try {
      history.insertTaskHistory(task.getTaskRequest().getRequest().getName(),
          task.getTaskRequest().getTaskId().toString(),
          task.getTaskData(objectMapper),
          driverStatus,
          new Date());
    } catch (JsonProcessingException jpe) {
      LOG.warn(String.format("Couldn't insert task history for task %s due to json exception", task), jpe);
    }
  }

  @Override
  public void saveTaskUpdate(String taskId, String statusUpdate, Optional<String> message) {
    history.insertTaskUpdate(taskId, statusUpdate, message.orNull(), new Date());
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
      // TODO handle this
      throw Throwables.propagate(e);
    }
  }
  
}
