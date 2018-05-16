package com.hubspot.singularity.resources;

import java.util.Set;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.InactiveSlaveManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.INACTIVE_SLAVES_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Schema(title = "Manage Singularity machines that should be marked as inactive")
@Tags({@Tag(name = "Inactive Machines")})
public class InactiveSlaveResource {
  private final InactiveSlaveManager inactiveSlaveManager;
  private final SingularityAuthorizationHelper authorizationHelper;


  @Inject
  public InactiveSlaveResource(InactiveSlaveManager inactiveSlaveManager,
                               SingularityAuthorizationHelper authorizationHelper) {
    this.inactiveSlaveManager = inactiveSlaveManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GET
  @Operation(summary = "Retrieve a list of slaves marked as inactive")
  public Set<String> getInactiveSlaves() {
    return inactiveSlaveManager.getInactiveSlaves();
  }

  @POST
  @Operation(summary = "Mark a slave as inactive")
  public void deactivateSlave(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The host to deactivate") @QueryParam("host") String host) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveSlaveManager.deactivateSlave(host);
  }

  @DELETE
  @Operation(summary = "Remove a host from teh deactivated list")
  public void reactivateSlave(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "The host to remove from the deactivated list") @QueryParam("host") String host) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveSlaveManager.activateSlave(host);
  }

  @DELETE
  @Path("/all")
  public void clearAllInactiveHosts(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveSlaveManager.getInactiveSlaves().forEach(inactiveSlaveManager::activateSlave);
  }
}
