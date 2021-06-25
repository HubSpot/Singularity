package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.NotificationsManager;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

@Path(ApiPaths.NOTIFICATIONS_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manage email notifications")
@Tags({ @Tag(name = "Notifications") })
public class NotificationsResource extends AbstractLeaderAwareResource {
  private static final Pattern LEADING_TRAILING_QUOTES = Pattern.compile("^\"|\"$");
  private final NotificationsManager notificationsManager;

  @Inject
  public NotificationsResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    @Singularity ObjectMapper objectMapper,
    NotificationsManager notificationsManager
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.notificationsManager = notificationsManager;
  }

  @GET
  @Path("/blocklist")
  @Operation(summary = "Retrieve the list of blocklisted emails")
  public List<String> getBlacklist(@Parameter(hidden = true) @Auth SingularityUser user) {
    return notificationsManager.getBlocklist();
  }

  @GET
  @Path("/allowlist")
  @Operation(summary = "Retrieve the list of emails subscribed to Singularity")
  public List<String> getAllowlist(@Parameter(hidden = true) @Auth SingularityUser user) {
    return notificationsManager.getAllowlist();
  }

  @POST
  @Path("/unsubscribe")
  @Operation(summary = "Unsubscribe from Singularity emails.")
  public void unsubscribe(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @RequestBody(
      required = true,
      description = "The email address to unsubscribe"
    ) String email,
    @Context HttpServletRequest requestContext
  ) {
    notificationsManager.unsubscribe(getFormattedEmail(email));
  }

  @POST
  @Path("/subscribe")
  @Operation(summary = "Subscribe to Singularity emails")
  public void subscribe(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @RequestBody(
      required = true,
      description = "The email address to subscribe"
    ) String email,
    @Context HttpServletRequest requestContext
  ) {
    notificationsManager.subscribe(getFormattedEmail(email));
  }

  private String getFormattedEmail(String email) {
    return LEADING_TRAILING_QUOTES.matcher(email).replaceAll("");
  }
}
