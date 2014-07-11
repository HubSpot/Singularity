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
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.DeployManager.ConditionalSaveResult;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.history.HistoryManager;
import com.sun.jersey.api.NotFoundException;

@Path(SingularityService.API_BASE_PATH + "/requests")
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
  public SingularityRequestParent submit(SingularityRequest request, @QueryParam("user") Optional<String> user) {
    if (request.getId() == null) {
      throw WebExceptions.badRequest("Request must have an id");
    }
    
    Optional<SingularityRequestWithState> maybeOldRequestWithState = requestManager.getRequest(request.getId());
    Optional<SingularityRequest> maybeOldRequest = maybeOldRequestWithState.isPresent() ? Optional.of(maybeOldRequestWithState.get().getRequest()) : Optional.<SingularityRequest> absent();
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(request.getId());
    
    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();
    
    if (requestDeployState.isPresent()) {
      if (requestDeployState.get().getActiveDeploy().isPresent()) {
        activeDeploy = deployManager.getDeploy(request.getId(), requestDeployState.get().getActiveDeploy().get().getDeployId());
      }
      if (requestDeployState.get().getPendingDeploy().isPresent()) {
        pendingDeploy = deployManager.getDeploy(request.getId(), requestDeployState.get().getPendingDeploy().get().getDeployId());
      }
    }
    
    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest, activeDeploy, pendingDeploy);
    
    if (!maybeOldRequest.isPresent() && requestManager.getCleanupRequest(request.getId()).isPresent()) {
      throw WebExceptions.conflict("Request %s is currently cleaning. Try again after a few moments", request.getId());
    }
    
    SingularityCreateResult result = requestManager.saveRequest(newRequest);
    
    historyManager.saveRequestHistoryUpdate(newRequest, result == SingularityCreateResult.CREATED ? RequestHistoryType.CREATED : RequestHistoryType.UPDATED, user);
    
    checkReschedule(newRequest, maybeOldRequest);
    
    return fillEntireRequest(fetchRequestWithState(request.getId()));
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
  
  private SingularityRequestParent fillEntireRequest(SingularityRequestWithState requestWithState) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());
    
    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();
    
    if (requestDeployState.isPresent()) {
      activeDeploy = fillDeploy(requestDeployState.get().getActiveDeploy());
      pendingDeploy = fillDeploy(requestDeployState.get().getPendingDeploy());
    }
    
    Optional<SingularityPendingDeploy> pendingDeployState = deployManager.getPendingDeploy(requestWithState.getRequest().getId());
    
    return new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), requestDeployState, activeDeploy, pendingDeploy, pendingDeployState);
  }
  
  private void checkRequestStateNotPaused(SingularityRequestWithState requestWithState, String action) {
    if (requestWithState.getState() == RequestState.PAUSED) {
      throw WebExceptions.conflict("Request %s is paused. Unable to %s (it must be manually unpaused first)", requestWithState.getRequest().getId(), action);
    }
  }
  
  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @Path("/request/{requestId}/deploy")
  public SingularityRequestParent deploy(@PathParam("requestId") String requestId, SingularityDeploy pendingDeploy, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    SingularityRequest request = requestWithState.getRequest();
    
    checkRequestStateNotPaused(requestWithState, "deploy");
    
    validator.checkDeploy(request, pendingDeploy);

    SingularityDeployMarker deployMarker = new SingularityDeployMarker(requestId, pendingDeploy.getId(), System.currentTimeMillis(), user);
    SingularityPendingDeploy pendingDeployObj = new SingularityPendingDeploy(deployMarker, Optional.<SingularityLoadBalancerUpdate> absent(), DeployState.WAITING);
    
    if (deployManager.createPendingDeploy(pendingDeployObj) == SingularityCreateResult.EXISTED) {
      throw WebExceptions.conflict("Pending deploy already in progress for %s - cancel it or wait for it to complete (%s)", requestId, deployManager.getPendingDeploy(requestId).orNull());
    }
    
    ConditionalSaveResult persistResult = deployManager.saveDeploy(request, deployMarker, pendingDeploy);
    
    if (persistResult == ConditionalSaveResult.STATE_CHANGED) {
      throw WebExceptions.conflict("State changed while persisting deploy - try again or contact an administrator. deploy state: %s (marker: %s)", deployManager.getRequestDeployState(requestId).orNull(), deployManager.getPendingDeploy(requestId).orNull());
    }
    
    if (!request.isDeployable()) {
      deployManager.saveDeployResult(deployMarker, new SingularityDeployResult(DeployState.SUCCEEDED));
      
      deployManager.deletePendingDeploy(requestId);
    }
    
    if (shouldAddToPendingQueue(request)) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, deployMarker.getDeployId(), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.NEW_DEPLOY)); 
    }
    
    return fillEntireRequest(requestWithState);
  }
  
  @DELETE
  @Path("/request/{requestId}/deploy/{deployId}")
  public SingularityRequestParent cancelDeploy(@PathParam("requestId") String requestId, @PathParam("deployId") String deployId, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    
    Optional<SingularityRequestDeployState> deployState = deployManager.getRequestDeployState(requestWithState.getRequest().getId());
    
    if (!deployState.isPresent() || !deployState.get().getPendingDeploy().isPresent() || !deployState.get().getPendingDeploy().get().getDeployId().equals(deployId)) {
      throw WebExceptions.badRequest("Request %s does not have a pending deploy %s", requestId, deployId);
    }
    
    deployManager.createCancelDeployRequest(new SingularityDeployMarker(requestId, deployId, System.currentTimeMillis(), user));
    
    return fillEntireRequest(requestWithState);
  }
  
  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);
    
    if (!maybeDeployId.isPresent()) {
      throw WebExceptions.conflict("Can not schedule a request (%s) with no deploy", requestId);
    }
    
    return maybeDeployId.get();
  }
  
  private boolean shouldAddToPendingQueue(SingularityRequest request) {
    return !request.isOneOff();
  }
  
  @POST
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent bounce(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
        
    if (requestWithState.getRequest().isScheduled() || requestWithState.getRequest().isOneOff()) {
      throw WebExceptions.badRequest("Can not bounce a scheduled or one-off request (%s)", requestWithState);
    }
    
    checkRequestStateNotPaused(requestWithState, "bounce");
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.BOUNCE));
  
    return fillEntireRequest(requestWithState);
  }
  
  @POST
  @Path("/request/{requestId}/run")
  public SingularityRequestParent scheduleImmediately(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user, String commandLineArgs) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    
    checkRequestStateNotPaused(requestWithState, "run now");
    
    Optional<String> maybeCmdLineArgs = Optional.absent();
    
    PendingType pendingType = null;
    
    if (requestWithState.getRequest().isScheduled()) {
      pendingType = PendingType.IMMEDIATE;
    } else if (requestWithState.getRequest().isOneOff()) {
      pendingType = PendingType.ONEOFF;
    } else {
      throw WebExceptions.badRequest("Can not request an immediate run of a non-scheduled / always running request (%s)", requestWithState.getRequest());
    }
    
    if (!Strings.isNullOrEmpty(commandLineArgs)) {
      maybeCmdLineArgs = Optional.of(commandLineArgs);
    }
    
    requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, getAndCheckDeployId(requestId), System.currentTimeMillis(), maybeCmdLineArgs, user, pendingType));
  
    return fillEntireRequest(requestWithState);
  }
  
  @POST
  @Path("/request/{requestId}/pause")
  public SingularityRequestParent pause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);
    
    checkRequestStateNotPaused(requestWithState, "pause");
    
    requestManager.pause(requestWithState.getRequest());
    
    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.PAUSING, System.currentTimeMillis(), requestId));
    
    if (result == SingularityCreateResult.CREATED) {
      historyManager.saveRequestHistoryUpdate(requestWithState.getRequest(), RequestHistoryType.PAUSED, user);
    } else {
      throw WebExceptions.conflict("A cleanup/pause request for %s failed to create because it was in state %s", requestId, result);
    }
    
    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.PAUSED));
  }
  
  @POST
  @Path("/request/{requestId}/unpause")
  public SingularityRequestParent unpause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    if (requestWithState.getState() != RequestState.PAUSED) {
      throw WebExceptions.conflict("Request %s is not in PAUSED state, it is in %s", requestId, requestWithState.getState());
    }
    
    requestManager.makeActive(requestWithState.getRequest());
    
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);
    
    if (maybeDeployId.isPresent() && shouldAddToPendingQueue(requestWithState.getRequest())) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.UNPAUSED));
    }
  
    historyManager.saveRequestHistoryUpdate(requestWithState.getRequest(), RequestHistoryType.UNPAUSED, user);
  
    return fillEntireRequest(new SingularityRequestWithState(requestWithState.getRequest(), RequestState.ACTIVE));
  }

  @GET
  @PropertyFiltering
  @Path("/active")
  public List<SingularityRequestParent> getActiveRequests() {
    return getRequestsWithDeployState(requestManager.getActiveRequests());
  }
  
  private List<SingularityRequestParent> getRequestsWithDeployState(Iterable<SingularityRequestWithState> requests) {
    List<String> requestIds = Lists.newArrayList();
    for (SingularityRequestWithState requestWithState : requests) {
      requestIds.add(requestWithState.getRequest().getId());
    }
    
    List<SingularityRequestParent> parents = Lists.newArrayListWithCapacity(requestIds.size());
  
    Map<String, SingularityRequestDeployState> deployStates = deployManager.getRequestDeployStatesByRequestIds(requestIds);
    
    for (SingularityRequestWithState requestWithState : requests) {
      Optional<SingularityRequestDeployState> deployState = Optional.fromNullable(deployStates.get(requestWithState.getRequest().getId()));
      parents.add(new SingularityRequestParent(requestWithState.getRequest(), requestWithState.getState(), deployState, Optional.<SingularityDeploy> absent(), Optional.<SingularityDeploy> absent(), Optional.<SingularityPendingDeploy> absent()));
    }
    
    return parents;  
  }
  
  @GET
  @PropertyFiltering
  @Path("/paused")
  public Iterable<SingularityRequestParent> getPausedRequests() {
    return getRequestsWithDeployState(requestManager.getPausedRequests());
  }
  
  @GET
  @PropertyFiltering
  @Path("/cooldown")
  public Iterable<SingularityRequestParent> getCooldownRequests() {
    return getRequestsWithDeployState(requestManager.getCooldownRequests());
  }
  
  @GET
  @PropertyFiltering
  @Path("/")
  public Iterable<SingularityRequestParent> getRequests() {
    return getRequestsWithDeployState(requestManager.getRequests());
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
    return fillEntireRequest(fetchRequestWithState(requestId));
  }
  
  private SingularityRequest fetchRequest(String requestId) {
    return fetchRequestWithState(requestId).getRequest();
  }
  
  private SingularityRequestWithState fetchRequestWithState(String requestId) {
    Optional<SingularityRequestWithState> request = requestManager.getRequest(requestId);
    
    if (!request.isPresent()) {
      throw handleNoMatchingRequest(requestId);
    }
    
    return request.get();
  }
  
  private NotFoundException handleNoMatchingRequest(String requestId) {
    throw WebExceptions.notFound("Couldn't find request with id %s", requestId);
  }
  
  @DELETE
  @Path("/request/{requestId}")
  public SingularityRequest deleteRequest(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);
    
    requestManager.deleteRequest(user, requestId);
    historyManager.saveRequestHistoryUpdate(request, RequestHistoryType.DELETED, user);
    
    return request;
  }

}
