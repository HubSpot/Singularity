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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import io.dropwizard.auth.Auth;

@Path(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Produces({ MediaType.APPLICATION_JSON })
@Api(description="Manages Singularity webhooks.", value=ApiPaths.WEBHOOK_RESOURCE_PATH)
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
  @ApiOperation("Retrieve a list of active webhooks.")
  public List<SingularityWebhook> getActiveWebhooks(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getActiveWebhooks();
  }

  @GET
  @Path("/summary")
  @ApiOperation("Retrieve a summary of each active webhook")
  public List<SingularityWebhookSummary> getWebhooksWithQueueSize(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getWebhooksWithQueueSize();
  }

  @POST
  @ApiOperation("Add a new webhook.")
  public SingularityCreateResult addWebhook(@Auth SingularityUser user, SingularityWebhook webhook) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.ADD_WEBHOOK);
    validator.checkSingularityWebhook(webhook);
    return webhookManager.addWebhook(webhook);
  }

  @DELETE
  @Deprecated
  @Path("/{webhookId}")
  @ApiOperation("Delete a specific webhook.")
  public SingularityDeleteResult deleteWebhookDeprecated(@Auth SingularityUser user, @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.REMOVE_WEBHOOK);
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Deprecated
  @Path("/deploy/{webhookId}")
  @ApiOperation("Retrieve a list of queued deploy updates for a specific webhook.")
  public List<SingularityDeployUpdate> getQueuedDeployUpdatesDeprecated(@Auth SingularityUser user, @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Deprecated
  @Path("/request/{webhookId}")
  @ApiOperation("Retrieve a list of queued request updates for a specific webhook.")
  public List<SingularityRequestHistory> getQueuedRequestUpdatesDeprecated(@Auth SingularityUser user, @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Deprecated
  @Path("/task/{webhookId}")
  @ApiOperation("Retrieve a list of queued task updates for a specific webhook.")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesDeprecated(@Auth SingularityUser user, @PathParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @DELETE
  @ApiOperation("Delete a specific webhook.")
  public SingularityDeleteResult deleteWebhook(@Auth SingularityUser user, @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.REMOVE_WEBHOOK);
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/deploy")
  @ApiOperation("Retrieve a list of queued deploy updates for a specific webhook.")
  public List<SingularityDeployUpdate> getQueuedDeployUpdates(@Auth SingularityUser user, @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/request")
  @ApiOperation("Retrieve a list of queued request updates for a specific webhook.")
  public List<SingularityRequestHistory> getQueuedRequestUpdates(@Auth SingularityUser user, @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GET
  @Path("/task")
  @ApiOperation("Retrieve a list of queued task updates for a specific webhook.")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(@Auth SingularityUser user, @QueryParam("webhookId") String webhookId) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

}
