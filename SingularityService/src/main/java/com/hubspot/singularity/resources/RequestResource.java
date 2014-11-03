package com.hubspot.singularity.resources;

import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import com.hubspot.singularity.RequestState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestCleanup.RequestCleanupType;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityRequestInstances;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.data.DeployManager;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.smtp.SingularityMailer;
import com.wordnik.swagger.annotations.Api;

@Path(RequestResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity requests.", value=RequestResource.PATH)
public class RequestResource extends AbstractRequestResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/requests/";

  private final SingularityValidator validator;

  private final SingularityMailer mailer;
  private final RequestManager requestManager;
  private final DeployManager deployManager;

  @Inject
  public RequestResource(SingularityValidator validator, DeployManager deployManager, RequestManager requestManager, SingularityMailer mailer) {
    super(requestManager, deployManager);

    this.validator = validator;
    this.mailer = mailer;
    this.deployManager = deployManager;
    this.requestManager = requestManager;
  }

  private static class SingularityRequestDeployHolder {

    private final Optional<SingularityDeploy> activeDeploy;
    private final Optional<SingularityDeploy> pendingDeploy;

    public SingularityRequestDeployHolder(Optional<SingularityDeploy> activeDeploy, Optional<SingularityDeploy> pendingDeploy) {
      this.activeDeploy = activeDeploy;
      this.pendingDeploy = pendingDeploy;
    }

    public Optional<SingularityDeploy> getActiveDeploy() {
      return activeDeploy;
    }

    public Optional<SingularityDeploy> getPendingDeploy() {
      return pendingDeploy;
    }

  }

  private SingularityRequestDeployHolder getDeployHolder(String requestId) {
    Optional<SingularityRequestDeployState> requestDeployState = deployManager.getRequestDeployState(requestId);

    Optional<SingularityDeploy> activeDeploy = Optional.absent();
    Optional<SingularityDeploy> pendingDeploy = Optional.absent();

    if (requestDeployState.isPresent()) {
      if (requestDeployState.get().getActiveDeploy().isPresent()) {
        activeDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getActiveDeploy().get().getDeployId());
      }
      if (requestDeployState.get().getPendingDeploy().isPresent()) {
        pendingDeploy = deployManager.getDeploy(requestId, requestDeployState.get().getPendingDeploy().get().getDeployId());
      }
    }

    return new SingularityRequestDeployHolder(activeDeploy, pendingDeploy);
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  public SingularityRequestParent submit(SingularityRequest request, @QueryParam("user") Optional<String> user) {
    if (request.getId() == null) {
      throw WebExceptions.badRequest("Request must have an id");
    }

    Optional<SingularityRequestWithState> maybeOldRequestWithState = requestManager.getRequest(request.getId());
    Optional<SingularityRequest> maybeOldRequest = maybeOldRequestWithState.isPresent() ? Optional.of(maybeOldRequestWithState.get().getRequest()) : Optional.<SingularityRequest> absent();

    SingularityRequestDeployHolder deployHolder = getDeployHolder(request.getId());

    SingularityRequest newRequest = validator.checkSingularityRequest(request, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy());

    if (!maybeOldRequest.isPresent() && requestManager.getCleanupRequest(request.getId()).isPresent()) {
      throw WebExceptions.conflict("Request %s is currently cleaning. Try again after a few moments", request.getId());
    }

    requestManager.activate(newRequest, maybeOldRequest.isPresent() ? RequestHistoryType.UPDATED : RequestHistoryType.CREATED, user);

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
      if (!newRequest.getQuartzScheduleSafe().equals(oldRequest.getQuartzScheduleSafe())) {
        return true;
      }
    }

    return false;
  }

  private String getAndCheckDeployId(String requestId) {
    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    if (!maybeDeployId.isPresent()) {
      throw WebExceptions.conflict("Can not schedule a request (%s) with no deploy", requestId);
    }

    return maybeDeployId.get();
  }

  @POST
  @Path("/request/{requestId}/bounce")
  public SingularityRequestParent bounce(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    if (!requestWithState.getRequest().isLongRunning()) {
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
  public SingularityRequestParent pause(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user, Optional<SingularityPauseRequest> pauseRequest) {
    SingularityRequestWithState requestWithState = fetchRequestWithState(requestId);

    checkRequestStateNotPaused(requestWithState, "pause");

    Optional<Boolean> killTasks = Optional.absent();
    if (pauseRequest.isPresent()) {
      user = pauseRequest.get().getUser();
      killTasks = pauseRequest.get().getKillTasks();
    }

    mailer.sendRequestPausedMail(requestWithState.getRequest(), user);
    requestManager.pause(requestWithState.getRequest(), user);

    SingularityCreateResult result = requestManager.createCleanupRequest(new SingularityRequestCleanup(user, RequestCleanupType.PAUSING, System.currentTimeMillis(), killTasks, requestId));

    if (result != SingularityCreateResult.CREATED) {
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

    mailer.sendRequestUnpausedMail(requestWithState.getRequest(), user);

    Optional<String> maybeDeployId = deployManager.getInUseDeployId(requestId);

    if (maybeDeployId.isPresent() && !requestWithState.getRequest().isOneOff()) {
      requestManager.addToPendingQueue(new SingularityPendingRequest(requestId, maybeDeployId.get(), System.currentTimeMillis(), Optional.<String> absent(), user, PendingType.UNPAUSED));
    }

    requestManager.unpause(requestWithState.getRequest(), user);

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
  @Path("/finished")
  public Iterable<SingularityRequestParent> getFinishedRequests() {
    return getRequestsWithDeployState(requestManager.getFinishedRequests());
  }

  @GET
  @PropertyFiltering
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

  @DELETE
  @Path("/request/{requestId}")
  public SingularityRequest deleteRequest(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user) {
    SingularityRequest request = fetchRequest(requestId);

    mailer.sendRequestRemovedMail(request, user);
    requestManager.deleteRequest(request, user);

    return request;
  }

  @PUT
  @Path("/request/{requestId}/instances")
  public SingularityRequest updateInstances(@PathParam("requestId") String requestId, @QueryParam("user") Optional<String> user, SingularityRequestInstances newInstances) {
    if (requestId == null || newInstances.getId() == null || !requestId.equals(newInstances.getId())) {
      throw WebExceptions.badRequest("Update for request instance must pass a matching non-null requestId in path (%s) and object (%s)", requestId, newInstances.getId());
    }

    SingularityRequest oldRequest = fetchRequest(requestId);
    Optional<SingularityRequest> maybeOldRequest = Optional.of(oldRequest);

    SingularityRequestDeployHolder deployHolder = getDeployHolder(newInstances.getId());
    SingularityRequest newRequest = oldRequest.toBuilder().setInstances(newInstances.getInstances()).build();

    validator.checkSingularityRequest(newRequest, maybeOldRequest, deployHolder.getActiveDeploy(), deployHolder.getPendingDeploy());

    requestManager.update(newRequest, user);

    checkReschedule(newRequest, maybeOldRequest);

    return newRequest;
  }

}
