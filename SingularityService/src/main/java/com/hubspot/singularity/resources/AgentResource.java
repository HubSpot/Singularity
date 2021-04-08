package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.AgentManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import java.util.Optional;
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

@Path(ApiPaths.AGENT_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manages Singularity agents")
@Tags({ @Tag(name = "Agents") })
public class AgentResource extends AbstractMachineResource<SingularityAgent> {

  @Inject
  public AgentResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    @Singularity ObjectMapper objectMapper,
    AgentManager agentManager,
    SingularityAuthorizer authorizationHelper,
    SingularityValidator validator
  ) {
    super(
      httpClient,
      leaderLatch,
      objectMapper,
      agentManager,
      authorizationHelper,
      validator
    );
  }

  @Override
  protected String getObjectTypeString() {
    return "Agent";
  }

  @GET
  @Path("/")
  @Operation(
    summary = "Retrieve the list of all known agents, optionally filtering by a particular state"
  )
  public List<SingularityAgent> getAgents(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      description = "Optionally specify a particular state to filter agents by"
    ) @QueryParam("state") Optional<MachineState> filterState
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/agent/{agentId}")
  @Operation(summary = "Retrieve the history of a given agent")
  public List<SingularityMachineStateHistoryUpdate> getAgentHistory(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Agent ID") @PathParam(
      "agentId"
    ) String agentId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getHistory(agentId);
  }

  @GET
  @Path("/agent/{agentId}/details")
  @Operation(summary = "Get information about a particular agent")
  public Optional<SingularityAgent> getAgent(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Agent ID") @PathParam(
      "agentId"
    ) String agentId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObject(agentId);
  }

  @DELETE
  @Path("/agent/{agentId}")
  @Operation(
    summary = "Remove a known agent, erasing history. This operation will cancel decomissioning of the agent"
  )
  public Response removeAgent(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active AgentId") @PathParam(
      "agentId"
    ) String agentId
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        super.remove(agentId, user);
        return Response.ok().build();
      }
    );
  }

  @POST
  @Path("/agent/{agentId}/decommission")
  @Operation(summary = "Begin decommissioning a specific active agent")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response decommissionAgent(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId,
    @RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      changeRequest,
      () -> {
        decommissionAgent(user, agentId, changeRequest);
        return Response.ok().build();
      }
    );
  }

  public void decommissionAgent(
    SingularityUser user,
    String agentId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.decommission(
      agentId,
      maybeChangeRequest,
      user,
      SingularityAction.DECOMMISSION_AGENT
    );
  }

  @POST
  @Path("/agent/{agentId}/freeze")
  @Operation(summary = "Freeze tasks on a specific agent")
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response freezeAgent(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Agent ID") @PathParam(
      "agentId"
    ) String agentId,
    @RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      changeRequest,
      () -> {
        freezeAgent(user, agentId, changeRequest);
        return Response.ok().build();
      }
    );
  }

  public void freezeAgent(
    SingularityUser user,
    String agentId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.freeze(agentId, maybeChangeRequest, user, SingularityAction.FREEZE_AGENT);
  }

  @POST
  @Path("/agent/{agentId}/activate")
  @Operation(
    summary = "Activate a decomissioning agent, canceling decomission without erasing history"
  )
  @Consumes({ MediaType.APPLICATION_JSON })
  public Response activateAgent(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId,
    @RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      changeRequest,
      () -> {
        activateAgent(user, agentId, changeRequest);
        return Response.ok().build();
      }
    );
  }

  public void activateAgent(
    SingularityUser user,
    String agentId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.activate(agentId, maybeChangeRequest, user, SingularityAction.ACTIVATE_AGENT);
  }

  @DELETE
  @Path("/agent/{agentId}/expiring")
  @Operation(summary = "Delete any expiring machine state changes for this agent")
  public Response deleteExpiringStateChange(
    @Context HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        super.cancelExpiring(agentId, user);
        return Response.ok().build();
      }
    );
  }

  @GET
  @Path("/expiring")
  @Operation(summary = "Get all expiring state changes for all agents")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return super.getExpiringStateChanges(user);
  }

  @DELETE
  @Path("/dead")
  public Response clearAllDeadAgents(
    @Context HttpServletRequest requestContext,
    @Auth SingularityUser user
  ) {
    return maybeProxyToLeader(
      requestContext,
      Response.class,
      null,
      () -> {
        authorizationHelper.checkAdminAuthorization(user);
        manager
          .getObjectsFiltered(MachineState.DEAD)
          .forEach(s -> manager.deleteObject(s.getId()));
        return Response.ok().build();
      }
    );
  }
}
