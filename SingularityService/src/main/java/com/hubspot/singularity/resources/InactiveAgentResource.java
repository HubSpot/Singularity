package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.InactiveAgentManager;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Set;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path(ApiPaths.INACTIVE_AGENTS_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Schema(title = "Manage Singularity machines that should be marked as inactive")
@Tags({ @Tag(name = "Inactive Machines") })
public class InactiveAgentResource {
  private final InactiveAgentManager inactiveAgentManager;
  private final SingularityAuthorizer authorizationHelper;

  @Inject
  public InactiveAgentResource(
    InactiveAgentManager inactiveAgentManager,
    SingularityAuthorizer authorizationHelper
  ) {
    this.inactiveAgentManager = inactiveAgentManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Operation(summary = "Retrieve a list of agents marked as inactive")
  public Set<String> getInactiveSlaves() {
    return inactiveAgentManager.getInactiveAgents();
  }

  @POST
  @Operation(summary = "Mark an agent as inactive")
  public void deactivateSlave(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "The host to deactivate") @QueryParam(
      "host"
    ) String host
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveAgentManager.deactivateAgent(host);
  }

  @DELETE
  @Operation(summary = "Remove a host from teh deactivated list")
  public void reactivateSlave(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The host to remove from the deactivated list"
    ) @QueryParam("host") String host
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveAgentManager.activateAgent(host);
  }

  @DELETE
  @Path("/all")
  public void clearAllInactiveHosts(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveAgentManager.getInactiveAgents().forEach(inactiveAgentManager::activateAgent);
  }
}
