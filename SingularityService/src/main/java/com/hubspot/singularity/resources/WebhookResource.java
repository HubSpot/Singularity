package com.hubspot.singularity.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SingularityWebhookSummary;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.WebhookManager;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;

@Path(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Schema(title = "Manage Singularity webhooks")
@Tags({@Tag(name = "Webhooks")})
public class WebhookResource {
  private final WebhookManager webhookManager;
  private final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityValidator validator;

  @Inject
  public WebhookResource(WebhookManager webhookManager, SingularityAuthorizationHelper authorizationHelper, SingularityValidator validator) {
    this.webhookManager = webhookManager;
    this.authorizationHelper = authorizationHelper;
    this.validator = validator;
  }

  @GET
  @Operation(summary = "Retrieve a list of active webhooks.")
  public List<SingularityWebhook> getActiveWebhooks(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getActiveWebhooks();
  }

  @GET
  @Path("/summary")
  @Operation(summary = "Retrieve a summary of each active webhook")
  public List<SingularityWebhookSummary> getWebhooksWithQueueSize(@Parameter(hidden = true) @Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getWebhooksWithQueueSize();
  }

  @POST
  @Consumes({ MediaType.APPLICATION_JSON })
  @Operation(
      summary = "Add a new webhook",
      responses = {
          @ApiResponse(responseCode = "409", description = "Adding new webhooks is currently disabled")
      }
  )
  public SingularityCreateResult addWebhook(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @RequestBody(required = true, description = "SingularityWebhook object describing the new webhook to be added") SingularityWebhook webhook) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.ADD_WEBHOOK);
    validator.checkSingularityWebhook(webhook);
    return webhookManager.addWebhook(webhook);
  }

  @DELETE
  @Deprecated
  @Path("/{webhookId}")
  @Operation(
      summary = "Delete a specific webhook",
      responses = {
          @ApiResponse(responseCode = "409", description = "Deleting webhooks is currently disabled")
      }
  )
  public SingularityDeleteResult deleteWebhookDeprecated(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to delete") @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.REMOVE_WEBHOOK);
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Deprecated
  @Path("/deploy/{webhookId}")
  @Operation(summary = "Retrieve a list of queued deploy updates for a specific webhook")
  public List<SingularityDeployUpdate> getQueuedDeployUpdatesDeprecated(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to get deploy updates for") @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Deprecated
  @Path("/request/{webhookId}")
  @Operation(summary = "Retrieve a list of queued request updates for a specific webhook")
  public List<SingularityRequestHistory> getQueuedRequestUpdatesDeprecated(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to get request updates for") @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Deprecated
  @Path("/task/{webhookId}")
  @Operation(summary = "Retrieve a list of queued task updates for a specific webhook")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesDeprecated(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to get task updates for") @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @DELETE
  @Operation(summary = "Delete a specific webhook by id")
  public SingularityDeleteResult deleteWebhook(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to delete") @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.REMOVE_WEBHOOK);
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/deploy")
  @Operation(summary = "Retrieve a list of queued deploy updates for a specific webhook")
  public List<SingularityDeployUpdate> getQueuedDeployUpdates(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to get deploy updates for") @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/request")
  @Operation(summary = "Retrieve a list of queued request updates for a specific webhook.")
  public List<SingularityRequestHistory> getQueuedRequestUpdates(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(description = "Id of the webhook to get request updates for") @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/task")
  @Operation(summary = "Retrieve a list of queued task updates for a specific webhook.")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(
      @Parameter(hidden = true) @Auth SingularityUser user,
      @Parameter(required = true, description = "Id of the webhook to get task updates for") @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

}
