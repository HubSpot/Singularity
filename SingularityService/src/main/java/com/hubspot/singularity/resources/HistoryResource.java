package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.data.history.HistoryManager;
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
  @Path("/request/{requestName}")
  public List<SingularityPendingTaskId> getHistoryForRequest(@PathParam("requestName") String requestName) {
    return historyManager.getTaskHistoryForRequest(requestName);
  }
  
}
