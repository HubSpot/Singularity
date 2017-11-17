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
import com.wordnik.swagger.annotations.Api;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.INACTIVE_SLAVES_RESOURCE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Api(description="Manages Singularity Deploys for existing requests", value=ApiPaths.INACTIVE_SLAVES_RESOURCE_PATH)
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
  public Set<String> getInactiveSlaves() {
    return inactiveSlaveManager.getInactiveSlaves();
  }

  @POST
  public void deactivateSlave(@Auth SingularityUser user,
                              @QueryParam("host") String host) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveSlaveManager.deactivateSlave(host);
  }

  @DELETE
  public void reactivateSlave(@Auth SingularityUser user,
                              @QueryParam("host") String host) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveSlaveManager.activateSlave(host);
  }
}
