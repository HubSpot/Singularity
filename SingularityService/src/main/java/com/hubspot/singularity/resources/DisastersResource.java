package com.hubspot.singularity.resources;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityTaskCredits;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.api.SingularityDisabledActionRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.DisasterManager;
import com.hubspot.singularity.mesos.SingularityMesosModule;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(DisastersResource.PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description="Manages Singularity Deploys for existing requests", value=DisastersResource.PATH)
public class DisastersResource {
  public static final String PATH = SingularityService.API_BASE_PATH + "/disasters";

  private final DisasterManager disasterManager;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final Optional<SingularityUser> user;
  private final LeaderLatch leaderLatch;
  private final AtomicInteger taskCredits;
  private final HttpClient httpClient;

  @Inject
  public DisastersResource(DisasterManager disasterManager,
                           SingularityAuthorizationHelper authorizationHelper,
                           Optional<SingularityUser> user,
                           LeaderLatch leaderLatch,
                           @Named(SingularityMesosModule.TASK_CREDITS) AtomicInteger taskCredits,
                           @Named(SingularityResourceModule.PROXY_TO_LEADER_HTTP_CLIENT) HttpClient httpClient) {
    this.disasterManager = disasterManager;
    this.authorizationHelper = authorizationHelper;
    this.user = user;
    this.leaderLatch = leaderLatch;
    this.taskCredits = taskCredits;
    this.httpClient = httpClient;
  }

  @GET
  @Path("/stats")
  @ApiOperation(value="Get current data related to disaster detection", response=SingularityDisastersData.class)
  public SingularityDisastersData disasterStats() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisastersData();
  }

  @GET
  @Path("/active")
  @ApiOperation(value="Get a list of current active disasters")
  public List<SingularityDisasterType> activeDisasters() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getActiveDisasters();
  }

  @POST
  @Path("/disable")
  @ApiOperation(value="Do not allow the automated poller to disable actions when a disaster is detected")
  public void disableAutomatedDisasterCreation() {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.disableAutomatedDisabledActions();
  }

  @POST
  @Path("/enable")
  @ApiOperation(value="Allow the automated poller to disable actions when a disaster is detected")
  public void enableAutomatedDisasterCreation() {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enableAutomatedDisabledActions();
  }

  @DELETE
  @Path("/active/{type}")
  @ApiOperation(value="Remove an active disaster (make it inactive)")
  public void removeDisaster(@PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.removeDisaster(type);
  }

  @POST
  @Path("/active/{type}")
  @ApiOperation(value="Create a new active disaster")
  public void newDisaster(@PathParam("type") SingularityDisasterType type) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.addDisaster(type);
    disasterManager.addDisabledActionsForDisasters(Collections.singletonList(type));
  }

  @GET
  @Path("/disabled-actions")
  @ApiOperation(value="Get a list of actions that are currently disable")
  public List<SingularityDisabledAction> disabledActions() {
    authorizationHelper.checkAdminAuthorization(user);
    return disasterManager.getDisabledActions();
  }

  @POST
  @Path("/disabled-actions/{action}")
  @ApiOperation(value="Disable a specific action")
  public void disableAction(@PathParam("action") SingularityAction action, Optional<SingularityDisabledActionRequest> maybeRequest) {
    authorizationHelper.checkAdminAuthorization(user);
    Optional<String> message = maybeRequest.isPresent() ? maybeRequest.get().getMessage() : Optional.<String>absent();
    disasterManager.disable(action, message, user, false, Optional.<Long>absent());
  }

  @DELETE
  @Path("/disabled-actions/{action}")
  @ApiOperation(value="Re-enable a specific action if it has been disabled")
  public void enableAction(@PathParam("action") SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    disasterManager.enable(action);
  }

  @POST
  @Path("/task-credits")
  @ApiOperation(value="Add task credits, enables task credit system if not already enabled")
  public void addTaskCredits(@QueryParam("credits") Optional<Integer> credits, @Context HttpServletRequest request) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    WebExceptions.checkBadRequest(credits.isPresent(), "Must specify credits to add");
    if (leaderLatch.hasLeadership()) {
      int previous = taskCredits.getAndAdd(credits.get());
      if (previous == -1) {
        // If previously disabled, we are starting from -1 not 0. Add one more
        taskCredits.getAndIncrement();
      }
    } else {
      String leaderUri = leaderLatch.getLeader().getId();
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .setUrl(String.format("%s/task-credits", leaderUri))
          .setMethod(Method.POST);
      copyHeadersAndParams(requestBuilder, request);
      HttpResponse response = httpClient.execute(requestBuilder.build());
      if (response.isError()) {
        throw new RuntimeException(response.getAsString());
      }
    }
  }

  @DELETE
  @Path("/task-credits")
  @ApiOperation(value="Disable task credit system")
  public void disableTaskCredits(@Context HttpServletRequest request) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    if (leaderLatch.hasLeadership()) {
      taskCredits.getAndSet(-1);
    } else {
      String leaderUri = leaderLatch.getLeader().getId();
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .setUrl(String.format("%s/task-credits", leaderUri))
          .setMethod(Method.DELETE);
      copyHeadersAndParams(requestBuilder, request);
      HttpResponse response = httpClient.execute(requestBuilder.build());
      if (response.isError()) {
        throw new RuntimeException(response.getAsString());
      }
    }
  }

  @GET
  @Path("/task-credits")
  @ApiOperation(value="Get task credit data")
  public SingularityTaskCredits getTaskCreditData(@Context HttpServletRequest request) throws Exception {
    authorizationHelper.checkAdminAuthorization(user);
    if (leaderLatch.hasLeadership()) {
      return new SingularityTaskCredits(taskCredits.get() != -1, taskCredits.get());
    } else {
      String leaderUri = leaderLatch.getLeader().getId();
      HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
          .setUrl(String.format("%s/task-credits", leaderUri))
          .setMethod(Method.GET);
      copyHeadersAndParams(requestBuilder, request);
      HttpResponse response = httpClient.execute(requestBuilder.build());
      if (response.isError()) {
        throw new RuntimeException(response.getAsString());
      } else {
        return response.getAs(SingularityTaskCredits.class);
      }
    }
  }

  private void copyHeadersAndParams(HttpRequest.Builder requestBuilder, HttpServletRequest request) {
    while (request.getHeaderNames().hasMoreElements()) {
      String headerName = request.getHeaderNames().nextElement();
      requestBuilder.addHeader(headerName, request.getHeader(headerName));
    }
    while (request.getParameterNames().hasMoreElements()) {
      String parameterName = request.getParameterNames().nextElement();
      requestBuilder.setQueryParam(parameterName).to(request.getParameter(parameterName));
    }
  }
}
