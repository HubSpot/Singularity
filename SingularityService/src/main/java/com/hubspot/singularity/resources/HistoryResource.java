package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.HistoryManager;
import com.sun.jersey.api.NotFoundException;

@Path("/history")
@Produces({ MediaType.APPLICATION_JSON })
public class HistoryResource {
  
  private final HistoryManager historyManager;
  
  @Inject
  public HistoryResource(HistoryManager historyManager) {
    this.historyManager = historyManager;
  }
  
  @GET
  @Path("/task/{taskId}")
  public SingularityTaskHistory getHistoryForTask(@PathParam("taskId") String taskId) {
    Optional<SingularityTaskHistory> history = historyManager.getTaskHistory(taskId);
  
    if (!history.isPresent()) {
      throw new NotFoundException(String.format("No history for task %s", taskId));
    }
    
    return history.get();
  }
  
  private Integer getLimitCount(Integer countParam) {
    if (countParam == null) {
      return 100;
    }
    
    if (countParam < 0) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }
    
    if (countParam > 1000) {
      return 1000;
    }
    
    return countParam;
  }
  
  private Integer getLimitStart(Integer limitCount, Integer pageParam) {
    if (pageParam == null) {
      return 0;
    }
    
    if (pageParam < 1) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }
    
    return limitCount * (pageParam - 1);
  }
  
  @GET
  @Path("/request/{requestId}/tasks")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@PathParam("requestId") String requestId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
    
    return historyManager.getTaskHistoryForRequest(requestId, limitStart, limitCount);
  }
  
  @GET
  @Path("/tasks/search")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
    
    return historyManager.getTaskHistoryForRequestLike(requestIdLike, limitStart, limitCount);
  }
  
  @GET
  @Path("/request/{requestId}/requests")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(@PathParam("requestId") String requestId) {
    return historyManager.getRequestHistory(requestId);
  }
  
  @GET
  @Path("/requests/search")
  public List<SingularityRequestHistory> getRequestHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
 
    return historyManager.getRequestHistoryLike(requestIdLike, limitStart, limitCount);
  }
  
}
