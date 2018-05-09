package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.SingularityValidator;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.PRIORITY_RESOURCE_PATH)
@Produces({MediaType.APPLICATION_JSON})
@Schema(title = "Manages whether or not to schedule tasks based on their priority levels")
@Tags({@Tag(name = "Task Priorities")})
public class PriorityResource {
  private final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityValidator singularityValidator;
  private final PriorityManager priorityManager;

  @Inject
  public PriorityResource(SingularityAuthorizationHelper authorizationHelper, SingularityValidator singularityValidator, PriorityManager priorityManager) {
    this.authorizationHelper = authorizationHelper;
    this.singularityValidator = singularityValidator;
    this.priorityManager = priorityManager;
  }

  @GET
  @Path("/freeze")
  @Operation(
      summary = "Get information about the active priority freeze",
      responses = {
          @ApiResponse(responseCode = "200", description = "The active priority freeze"),
          @ApiResponse(responseCode = "404", description = "There was no active priority freeze")
      }
  )
  public Optional<SingularityPriorityFreezeParent> getActivePriorityFreeze(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return priorityManager.getActivePriorityFreeze();
  }

  @DELETE
  @Path("/freeze")
  @Operation(
      summary = "Stops the active priority freeze",
      responses = {
          @ApiResponse(responseCode = "202", description = "The active priority freeze was deleted"),
          @ApiResponse(responseCode = "400", description = "There was no active priority freeze to delete")
      }
  )
  public void deleteActivePriorityFreeze(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);

    final SingularityDeleteResult deleteResult = priorityManager.deleteActivePriorityFreeze();

    checkBadRequest(deleteResult == SingularityDeleteResult.DELETED, "No active priority freeze to delete.");

    priorityManager.clearPriorityKill();
  }

  @POST
  @Path("/freeze")
  @Operation(
      summary = "Stop scheduling tasks below a certain priority level",
      responses = {
          @ApiResponse(responseCode = "200", description = "The priority freeze request was accepted"),
          @ApiResponse(responseCode = "400", description = "There was a validation error with the priority freeze request")
      }
  )
  public SingularityPriorityFreezeParent createPriorityFreeze(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @RequestBody(description = "the new priority freeze to create") SingularityPriorityFreeze priorityFreezeRequest) {
    authorizationHelper.checkAdminAuthorization(user);
    priorityFreezeRequest = singularityValidator.checkSingularityPriorityFreeze(priorityFreezeRequest);

    final SingularityPriorityFreezeParent priorityFreezeRequestParent = new SingularityPriorityFreezeParent(priorityFreezeRequest, System.currentTimeMillis(), user.getEmail());

    priorityManager.createPriorityFreeze(priorityFreezeRequestParent);

    if (priorityFreezeRequest.isKillTasks()) {
      priorityManager.setPriorityKill();
    }

    return priorityFreezeRequestParent;
  }
}
