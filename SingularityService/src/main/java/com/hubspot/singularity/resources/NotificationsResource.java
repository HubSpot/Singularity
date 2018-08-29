package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.NotificationsManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.NOTIFICATIONS_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manage email notifications")
@Tags({@Tag(name = "Notifications")})
public class NotificationsResource {
  private final NotificationsManager notificationsManager;

  @Inject
  public NotificationsResource(NotificationsManager notificationsManager) {
    this.notificationsManager = notificationsManager;
  }

  @GET
  @Path("/blacklist")
  @Operation(summary = "Retrieve the list of blacklisted emails")
  public List<String> getBlacklist(@Parameter(hidden = true) @Auth SingularityUser user) {
    return notificationsManager.getBlacklist();
  }

  @POST
  @Path("/unsubscribe")
  @Operation(summary = "Unsubscribe from Singularity emails.")
  public void unsubscribe(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @RequestBody(required = true, description = "The email address to unsubscribe") String email) {
    notificationsManager.addToBlacklist(email);
  }

  @POST
  @Path("/subscribe")
  @Operation(summary = "Delete an unsubscription for an email address")
  public void subscribe(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @RequestBody(required = true, description = "The email address to re-subscribe") String email) {
    notificationsManager.removeFromBlacklist(email);
  }
}
