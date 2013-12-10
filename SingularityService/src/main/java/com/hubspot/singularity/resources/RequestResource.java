package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingRequestId;
import com.hubspot.singularity.SingularityPendingRequestId.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.RequestManager.PersistResult;
import com.hubspot.singularity.data.SingularityRequestValidator;
import com.hubspot.singularity.data.history.HistoryManager;

@Path("/requests")
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource {

  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  
  @Inject
  public RequestResource(RequestManager requestManager, HistoryManager historyManager) {
    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequest submit(SingularityRequest request, @QueryParam("user") Optional<String> user) {
    SingularityRequestValidator validator = new SingularityRequestValidator(request);
    request = validator.buildValidRequest();
    
    PersistResult result = requestManager.persistRequest(request);
    
    requestManager.addToPendingQueue(new SingularityPendingRequestId(request.getId()));
  
    historyManager.saveRequestHistoryUpdate(request, result == PersistResult.CREATED ? RequestState.CREATED : RequestState.UPDATED, user);
    
    return request;
  }
  
  @POST
  @Path("/request/{requestId}/run")
  public void scheduleImmediately(@PathParam("requestId") String requestId) {
     requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId, PendingType.IMMEDIATE));
  }

  @GET
  public List<SingularityRequest> getKnownRequests() {
    return requestManager.getKnownRequests();
  }
  
  @GET
  @Path("/queued/pending")
  public List<SingularityPendingRequestId> getPendingRequests() {
    return requestManager.getPendingRequestIds();
  }
  
  @GET
  @Path("/queued/cleanup")
  public List<SingularityRequestCleanup> getCleanupRequests() {
    return requestManager.getCleanupRequests();
  }
  
  @DELETE
  @Path("/request/{requestId}")
  public Optional<SingularityRequest> getHistoryForTask(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityRequest> request = requestManager.deleteRequest(user, requestId);
  
    if (request.isPresent()) {
      historyManager.saveRequestHistoryUpdate(request.get(), RequestState.DELETED, user);
    }
    
    return request;
  }

}
