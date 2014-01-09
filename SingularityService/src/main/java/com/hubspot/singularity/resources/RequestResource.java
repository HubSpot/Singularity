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
import com.hubspot.singularity.BadRequestException;
import com.hubspot.singularity.SingularityPendingRequestId;
import com.hubspot.singularity.SingularityPendingRequestId.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.data.CuratorManager.CreateResult;
import com.hubspot.singularity.data.CuratorManager.DeleteResult;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.RequestManager.PersistResult;
import com.hubspot.singularity.data.SingularityRequestValidator;
import com.hubspot.singularity.data.history.HistoryManager;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

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
    fetchRequest(requestId);
    
    requestManager.addToPendingQueue(new SingularityPendingRequestId(requestId, PendingType.IMMEDIATE));
  }
  
  @POST
  @Path("/request/{requestId}/pause")
  public void pause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    CreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.PAUSING, System.currentTimeMillis(), requestId));
    
    if (result == CreateResult.CREATED) {
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
    
    DeleteResult result = requestManager.deletePausedRequest(requestId);
    
    if (result != DeleteResult.DELETED) {
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
