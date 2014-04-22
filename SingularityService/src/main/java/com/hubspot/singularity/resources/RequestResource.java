package com.hubspot.singularity.resources;

import java.util.List;
import java.util.Map;

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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.jackson.jaxrs.PropertyFiltering;
import com.hubspot.singularity.DeployState;
import com.hubspot.singularity.LoadBalancerState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestState;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DeployManager.ConditionalPersistResult;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.RequestManager.PersistResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.history.HistoryManager;
import com.sun.jersey.api.NotFoundException;

@Path("/requests")
@Produces({ MediaType.APPLICATION_JSON })
public class RequestResource {

  private final SingularityValidator validator;
  
  private final RequestManager requestManager;
  private final DeployManager deployManager;
  private final HistoryManager historyManager;
  
  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, RequestManager requestManager, HistoryManager historyManager) {
    this.validator = validator;
    
    this.deployManager = deployManager;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequest submit(SingularityRequest request, @QueryParam("user") Optional<String> user) {
    if (request.getId() == null) {
      throw WebExceptions.badRequest("Request must have an Id");
    }
    
    Optional<SingularityRequest> maybeOldRequest = requestManager.fetchRequest(request.getId());
    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest);
    
    PersistResult result = requestManager.persistRequest(newRequest);
    
    if (requestManager.isRequestPaused(request.getId())) {
      requestManager.deletePausedRequest(request.getId());
    }
    
    historyManager.saveRequestHistoryUpdate(newRequest, result == PersistResult.CREATED ? RequestState.CREATED : RequestState.UPDATED, user);
    
    checkReschedule(newRequest, maybeOldRequest);
    
    return newRequest;
  }
  
  private void checkReschedule(SingularityRequest newRequest, Optional<SingularityRequest> maybeOldRequest) {
    if (!maybeOldRequest.isPresent()) {
      return;
    }
    
    if (shouldReschedule(newRequest, maybeOldRequest.get())) {
      Optional<String> maybeDeployId = deployManager.getInUseDeployId(newRequest.getId());
      
      if (maybeDeployId.isPresent()) {
        requestManager.addToPendingQueue(new SingularityPendingRequest(newRequest.getId(), maybeDeployId.get(), PendingType.UPDATED_REQUEST));
      }
    }
  }
  
  private boolean shouldReschedule(SingularityRequest newRequest, SingularityRequest oldRequest) {
    if (newRequest.getInstancesSafe() != oldRequest.getInstancesSafe()) {
      return true;
    }
    if (newRequest.isScheduled() && oldRequest.isScheduled()) {
      if (!newRequest.getSchedule().get().equals(oldRequest.getSchedule().get())) {
        return true;
      }
    }
    
    return false;
  }
  
  private Optional<SingularityDeploy> fillDeploy(Optional<SingularityDeployMarker> deployMarker) {
    if (!deployMarker.isPresent()) {
      return Optional.absent();
    }
    
    return deployManager.getDeploy(deployMarker.get().getRequestId(), deployMarker.get().getDeployId());
  }
  
  private SingularityRequestParent fillEntireRequest(SingularityRequest request) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(request.getId());
    
    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();
    
    if (requestDeployState.isPresent()) {
      activeDeploy = fillDeploy(requestDeployState.get().getActiveDeploy());
      pendingDeploy = fillDeploy(requestDeployState.get().getPendingDeploy());
    }
    
    return new SingularityRequestParent(request, requestDeployState, activeDeploy, pendingDeploy);
  }
  
  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @Path("/request/{requestId}/deploy")
  public SingularityRequestParent deploy(@PathParam("requestId") String requestId, SingularityDeploy pendingDeploy, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    validator.checkDeploy(request, pendingDeploy);

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(requestId, pendingDeploy.getId(), System.currentTimeMillis(), user);
    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(deployMarker, Optional.<LoadBalancerState> absent());
    
    if (deployManager.createPendingDeploy(pendingDeployObj) == SingularityCreateResult.EXISTED) {
      throw WebExceptions.conflict("Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)", requestId, deployManager.getPendingDeploy(requestId).orNull());
    }
    
    ConditionalPersistResult persistResult = deployManager.persistDeploy(request, deployMarker, pendingDeploy);
    
    if (persistResult == ConditionalPersistResult.STATE_CHANGED) {
      throw WebExceptions.conflict("State changed while persisting deploy - try again or contact an administrator. deploy state: %s (marker: %s)", deployManager.getRequestDeployState(requestId).orNull(), deployManager.getPendingDeploy(requestId).orNull());
    }
    
    if (!request.isDeployable()) {
      deployManager.saveDeployResult(deployMarker, new SingularityDeployResult(DeployState.SUCCEEDED, Optional.<String> absent(), deployMarker.getTimestamp()));
      
      deployManager.deletePendingDeploy(pendingDeployObj);
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deployMarker.getDeployId(), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.NEW_DEPLOY)); 
    
    return fillEntireRequest(request);
  }
  
  @DELETE
  @Path("/request/{requestId}/deploy/{deployId}")
  public SingularityRequestParent cancelDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(request.getId());
    
    if (!deployState.isPresent() || !deployState.get().getPendingDeploy().isPresent() || !deployState.get().getPendingDeploy().get().getDeployId().equals(deployId)) {
      throw WebExceptions.badRequest("Request id %s does not have a pending deploy with id %s", requestId, deployId);
    }
    
    deployManager.cancelDeploy(new SingularityDeployMarker(requestId, deployId, System.currentTimeMillis(), user));
    
    return fillEntireRequest(request);
  }
  
  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);
    
    if (!maybeDeployId.isPresent()) {
      throw WebExceptions.badRequest("Can not schedule a request (%s) with no deploy", requestId);
    }
    
    return maybeDeployId.get();
  }
  
  @POST
  @Path("/request/{requestId}/bounce")
  public void bounce(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    if (request.isScheduled()) {
      throw WebExceptions.badRequest("Can not bounce a scheduled request (%s)", request);
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), PendingType.BOUNCE));
  }
  
  @POST
  @Path("/request/{requestId}/run")
  public void scheduleImmediately(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user, String commandLineArgs) {
    SingularityRequest request = fetchRequest(requestId);
    Optional<String> maybeCmdLineArgs = Optional.absent();
    
    PendingType pendingType = null;
    
    if (request.isScheduled()) {
      pendingType = PendingType.IMMEDIATE;
    } else if (request.isOneOff()) {
      pendingType = PendingType.ONEOFF;
      
      if (!Strings.isNullOrEmpty(commandLineArgs)) {
        maybeCmdLineArgs = Optional.of(commandLineArgs);
      }
    } else {
      throw WebExceptions.badRequest("Can not request an immediate run of a non-scheduled / always running request (%s)", request);
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(), maybeCmdLineArgs, user, pendingType));
  }
  
  @POST
  @Path("/request/{requestId}/pause")
  public void pause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.PAUSING, System.currentTimeMillis(), requestId));
    
    if (result == SingularityCreateResult.CREATED) {
      historyManager.saveRequestHistoryUpdate(request, RequestState.PAUSED, user);
    } else {
      throw WebExceptions.conflict("A cleanup/pause request for %s failed to create because it was in state %s", requestId, result);
    }
  }
  
  @POST
  @Path("/request/{requestId}/unpause")
  public SingularityRequest unpause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    Optional<SingularityRequest> request = requestManager.unpause(requestId);
    
    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }
    
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);
    
    if (maybeDeployId.isPresent()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.UNPAUSED));
    }
  
    historyManager.saveRequestHistoryUpdate(request.get(), RequestState.UNPAUSED, user);
  
    return request.get();
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  public List<SingularityRequestParent> getActiveRequests() {
    return getRequestsWithDeployState(requestManager.getActiveRequests());
  }
  
  private List<SingularityRequestParent> getRequestsWithDeployState(List<SingularityRequest> requests) {
    List<String> requestIds = Lists.newArrayListWithCapacity(requests.size());
    for (SingularityRequest request : requests) {
      requestIds.add(request.getId());
    }
    
    List<SingularityRequestParent> parents = Lists.newArrayListWithCapacity(requests.size());
  
    Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(requestIds);
    
    for (SingularityRequest request : requests) {
      Optional<SingularityRequestDeployState> deployState = Optional.fromNullable(deployStates.get(request.getId()));
      parents.add(new SingularityRequestParent(request, deployState, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeploy> absent()));
    }
    
    return parents;  
  }
  
  @GET
  @PropertyFiltering
  @Path("/paused")
  public List<SingularityRequestParent> getPausedRequests() {
    return getRequestsWithDeployState(requestManager.getPausedRequests());
  }
  
  @GET
  @PropertyFiltering
  @Path("/queued/pending")
  public List<SingularityPendingRequest> getPendingRequests() {
    return requestManager.getPendingRequests();
  }
  
  @GET
  @PropertyFiltering
  @Path("/queued/cleanup")
  public List<SingularityRequestCleanup> getCleanupRequests() {
    return requestManager.getCleanupRequests();
  }
  
  @GET
  @Path("/request/{requestId}")
  public SingularityRequestParent getRequest(@PathParam("requestId") String requestId) {
    SingularityRequest request = fetchRequest(requestId);
    
    return fillEntireRequest(request);
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
