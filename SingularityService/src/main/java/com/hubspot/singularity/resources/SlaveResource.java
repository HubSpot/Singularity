package com.hubspot.singularity.resources;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.SlaveManager;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.ning.http.client.AsyncHttpClient;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.SLAVE_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manages Singularity slaves")
@Tags({@Tag(name = "Slaves")})
public class SlaveResource extends AbstractMachineResource<SingularitySlave> {
  @Inject
  public SlaveResource(AsyncHttpClient httpClient,
                       LeaderLatch leaderLatch,
                       ObjectMapper objectMapper,
                       SlaveManager slaveManager,
                       SingularityAuthorizationHelper authorizationHelper,
                       SingularityValidator validator) {
    super(httpClient, leaderLatch, objectMapper, slaveManager, authorizationHelper, validator);
  }

  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GET
  @Path("/")
  @Operation(summary = "Retrieve the list of all known slaves, optionally filtering by a particular state")
  public List<SingularitySlave> getSlaves(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Optionally specify a particular state to filter slaves by") @QueryParam("state") Optional<MachineState> filterState) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/slave/{slaveId}")
  @Operation(summary = "Retrieve the history of a given slave")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Slave ID") @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getHistory(slaveId);
  }

  @GET
  @Path("/slave/{slaveId}/details")
  @Operation(summary = "Get information about a particular slave")
  public Optional<SingularitySlave> getSlave(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Slave ID") @PathParam("slaveId") String slaveId) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObject(slaveId);
  }

  @DELETE
  @Path("/slave/{slaveId}")
  @Operation(summary = "Remove a known slave, erasing history. This operation will cancel decomissioning of the slave")
  public Response removeSlave(
      @Context HttpServletRequest requestContext,
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Active SlaveId") @PathParam("slaveId") String slaveId) {
    return maybeProxyToLeader(requestContext, Response.class, null, () -> {
      super.remove(slaveId, user);
      return Response.ok().build();
    });
  }

  @POST
  @Path("/slave/{slaveId}/decommission")
  @Operation(summary = "Begin decommissioning a specific active slave")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response decommissionSlave(
      @Context HttpServletRequest requestContext,
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Active slaveId") @PathParam("slaveId") String slaveId,
      @RequestBody(description = "Settings related to changing the state of a slave") SingularityMachineChangeRequest changeRequest) {
    return maybeProxyToLeader(requestContext, Response.class, changeRequest, () -> {
      decommissionSlave(user, slaveId, changeRequest);
      return Response.ok().build();
    });
  }

  public void decommissionSlave(SingularityUser user, String slaveId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.decommission(slaveId, maybeChangeRequest, user, SingularityAction.DECOMMISSION_SLAVE);
  }

  @POST
  @Path("/slave/{slaveId}/freeze")
  @Operation(summary = "Freeze tasks on a specific slave")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response freezeSlave(
      @Context HttpServletRequest requestContext,
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Slave ID") @PathParam("slaveId") String slaveId,
      @RequestBody(description = "Settings related to changing the state of a slave") SingularityMachineChangeRequest changeRequest) {
    return maybeProxyToLeader(requestContext, Response.class, changeRequest, () -> {
      freezeSlave(user, slaveId, changeRequest);
      return Response.ok().build();
    });
  }

  public void freezeSlave(SingularityUser user, String slaveId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.freeze(slaveId, maybeChangeRequest, user, SingularityAction.FREEZE_SLAVE);
  }

  @POST
  @Path("/slave/{slaveId}/activate")
  @Operation(summary = "Activate a decomissioning slave, canceling decomission without erasing history")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response activateSlave(
      @Context HttpServletRequest requestContext,
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Active slaveId") @PathParam("slaveId") String slaveId,
      @RequestBody(description = "Settings related to changing the state of a slave") SingularityMachineChangeRequest changeRequest) {
    return maybeProxyToLeader(requestContext, Response.class, changeRequest, () -> {
      activateSlave(user, slaveId, changeRequest);
      return Response.ok().build();
    });
  }

  public void activateSlave(SingularityUser user, String slaveId, SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.activate(slaveId, maybeChangeRequest, user, SingularityAction.ACTIVATE_SLAVE);
  }

  @DELETE
  @Path("/slave/{slaveId}/expiring")
  @Operation(summary = "Delete any expiring machine state changes for this slave")
  public Response deleteExpiringStateChange(
      @Context HttpServletRequest requestContext,
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Active slaveId") @PathParam("slaveId") String slaveId) {
    return maybeProxyToLeader(requestContext, Response.class, null, () -> {
      super.cancelExpiring(slaveId, user);
      return Response.ok().build();
    });
  }

  @GET
  @Path("/expiring")
  @Operation(summary = "Get all expiring state changes for all slaves")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(@Parameter(hidden = true) @Auth SingularityUser user) {
    return super.getExpiringStateChanges(user);
  }

  @DELETE
  @Path("/dead")
  public Response clearAllDeadSlaves(@Context HttpServletRequest requestContext, @Auth SingularityUser user) {
    return maybeProxyToLeader(requestContext, Response.class, null, () -> {
      authorizationHelper.checkAdminAuthorization(user);
      manager.getObjectsFiltered(MachineState.DEAD).forEach((s) -> manager.deleteObject(s.getId()));
      return Response.ok().build();
    });
  }
}
