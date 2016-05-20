package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkBadRequest;
import static com.hubspot.singularity.WebExceptions.checkConflict;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityPriorityKillRequestParent;
import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityPriorityKillRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.PriorityManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(PriorityResource.PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api( description="Manages Singularity priority.", value=RackResource.PATH )  // TODO: better description
public class PriorityResource {
    public static final String PATH = SingularityService.API_BASE_PATH + "/priority";

    private static final Logger LOG = LoggerFactory.getLogger(PriorityResource.class);

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
    public Optional<SingularityPriorityKillRequestParent> getPriorityKill() {
        return priorityManager.getPriorityKill();
    }

    @DELETE
    @Path("/kill")
    public void deletePriorityKill() {
        authorizationHelper.checkAdminAuthorization(user);

        final SingularityDeleteResult deleteResult = priorityManager.deletePriorityKill();

        checkBadRequest(deleteResult == SingularityDeleteResult.DELETED, "No priority kill to delete");
    }

    @POST
    @Path("/kill")
    @ApiOperation("Kill all tasks below a certain priority level.")
    public SingularityPriorityKillRequestParent priorityKill(SingularityPriorityKillRequest priorityKillRequest) {
        authorizationHelper.checkAdminAuthorization(user);
        priorityKillRequest = singularityValidator.checkSingularityPriorityKillRequest(priorityKillRequest);

        final SingularityPriorityKillRequestParent priorityKillRequestParent = new SingularityPriorityKillRequestParent(priorityKillRequest, System.currentTimeMillis(), JavaUtils.getUserEmail(user));

        final SingularityCreateResult createResult = priorityManager.createPriorityKill(priorityKillRequestParent);
        checkConflict(createResult == SingularityCreateResult.CREATED, "Already a pending priority kill");

        return priorityKillRequestParent;
    }
}
