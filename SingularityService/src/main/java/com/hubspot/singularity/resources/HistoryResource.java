package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.SingularityRequestHistory;
import com.hubspot.singularity.data.history.SingularityTaskHistory;

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
    return historyManager.getTaskHistory(taskId);
  }
  
  @GET
  @Path("/request/{requestId}/tasks")
  public List<SingularityTaskId> getTaskHistoryForRequest(@PathParam("requestId") String requestId) {
    return historyManager.getTaskHistoryForRequest(requestId);
  }
  
  @GET
  @Path("/tasks/search")
  public List<SingularityTaskId> getTaskHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike) {
    return historyManager.getTaskHistoryForRequestLike(requestIdLike);
  }
  
  @GET
  @Path("/request/{requestId}/requests")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(@PathParam("requestId") String requestId) {
    return historyManager.getRequestHistory(requestId);
  }
  
  @GET
  @Path("/requests/search")
  public List<SingularityRequestHistory> getRequestHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike) {
    return historyManager.getRequestHistoryLike(requestIdLike);
  }
  
}
