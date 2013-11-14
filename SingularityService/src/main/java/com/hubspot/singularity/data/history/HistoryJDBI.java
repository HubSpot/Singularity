package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;

import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.history.SingularityRequestHistory.SingularityRequestHistoryMapper;
import com.hubspot.singularity.data.history.SingularityTaskHistory.SingularityTaskIdMapper;
import com.hubspot.singularity.data.history.SingularityTaskHistoryHelper.SingularityTaskHistoryHelperMapper;
import com.hubspot.singularity.data.history.SingularityTaskHistoryUpdate.SingularityTaskUpdateMapper;

public interface HistoryJDBI {

  @SqlUpdate("INSERT INTO requestHistory (requestId, request, createdAt, requestState, user) VALUES (:requestId, :request, :createdAt, :requestState, :user)")
  void insertRequestHistory(@Bind("requestId") String requestId, @Bind("request") byte[] request, @Bind("createdAt") Date createdAt, @Bind("requestState") String requestState, @Bind("user") String user);
  
  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, task, status, createdAt) VALUES (:requestId, :taskId, :task, :status, :createdAt)")
  void insertTaskHistory(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("task") byte[] task, @Bind("status") String status, @Bind("createdAt") Date createdAt);

  @SqlUpdate("INSERT INTO taskUpdates (taskId, status, message, createdAt) VALUES (:taskId, :status, :message, :createdAt)")
  void insertTaskUpdate(@Bind("taskId") String taskId, @Bind("status") String status, @Bind("message") String message, @Bind("createdAt") Date createdAt);
  
  @Mapper(SingularityTaskUpdateMapper.class)
  @SqlQuery("SELECT status, message, createdAt FROM taskUpdates WHERE taskId = :taskId")
  List<SingularityTaskHistoryUpdate> getTaskUpdates(@Bind("taskId") String taskId);
  
  @Mapper(SingularityTaskHistoryHelperMapper.class)
  @SqlQuery("SELECT createdAt, task FROM taskHistory WHERE taskId = :taskId")
  SingularityTaskHistoryHelper getTaskHistoryForTask(@Bind("taskId") String taskId);
  
  @Mapper(SingularityTaskIdMapper.class)
  @SqlQuery("SELECT taskId FROM taskHistory WHERE requestId = :requestId")
  List<SingularityTaskId> getTaskHistoryForRequest(@Bind("requestId") String requestId);
  
  @Mapper(SingularityTaskIdMapper.class)
  @SqlQuery("SELECT taskId FROM taskHistory WHERE requestId LIKE CONCAT('%', CONCAT(:requestIdLike, '%'))")
  List<SingularityTaskId> getTaskHistoryForRequestLike(@Bind("requestIdLike") String requestIdLike);
  
  @Mapper(SingularityRequestHistoryMapper.class)
  @SqlQuery("SELECT request, createdAt, requestState, user FROM requestHistory WHERE requestId = :requestId")
  List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId);
  
  @Mapper(SingularityRequestHistoryMapper.class)
  @SqlQuery("SELECT request, createdAt, requestState, user FROM requestHistory WHERE requestId LIKE CONCAT('%', CONCAT(:requestIdLike, '%'))")
  List<SingularityRequestHistory> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike);
  
  void close();

  
}
