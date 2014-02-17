package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.*;
import com.hubspot.singularity.SingularityPendingRequestId.PendingType;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.RequestManager.PersistResult;
import com.hubspot.singularity.data.SingularityRequestValidator;
import com.hubspot.singularity.data.history.HistoryManager;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

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
    
    if (requestManager.isRequestPaused(request.getId())) {
      requestManager.deletePausedRequest(request.getId());
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequestId(request.getId()));
  
    historyManager.saveRequestHistoryUpdate(request, result == PersistResult.CREATED ? RequestState.CREATED : RequestState.UPDATED, user);
    
    return request;
  }
  
  @POST
  @Path("/request/{requestId}/bounce")
  public void bounce(@PathParam("requestId") String requestId) {
    SingularityRequest request = fetchRequest(requestId);
    
    if (request.isScheduled()) {
      throw new BadRequestException(String.format("Can not request a bounce of a scheduled request (%s - %s)", request.getId(), request.getSchedule()));
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId, PendingType.BOUNCE));
  }
  
  @POST
  @Path("/request/{requestId}/run")
  public void scheduleImmediately(@PathParam("requestId") String requestId) {
    SingularityRequest request = fetchRequest(requestId);
    
    PendingType pendingType = null;
    
    if (request.isScheduled()) {
      pendingType = PendingType.IMMEDIATE;
    } else if (request.isOneOff()) {
      pendingType = PendingType.ONEOFF;
    } else {
      throw new BadRequestException(String.format("Can not request an immediate run of a non-scheduled / always running request (%s)", request));
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId, pendingType));
  }
  
  @POST
  @Path("/request/{requestId}/pause")
  public void pause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.PAUSING, System.currentTimeMillis(), requestId));
    
    if (result == SingularityCreateResult.CREATED) {
      historyManager.saveRequestHistoryUpdate(request, RequestState.PAUSED, user);
    } else {
      throw new ConflictException(String.format("A cleanup/pause request for %s failed to create because it was in state %s", requestId, result));
    }
  }
  
  @POST
  @Path("/request/{requestId}/unpause")
  public SingularityRequest unpause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityRequest> request = requestManager.unpause(requestId);
    
    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId, PendingType.UNPAUSED));
  
    historyManager.saveRequestHistoryUpdate(request.get(), RequestState.UNPAUSED, user);
  
    return request.get();
  }

  @GET
  @Path("/active")
  public List<SingularityRequest> getActiveRequests() {
    return requestManager.getActiveRequests();
  }
  
  @GET
  @Path("/paused")
  public List<SingularityRequest> getPausedRequests() {
    return requestManager.getPausedRequests();
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
  
  @GET
  @Path("/request/{requestId}")
  public SingularityRequest getRequest(@PathParam("requestId") String requestId) {
    SingularityRequest request = fetchRequest(requestId);
    
    return request;
  }
  
  private SingularityRequest fetchRequest(String requestId) {
    Optional<SingularityRequest> request = requestManager.fetchRequest(requestId);
    
    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }
    
    return request.get();
  }
  
  private NotFoundException handleNoMatchingRequest(String requestId) {
    throw new NotFoundException("Couldn't find request with id: " + requestId);
  }
  
  @DELETE
  @Path("/request/{requestId}/paused")
  public SingularityRequest deletedRequestPaused(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityRequest> request = requestManager.fetchPausedRequest(requestId);
    
    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }

    SingularityDeleteResult result = requestManager.deletePausedRequest(requestId);
    
    if (result != SingularityDeleteResult.DELETED) {
      throw handleNoMatchingRequest(requestId);
    }
    
    historyManager.saveRequestHistoryUpdate(request.get(), RequestState.DELETED, user);
    
    return request.get();
  }
  
  @DELETE
  @Path("/request/{requestId}")
  public SingularityRequest deleteRequest(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityRequest> request = requestManager.deleteRequest(user, requestId);
  
    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }
    
    historyManager.saveRequestHistoryUpdate(request.get(), RequestState.DELETED, user);
    
    return request.get();
  }

}
