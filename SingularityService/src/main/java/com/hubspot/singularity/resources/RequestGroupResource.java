package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RequestGroupManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path(ApiPaths.REQUEST_GROUP_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity Request Groups, which are collections of one or more Singularity Requests", value=ApiPaths.REQUEST_GROUP_RESOURCE_PATH, position=1)
public class RequestGroupResource {
    private final RequestGroupManager requestGroupManager;
    private final SingularityValidator validator;

    @Inject
    public RequestGroupResource(RequestGroupManager requestGroupManager, SingularityValidator validator) {
        this.requestGroupManager = requestGroupManager;
        this.validator = validator;
    }

    @GET
    @ApiOperation(value="Get a list of Singularity request groups")
    public List<SingularityRequestGroup> getRequestGroupIds(@QueryParam("useWebCache") Boolean useWebCache) {
        return requestGroupManager.getRequestGroups(useWebCache != null && useWebCache);
    }

    @GET
    @Path("/group/{requestGroupId}")
    @ApiOperation(value="Get a specific Singularity request group by ID")
    public Optional<SingularityRequestGroup> getRequestGroup(@PathParam("requestGroupId") String requestGroupId) {
        return requestGroupManager.getRequestGroup(requestGroupId);
    }

    @DELETE
    @Path("/group/{requestGroupId}")
    @ApiOperation(value="Delete a specific Singularity request group by ID")
    public void deleteRequestGroup(@PathParam("requestGroupId") String requestGroupId) {
        requestGroupManager.deleteRequestGroup(requestGroupId);
    }

    @POST
    @ApiOperation(value="Create a Singularity request group")
    public SingularityRequestGroup saveRequestGroup(SingularityRequestGroup requestGroup) {
        validator.checkRequestGroup(requestGroup);

        requestGroupManager.saveRequestGroup(requestGroup);

        return requestGroup;
    }
}
