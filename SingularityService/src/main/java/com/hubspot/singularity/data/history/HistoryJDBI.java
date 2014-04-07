package com.hubspot.singularity.data.history;

import java.util.Date;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.Define;
import org.skife.jdbi.v2.sqlobject.customizers.Mapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.mappers.SingularityBytesMapper;
import com.hubspot.singularity.data.history.mappers.SingularityDeployHistoryLiteMapper;
import com.hubspot.singularity.data.history.mappers.SingularityRequestHistoryMapper;
import com.hubspot.singularity.data.history.mappers.SingularityTaskIdHistoryMapper;

@UseStringTemplate3StatementLocator
public interface HistoryJDBI {

  @SqlUpdate("INSERT INTO requestHistory (requestId, request, createdAt, requestState, user) VALUES (:requestId, :request, :createdAt, :requestState, :user)")
  void insertRequestHistory(@Bind("requestId") String requestId, @Bind("request") byte[] request, @Bind("createdAt") Date createdAt, @Bind("requestState") String requestState, @Bind("user") String user);

  @SqlUpdate("INSERT INTO deployHistory (requestId, deployId, createdAt, user, deployState, bytes) VALUES (:requestId, :deployId, :createdAt, :user, :deployState, :bytes)")
  void insertDeployHistory(@Bind("requestId") String requestId, @Bind("deployId") String deployId, @Bind("createdAt") Date createdAt, @Bind("user") String user, @Bind("deployState") String deployState,  @Bind("bytes") byte[] bytes);
  
  @SqlUpdate("INSERT INTO taskHistory (requestId, taskId, bytes, updatedAt, lastTaskStatus) VALUES (:requestId, :taskId, :bytes, :updatedAt, :lastTaskStatus)")
  void insertTaskHistory(@Bind("requestId") String requestId, @Bind("taskId") String taskId, @Bind("bytes") byte[] bytes, @Bind("updatedAt") Date updatedAt, @Bind("lastTaskStatus") String lastTaskStatus);
  
  @Mapper(SingularityBytesMapper.class)
  @SqlQuery("SELECT bytes FROM taskHistory WHERE taskId = :taskId")
  byte[] getTaskHistoryForTask(@Bind("taskId") String taskId);

  @Mapper(SingularityBytesMapper.class)
  @SqlQuery("SELECT bytes FROM deployHistory WHERE requestId = :requestId AND deployId = :deployId")
  byte[] getDeployHistoryForDeploy(@Bind("requestId") String requestId, @Bind("deployId") String deployId);
  
  @Mapper(SingularityDeployHistoryLiteMapper.class)
  @SqlQuery("SELECT requestId, deployId, createdAt, user, deployState FROM deployHistory WHERE requestId = :requestId ORDER BY createdAt DESC LIMIT :limitStart, :limitCount")
  List<SingularityDeployHistory> getDeployHistoryForRequest(@Bind("requestId") String requestId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);
  
  @Mapper(SingularityTaskIdHistoryMapper.class)
  @SqlQuery("SELECT taskId, requestId, updatedAt, lastTaskStatus FROM taskHistory WHERE requestId = :requestId ORDER BY updatedAt DESC LIMIT :limitStart, :limitCount")
  List<SingularityTaskIdHistory> getTaskHistoryForRequest(@Bind("requestId") String requestId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);
  
  @Mapper(SingularityRequestHistoryMapper.class)
  @SqlQuery("SELECT request, createdAt, requestState, user FROM requestHistory WHERE requestId = :requestId ORDER BY <orderBy> <orderDirection> LIMIT :limitStart, :limitCount")
  List<SingularityRequestHistory> getRequestHistory(@Bind("requestId") String requestId, @Define("orderBy") String orderBy, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);
  
  @Mapper(SingularityRequestHistoryMapper.class)
  @SqlQuery("SELECT request, createdAt, requestState, user FROM requestHistory WHERE requestId LIKE CONCAT(:requestIdLike, '%') ORDER BY <orderBy> <orderDirection> LIMIT :limitStart, :limitCount")
  List<SingularityRequestHistory> getRequestHistoryLike(@Bind("requestIdLike") String requestIdLike, @Define("orderBy") String orderBy, @Define("orderDirection") String orderDirection, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);
  
  void close();
  
}
