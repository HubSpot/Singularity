package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityRequestValidator;
import com.hubspot.singularity.scheduler.SingularityScheduler;

@Path("/request")
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource {

  private final RequestManager requestManager;
  private final SingularityScheduler scheduler;
  
  @Inject
  public RequestResource(RequestManager requestManager, SingularityScheduler scheduler) {
    this.requestManager = requestManager;
    this.scheduler = scheduler;
  }

  static class SingularitySubmitRequestResponse {

    private final SingularityRequest request;
    private final List<SingularityPendingTaskId> scheduledTasks;

    public SingularitySubmitRequestResponse(SingularityRequest request, List<SingularityPendingTaskId> scheduledTasks) {
      super();
      this.request = request;
      this.scheduledTasks = scheduledTasks;
    }

    public SingularityRequest getRequest() {
      return request;
    }

    public List<SingularityPendingTaskId> getScheduledTasks() {
      return scheduledTasks;
    }

  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularitySubmitRequestResponse submit(SingularityRequest request) {
    SingularityRequestValidator validator = new SingularityRequestValidator(request);
    request = validator.buildValidRequest();
    
    requestManager.persistRequest(request);
  
    List<SingularityPendingTaskId> scheduledTasks = scheduler.scheduleTasks(request);
    
    return new SingularitySubmitRequestResponse(request, scheduledTasks);
  }

  @GET
  public List<SingularityRequest> getKnownRequests() {
    return requestManager.getKnownRequests();
  }

  @DELETE
  @Path("/{requestName}")
  public Optional<SingularityRequest> getHistoryForTask(@PathParam("requestName") String requestName) {
    return requestManager.deleteRequest(requestName);
  }

}
