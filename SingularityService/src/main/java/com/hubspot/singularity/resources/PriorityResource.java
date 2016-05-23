package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;

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
import com.hubspot.singularity.SingularityPriorityRequestParent;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityPriorityRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(PriorityResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api( description="Manages whether or not to schedule tasks based on their priority levels.", value=RackResource.PATH )
public class PriorityResource {
    public static final String PATH = SingularityService.API_BASE_PATH + "/priority";

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
    @Path("/kill")
    @ApiOperation(value="Retrieve the active priority kill.", response=SingularityPriorityRequestParent.class)
    @ApiResponses({
        @ApiResponse(code=200, message="The active priority kill."),
        @ApiResponse(code=404, message="There was no active priority kill.")
    })
    public Optional<SingularityPriorityRequestParent> getPriorityKill() {
        return priorityManager.getActivePriorityKill();
    }

    @DELETE
    @Path("/kill")
    @ApiOperation("Stops the active priority kill.")
    @ApiResponses({
        @ApiResponse(code=202, message="The active priority kill was deleted."),
        @ApiResponse(code=400, message="There was no active priority kill to delete.")
    })
    public void deleteActivePriorityKill() {
        authorizationHelper.checkAdminAuthorization(user);

        final SingularityDeleteResult deleteResult = priorityManager.deleteActivePriorityKill();

        checkBadRequest(deleteResult == SingularityDeleteResult.DELETED, "Active priority kill does not exist.");
    }

    @POST
    @Path("/kill")
    @ApiOperation(value="Kill all tasks below a certain priority level.", response=SingularityPriorityRequestParent.class)
    @ApiResponses({
        @ApiResponse(code=200, message="The priority kill request was accepted."),
        @ApiResponse(code=400, message="There was a validation error with the priority kill request.")
    })
    public SingularityPriorityRequestParent createPriorityKill(SingularityPriorityRequest priorityKillRequest) {
        authorizationHelper.checkAdminAuthorization(user);
        priorityKillRequest = singularityValidator.checkSingularityPriorityRequest(priorityKillRequest);

        checkConflict(!priorityManager.getActivePriorityKill().isPresent(), "There is already an active priority kill underway. Please try again soon.");

        final SingularityPriorityRequestParent priorityKillRequestParent = new SingularityPriorityRequestParent(priorityKillRequest, System.currentTimeMillis(), JavaUtils.getUserEmail(user));

        priorityManager.createPriorityKill(priorityKillRequestParent);

        return priorityKillRequestParent;
    }

    @GET
    @Path("/freeze")
    @ApiOperation(value="Get information about the active priority freeze.", response=SingularityPriorityRequestParent.class)
    @ApiResponses({
        @ApiResponse(code=200, message="The active priority freeze."),
        @ApiResponse(code=404, message="There was no active priority freeze.")
    })
    public Optional<SingularityPriorityRequestParent> getActivePriorityFreeze() {
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
    }

    @POST
    @Path("/freeze")
    @ApiOperation(value="Stop scheduling tasks below a certain priority level.", response=SingularityPriorityRequestParent.class)
    @ApiResponses({
        @ApiResponse(code=200, message="The priority freeze request was accepted."),
        @ApiResponse(code=400, message="There was a validation error with the priorty freeze request.")
    })
    public SingularityPriorityRequestParent createPriorityFreeze(SingularityPriorityRequest priorityFreezeRequest) {
        authorizationHelper.checkAdminAuthorization(user);
        priorityFreezeRequest = singularityValidator.checkSingularityPriorityRequest(priorityFreezeRequest);

        final SingularityPriorityRequestParent priorityFreezeRequestParent = new SingularityPriorityRequestParent(priorityFreezeRequest, System.currentTimeMillis(), JavaUtils.getUserEmail(user));

        priorityManager.createPriorityFreeze(priorityFreezeRequestParent);

        return priorityFreezeRequestParent;
    }
}
