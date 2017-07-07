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
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityPriorityFreeze;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(ApiPaths.PRIORITY_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages whether or not to schedule tasks based on their priority levels.", value=ApiPaths.PRIORITY_RESOURCE_PATH )
public class PriorityResource {
    private final Optional<SingularityUser> user;
    private final SingularityAuthorizationHelper authorizationHelper;
    private final SingularityValidator singularityValidator;
    private final PriorityManager priorityManager;

    @Inject
    public PriorityResource(Optional<SingularityUser> user, SingularityAuthorizationHelper authorizationHelper, SingularityValidator singularityValidator, PriorityManager priorityManager) {
        this.user = user;
        this.authorizationHelper = authorizationHelper;
        this.singularityValidator = singularityValidator;
        this.priorityManager = priorityManager;
    }

    @GET
    @Path("/freeze")
    @ApiOperation(value="Get information about the active priority freeze.", response=SingularityPriorityFreezeParent.class)
    @ApiResponses({
        @ApiResponse(code=200, message="The active priority freeze."),
        @ApiResponse(code=404, message="There was no active priority freeze.")
    })
    public Optional<SingularityPriorityFreezeParent> getActivePriorityFreeze() {
        return priorityManager.getActivePriorityFreeze();
    }

    @DELETE
    @Path("/freeze")
    @ApiOperation("Stops the active priority freeze.")
    @ApiResponses({
        @ApiResponse(code=202, message="The active priority freeze was deleted."),
        @ApiResponse(code=400, message="There was no active priority freeze to delete.")
    })
    public void deleteActivePriorityFreeze() {
        authorizationHelper.checkAdminAuthorization(user);

        final SingularityDeleteResult deleteResult = priorityManager.deleteActivePriorityFreeze();

        checkBadRequest(deleteResult == SingularityDeleteResult.DELETED, "No active priority freeze to delete.");

        priorityManager.clearPriorityKill();
    }

    @POST
    @Path("/freeze")
    @ApiOperation(value="Stop scheduling tasks below a certain priority level.", response=SingularityPriorityFreezeParent.class)
    @ApiResponses({
        @ApiResponse(code=200, message="The priority freeze request was accepted."),
        @ApiResponse(code=400, message="There was a validation error with the priority freeze request.")
    })
    public SingularityPriorityFreezeParent createPriorityFreeze(SingularityPriorityFreeze priorityFreezeRequest) {
        authorizationHelper.checkAdminAuthorization(user);
        priorityFreezeRequest = singularityValidator.checkSingularityPriorityFreeze(priorityFreezeRequest);

        final SingularityPriorityFreezeParent priorityFreezeRequestParent = new SingularityPriorityFreezeParent(priorityFreezeRequest, System.currentTimeMillis(), JavaUtils.getUserEmail(user));

        priorityManager.createPriorityFreeze(priorityFreezeRequestParent);

        if (priorityFreezeRequest.isKillTasks()) {
            priorityManager.setPriorityKill();
        }

        return priorityFreezeRequestParent;
    }
}
