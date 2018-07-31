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
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.RACK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manage Singularity racks")
@Tags({@Tag(name = "Racks")})
public class RackResource extends AbstractMachineResource<SingularityRack> {

  @Inject
  public RackResource(RackManager rackManager, SingularityAuthorizationHelper authorizationHelper, SingularityValidator validator) {
    super(rackManager, authorizationHelper, validator);
  }

  @Override
  protected String getObjectTypeString() {
    return "Rack";
  }

  @GET
  @Path("/")
  @Operation(summary = "Retrieve the list of all known racks, optionally filtering by a particular state")
  public List<SingularityRack> getRacks(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Optionally specify a particular state to filter racks by") @QueryParam("state") Optional<MachineState> filterState) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GET
  @Path("/rack/{rackId}")
  @Operation(summary = "Retrieve the history of a given rack")
  public List<SingularityMachineStateHistoryUpdate> getRackHistory(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Rack ID") @PathParam("rackId") String rackId) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getHistory(rackId);
  }

  @DELETE
  @Path("/rack/{rackId}")
  @Operation(summary = "Remove a known rack, erasing history. This operation will cancel decommissioning of racks")
  public void removeRack(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Rack ID") @PathParam("rackId") String rackId) {
    super.remove(rackId, user);
  }

  @POST
  @Path("/rack/{rackId}/decommission")
  @Operation(summary = "Begin decommissioning a specific active rack")
  public void decommissionRack(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Rack ID") @PathParam("rackId") String rackId,
      @RequestBody(description = "Settings related to changing the state of a rack") SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.decommission(rackId, maybeChangeRequest, user, SingularityAction.DECOMMISSION_RACK);
  }

  @POST
  @Path("/rack/{rackId}/freeze")
  @Operation(summary = "Freeze a specific rack")
  public void freezeRack(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Rack ID") @PathParam("rackId") String rackId,
      @RequestBody(description = "Settings related to changing the state of a slave") SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.freeze(rackId, maybeChangeRequest, user, SingularityAction.FREEZE_RACK);
  }

  @POST
  @Path("/rack/{rackId}/activate")
  @Operation(summary = "Activate a decomissioning rack, canceling decomission without erasing history")
  public void activateRack(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Rack ID") @PathParam("rackId") String rackId,
      @RequestBody(description = "Settings related to changing the state of a slave") SingularityMachineChangeRequest changeRequest) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.fromNullable(changeRequest);
    super.activate(rackId, maybeChangeRequest, user, SingularityAction.ACTIVATE_RACK);
  }

  @DELETE
  @Path("/rack/{rackId}/expiring")
  @Operation(summary = "Delete any expiring machine state changes for this rack")
  public void deleteExpiringStateChange(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Rack ID") @PathParam("rackId") String rackId) {
    super.cancelExpiring(rackId, user);
  }

  @GET
  @Path("/expiring")
  @Operation(summary = "Get all expiring state changes for all racks")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(@Parameter(hidden = true) @Auth SingularityUser user) {
    return super.getExpiringStateChanges(user);
  }

}
