package com.hubspot.singularity.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanupResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.api.SingularityDeployRequest;

public class SingularityClient {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityClient.class);

  private static final String STATE_FORMAT = "http://%s/%s/state";

  private static final String RACKS_FORMAT = "http://%s/%s/racks";
  private static final String RACKS_GET_ACTIVE_FORMAT = RACKS_FORMAT + "/active";
  private static final String RACKS_GET_DEAD_FORMAT = RACKS_FORMAT + "/dead";
  private static final String RACKS_GET_DECOMISSIONING_FORMAT = RACKS_FORMAT + "/decomissioning";
  private static final String RACKS_DECOMISSION_FORMAT = RACKS_FORMAT + "/rack/%s/decomission";
  private static final String RACKS_DELETE_DEAD_FORMAT = RACKS_FORMAT + "/rack/%s/dead";
  private static final String RACKS_DELETE_DECOMISSIONING_FORMAT = RACKS_FORMAT + "/rack/%s/decomissioning";

  private static final String SLAVES_FORMAT = "http://%s/%s/slaves";
  private static final String SLAVES_GET_ACTIVE_FORMAT = SLAVES_FORMAT + "/active";
  private static final String SLAVES_GET_DEAD_FORMAT = SLAVES_FORMAT + "/dead";
  private static final String SLAVES_GET_DECOMISSIONING_FORMAT = SLAVES_FORMAT + "/decomissioning";
  private static final String SLAVES_DECOMISSION_FORMAT = SLAVES_FORMAT + "/slave/%s/decomission";
  private static final String SLAVES_DELETE_DECOMISSIONING_FORMAT = SLAVES_FORMAT + "/slave/%s/decomissioning";
  private static final String SLAVES_DELETE_DEAD_FORMAT = SLAVES_FORMAT + "/slave/%s/dead";

  private static final String TASKS_FORMAT = "http://%s/%s/tasks";
  private static final String TASKS_KILL_TASK_FORMAT = TASKS_FORMAT + "/task/%s";
  private static final String TASKS_GET_ACTIVE_FORMAT = TASKS_FORMAT + "/active";
  private static final String TASKS_GET_ACTIVE_PER_HOST_FORMAT = TASKS_FORMAT + "/active/%s";
  private static final String TASKS_GET_SCHEDULED_FORMAT = TASKS_FORMAT + "/scheduled";

  private static final String HISTORY_FORMAT = "http://%s/%s/history";
  private static final String TASK_HISTORY_FORMAT = HISTORY_FORMAT + "/task/%s";
  private static final String REQUEST_ACTIVE_TASKS_HISTORY_FORMAT = HISTORY_FORMAT + "/request/%s/tasks/active";
  private static final String REQUEST_INACTIVE_TASKS_HISTORY_FORMAT = HISTORY_FORMAT + "/request/%s/tasks";
  private static final String REQUEST_DEPLOY_HISTORY_FORMAT = HISTORY_FORMAT + "/request/%s/deploy/%s";

  private static final String REQUESTS_FORMAT = "http://%s/%s/requests";
  private static final String REQUESTS_GET_ACTIVE_FORMAT = REQUESTS_FORMAT + "/active";
  private static final String REQUESTS_GET_PAUSED_FORMAT = REQUESTS_FORMAT + "/paused";
  private static final String REQUESTS_GET_COOLDOWN_FORMAT = REQUESTS_FORMAT + "/cooldown";
  private static final String REQUESTS_GET_PENDING_FORMAT = REQUESTS_FORMAT + "/queued/pending";
  private static final String REQUESTS_GET_CLEANUP_FORMAT = REQUESTS_FORMAT + "/queued/cleanup";

  private static final String REQUEST_GET_FORMAT = REQUESTS_FORMAT + "/request/%s";
  private static final String REQUEST_CREATE_OR_UPDATE_FORMAT = REQUESTS_FORMAT;
  private static final String REQUEST_DELETE_ACTIVE_FORMAT = REQUESTS_FORMAT + "/request/%s";
  private static final String REQUEST_DELETE_PAUSED_FORMAT = REQUESTS_FORMAT + "/request/%s/paused";
  private static final String REQUEST_BOUNCE_FORMAT = REQUESTS_FORMAT + "/request/%s/bounce";
  private static final String REQUEST_PAUSE_FORMAT = REQUESTS_FORMAT + "/request/%s/pause";

  private static final String DEPLOYS_FORMAT = "http://%s/%s/deploys";
  private static final String DELETE_DEPLOY_FORMAT = DEPLOYS_FORMAT + "/deploy/%s/request/%s";

  private static final String WEBHOOKS_FORMAT = "http://%s/%s/webhooks";
  private static final String WEBHOOKS_DELETE_FORMAT = WEBHOOKS_FORMAT +"/%s";
  private static final String WEBHOOKS_GET_QUEUED_DEPLOY_UPDATES_FORMAT = WEBHOOKS_FORMAT + "/deploy/%s";
  private static final String WEBHOOKS_GET_QUEUED_REQUEST_UPDATES_FORMAT = WEBHOOKS_FORMAT + "/request/%s";
  private static final String WEBHOOKS_GET_QUEUED_TASK_UPDATES_FORMAT = WEBHOOKS_FORMAT + "/task/%s";

  private static final TypeReference<Collection<SingularityRequest>> REQUESTS_COLLECTION = new TypeReference<Collection<SingularityRequest>>() {};
  private static final TypeReference<Collection<SingularityPendingRequest>> PENDING_REQUESTS_COLLECTION = new TypeReference<Collection<SingularityPendingRequest>>() {};
  private static final TypeReference<Collection<SingularityRequestCleanup>> CLEANUP_REQUESTS_COLLECTION = new TypeReference<Collection<SingularityRequestCleanup>>() {};
  private static final TypeReference<Collection<SingularityTask>> TASKS_COLLECTION = new TypeReference<Collection<SingularityTask>>() {};
  private static final TypeReference<Collection<SingularityTaskIdHistory>> TASKID_HISTORY_COLLECTION = new TypeReference<Collection<SingularityTaskIdHistory>>() {};
  private static final TypeReference<Collection<SingularityRack>> RACKS_COLLECTION = new TypeReference<Collection<SingularityRack>>() {};
  private static final TypeReference<Collection<SingularitySlave>> SLAVES_COLLECTION = new TypeReference<Collection<SingularitySlave>>() {};
  private static final TypeReference<Collection<SingularityWebhook>> WEBHOOKS_COLLECTION = new TypeReference<Collection<SingularityWebhook>>() {};
  private static final TypeReference<Collection<SingularityDeployWebhook>> DEPLOY_UPDATES_COLLECTION = new TypeReference<Collection<SingularityDeployWebhook>>() {};
  private static final TypeReference<Collection<SingularityRequestHistory>> REQUEST_UPDATES_COLLECTION = new TypeReference<Collection<SingularityRequestHistory>>() {};
  private static final TypeReference<Collection<SingularityTaskHistoryUpdate>> TASK_UPDATES_COLLECTION = new TypeReference<Collection<SingularityTaskHistoryUpdate>>() {};

  private final Random random;
  private final String[] hosts;
  private final String contextPath;

  private final HttpClient httpClient;

  @Inject
  public SingularityClient(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath, @Named(SingularityClientModule.HTTP_CLIENT_NAME) HttpClient httpClient, @Named(SingularityClientModule.HOSTS_PROPERTY_NAME) String hosts) {
    this.httpClient = httpClient;
    this.contextPath = contextPath;

    this.hosts = hosts.split(",");
    this.random = new Random();
  }

  private String getHost() {
    return hosts[random.nextInt(hosts.length)];
  }

  private void checkResponse(String type, HttpResponse response) {
    if (response.isError()) {
      throw fail(type, response);
    }
  }

  private SingularityClientException fail(String type, HttpResponse response) {
    String body = "";

    try {
      body = response.getAsString();
    } catch (Exception e) {
      LOG.warn("Unable to read body", e);
    }

    String uri = "";

    try {
      uri = response.getRequest().getUrl().toString();
    } catch (Exception e) {
      LOG.warn("Unable to read uri", e);
    }

    throw new SingularityClientException(String.format("Failed '%s' action on Singularity (%s) - code: %s, %s", type, uri, response.getStatusCode(), body), response.getStatusCode());
  }

  private <T> Optional<T> getSingle(String uri, String type, String id, Class<T> clazz) {
    checkNotNull(id, String.format("Provide a %s id", type));

    LOG.info("Getting {} {} from {}", type, id, uri);

    final long start = System.currentTimeMillis();

    HttpResponse response = httpClient.execute(HttpRequest.newBuilder().setUrl(uri).build());

    if (response.getStatusCode() == 404) {
      return Optional.absent();
    }

    checkResponse(type, response);

    LOG.info("Got {} {} in {}ms", type, id, System.currentTimeMillis() - start);

    return Optional.fromNullable(response.getAs(clazz));
  }

  private <T> Collection<T> getCollection(String uri, String type, TypeReference<Collection<T>> typeReference) {
    LOG.info("Getting all {} from {}", type, uri);

    final long start = System.currentTimeMillis();

    HttpResponse response = httpClient.execute(HttpRequest.newBuilder().setUrl(uri).build());

    if (response.getStatusCode() == 404) {
      return ImmutableList.of();
    }

    checkResponse(type, response);

    LOG.info("Got {} in {}ms", type, System.currentTimeMillis() - start);

    return response.getAs(typeReference);
  }

  private <T> void delete(String uri, String type, String id, Optional<String> user) {
    delete(uri, type, id, user, Optional.<Class<T>> absent());
  }

  private <T> Optional<T> delete(String uri, String type, String id, Optional<String> user, Optional<Class<T>> clazz) {
    LOG.info("Deleting {} {} from {}", type, id, uri);

    final long start = System.currentTimeMillis();

    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.DELETE);

    if (user.isPresent()) {
      request.addQueryParam("user", user.get());
    }

    HttpResponse response = httpClient.execute(request.build());

    if (response.isSuccess()) {
      LOG.info("Deleted {} ({}) from Singularity in %sms", type, id, System.currentTimeMillis() - start);

      if (clazz.isPresent()) {
        return Optional.of(response.getAs(clazz.get()));
      }
    } else {
      try {
        LOG.warn("Failed to delete {} {} - ({})", type, id, response.getAsString());
      } catch (Exception e) {
        LOG.warn("Failed to delete {} {}, and couldn't read response", type, id, e);
      }
    }

    return Optional.absent();
  }

  private <T> Optional<T> post(String uri, String type, Optional<?> body, Optional<String> user, Optional<Class<T>> clazz) {
    try {
      HttpResponse response = post(uri, type, body, user);

      if (clazz.isPresent()) {
        return Optional.of(response.getAs(clazz.get()));
      }
    } catch (Exception e) {
      LOG.warn("Http post failed", e);
    }

    return Optional.<T>absent();
  }

  private HttpResponse post(String uri, String type, Optional<?> body, Optional<String> user) {
    LOG.info("Posting {} to {}", type, uri);

    final long start = System.currentTimeMillis();

    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.POST);

    if (user.isPresent()) {
      request.addQueryParam("user", user.get());
    }

    if (body.isPresent()) {
      request.setBody(body.get());
    }

    HttpResponse response = httpClient.execute(request.build());

    checkResponse(type, response);

    LOG.info("Successfully posted {} in {}ms", type, System.currentTimeMillis() - start);

    return response;
  }

  //
  // GLOBAL
  //

  public SingularityState getState(Optional<Boolean> skipCache, Optional<Boolean> includeRequestIds) {
    final String uri = String.format(STATE_FORMAT, getHost(), contextPath);

    LOG.info("Fetching state from {}", uri);

    final long start = System.currentTimeMillis();

    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri);

    if (skipCache.isPresent()) {
      request.addQueryParam("skipCache", skipCache.get().booleanValue());
    }
    if (includeRequestIds.isPresent()) {
      request.addQueryParam("includeRequestIds", includeRequestIds.get().booleanValue());
    }

    HttpResponse response = httpClient.execute(request.build());

    checkResponse("state", response);

    LOG.info("Got state in {}ms", System.currentTimeMillis() - start);

    return response.getAs(SingularityState.class);
  }

  //
  // ACTIONS ON A SINGLE SINGULARITY REQUEST
  //

  public Optional<SingularityRequestParent> getSingularityRequest(String requestId) {
    final String singularityApiRequestUri = String.format(REQUEST_GET_FORMAT, getHost(), contextPath, requestId);

    return getSingle(singularityApiRequestUri, "request", requestId, SingularityRequestParent.class);
  }

  public void createOrUpdateSingularityRequest(SingularityRequest request, Optional<String> user) {
    checkNotNull(request.getId(), "A posted Singularity Request must have an id");

    final String requestUri = String.format(REQUEST_CREATE_OR_UPDATE_FORMAT, getHost(), contextPath);

    post(requestUri, String.format("request %s", request.getId()), Optional.of(request), user);
  }

  /**
   * Delete a singularity request that is active.
   * If the deletion is successful the deleted singularity request is returned.
   * If the request to be deleted is not found {code Optional.absent()} is returned
   * If the singularity request to be deleted is paused the deletion will fail ({code Optional.absent()} will be returned)
   * If you want to delete a paused singularity request use the provided {@link SingularityClient#deletePausedSingularityRequest}
   *
   * @param requestId
   *      the id of the singularity request to delete
   * @param user
   *      the ...
   * @return
   *      the singularity request that was deleted
   */
  public Optional<SingularityRequest> deleteActiveSingularityRequest(String requestId, Optional<String> user) {
    final String requestUri = String.format(REQUEST_DELETE_ACTIVE_FORMAT, getHost(), contextPath, requestId);

    return delete(requestUri, "active request", requestId, user, Optional.of(SingularityRequest.class));
  }

  public Optional<SingularityRequest> deletePausedSingularityRequest(String requestId, Optional<String> user) {
    final String requestUri = String.format(REQUEST_DELETE_PAUSED_FORMAT, getHost(), contextPath, requestId);

    return delete(requestUri, "paused request", requestId, user, Optional.of(SingularityRequest.class));
  }

  public void pauseSingularityRequest(String requestId, Optional<String> user) {
    final String requestUri = String.format(REQUEST_PAUSE_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("pause of request %s", requestId), Optional.absent(), user);
  }

  public void bounceSingularityRequest(String requestId, Optional<String> user) {
    final String requestUri = String.format(REQUEST_BOUNCE_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("bounce of request %s", requestId), Optional.absent(), user);
  }

  //
  // ACTIONS ON A DEPLOY FOR A SINGULARITY REQUEST
  //

  public SingularityRequestParent createDeployForSingularityRequest(String requestId, SingularityDeploy pendingDeploy, Optional<Boolean> deployUnpause, Optional<String> user) {
    final String requestUri = String.format(DEPLOYS_FORMAT, getHost(), contextPath);

    List<Pair<String, String>> queryParams = Lists.newArrayList();

    if (user.isPresent()) {
      queryParams.add(Pair.of("user", user.get()));
    }

    if (deployUnpause.isPresent()) {
      queryParams.add(Pair.of("deployUnpause", Boolean.toString(deployUnpause.get())));
    }

    HttpResponse response = post(requestUri, String.format("new deploy %s", new SingularityDeployKey(requestId, pendingDeploy.getId())),
        Optional.of(new SingularityDeployRequest(pendingDeploy, user, deployUnpause)), Optional.<String> absent());

    return getAndLogRequestAndDeployStatus(response.getAs(SingularityRequestParent.class));
  }

  private SingularityRequestParent getAndLogRequestAndDeployStatus(SingularityRequestParent singularityRequestParent) {
    String activeDeployId = (singularityRequestParent.getActiveDeploy().isPresent()) ? singularityRequestParent.getActiveDeploy().get().getId() : "No Active Deploy";
    String pendingDeployId = (singularityRequestParent.getPendingDeploy().isPresent()) ? singularityRequestParent.getPendingDeploy().get().getId() : "No Pending deploy";
    LOG.info("Deploy status: Singularity request {} -> pending deploy: '{}', active deploy: '{}'", singularityRequestParent.getRequest().getId(), pendingDeployId, activeDeployId);

    return singularityRequestParent;
  }

  public SingularityRequestParent cancelPendingDeployForSingularityRequest(String requestId, String deployId, Optional<String> user) {
    final String requestUri = String.format(DELETE_DEPLOY_FORMAT, getHost(), contextPath, deployId, requestId);

    SingularityRequestParent singularityRequestParent = delete(requestUri, "pending deploy", new SingularityDeployKey(requestId, deployId).getId(), user, Optional.of(SingularityRequestParent.class)).get();

    return getAndLogRequestAndDeployStatus(singularityRequestParent);
  }

  /**
   * Get all singularity requests that their state is either ACTIVE, PAUSED or COOLDOWN
   *
   * For the requests that are pending to become ACTIVE use:
   *    {@link SingularityClient#getPendingSingularityRequests()}
   *
   * For the requests that are cleaning up use:
   *    {@link SingularityClient#getCleanupSingularityRequests()}
   *
   *
   * Use {@link SingularityClient#getActiveSingularityRequests()}, {@link SingularityClient#getPausedSingularityRequests()},
   * {@link SingularityClient#getCoolDownSingularityRequests()} respectively to get only the ACTIVE, PAUSED or COOLDOWN requests.
   *
   * @return
   *    returns all the [ACTIVE, PAUSED, COOLDOWN] {@link SingularityRequest} instances.
   *
   */
  public Collection<SingularityRequest> getSingularityRequests() {
    final String requestUri = String.format(REQUESTS_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "[ACTIVE, PAUSED, COOLDOWN] requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that their state is ACTIVE
   *
   * @return
   *    All ACTIVE {@link SingularityRequest} instances
   */
  public Collection<SingularityRequest> getActiveSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_ACTIVE_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "ACTIVE requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that their state is PAUSED
   * ACTIVE requests are paused by users, which is equivalent to stop their tasks from running without undeploying them
   *
   * @return
   *    All PAUSED {@link SingularityRequest} instances
   */
  public Collection<SingularityRequest> getPausedSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_PAUSED_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "PAUSED requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that has been set to a COOLDOWN state by singularity
   *
   * @return
   *    All {@link SingularityRequest} instances that their state is COOLDOWN
   */
  public Collection<SingularityRequest> getCoolDownSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_COOLDOWN_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "COOLDOWN requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that are pending to become ACTIVE
   *
   * @return
   *    A collection of {@link SingularityPendingRequest} instances that hold information about the singularity requests that are pending to become ACTIVE
   */
  public Collection<SingularityPendingRequest> getPendingSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_PENDING_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "pending requests", PENDING_REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that are cleaning up
   * Requests that are cleaning up are those that have been marked for removal and their tasks are being stopped/removed
   * before they are being removed. So after their have been cleaned up, these request cease to exist in Singularity.
   *
   * @return
   *    A collection of {@link SingularityRequestCleanup} instances that hold information about all singularity requests
   *    that are marked for deletion and are currently cleaning up.
   */
  public Collection<SingularityRequestCleanup> getCleanupSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_CLEANUP_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "cleaning requests", CLEANUP_REQUESTS_COLLECTION);
  }

  //
  // SINGULARITY TASK COLLECTIONS
  //

  //
  // ACTIVE TASKS
  //

  public Collection<SingularityTask> getActiveTasks() {
    final String requestUri = String.format(TASKS_GET_ACTIVE_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "active tasks", TASKS_COLLECTION);
  }

  public Collection<SingularityTask> getActiveTasks(final String host) {
    final String requestUri = String.format(TASKS_GET_ACTIVE_PER_HOST_FORMAT, getHost(), contextPath, host);

    return getCollection(requestUri, String.format("active tasks on %s", host), TASKS_COLLECTION);
  }

  public Optional<SingularityTaskCleanupResult> killTask(String taskId, Optional<String> user) {
    final String requestUri = String.format(TASKS_KILL_TASK_FORMAT, getHost(), contextPath, taskId);

    return delete(requestUri, "task", taskId, user, Optional.of(SingularityTaskCleanupResult.class));
  }

  //
  // SCHEDULED TASKS
  //

  public Collection<SingularityTask> getScheduledTasks() {
    final String requestUri = String.format(TASKS_GET_SCHEDULED_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "scheduled tasks", TASKS_COLLECTION);
  }

  //
  // RACKS
  //

  public Collection<SingularityRack> getActiveRacks() {
    return getRacks(RACKS_GET_ACTIVE_FORMAT, "active");
  }

  public Collection<SingularityRack> getDeadRacks() {
    return getRacks(RACKS_GET_DEAD_FORMAT, "dead");
  }

  public Collection<SingularityRack> getDecomissioningRacks() {
    return getRacks(RACKS_GET_DECOMISSIONING_FORMAT, "decomissioning");
  }

  private Collection<SingularityRack> getRacks(String format, String type) {
    final String requestUri = String.format(format, getHost(), contextPath);

    return getCollection(requestUri, String.format("%s racks", type), RACKS_COLLECTION);
  }

  public void decomissionRack(String rackId, Optional<String> user) {
    final String requestUri = String.format(RACKS_DECOMISSION_FORMAT, getHost(), contextPath, rackId);

    post(requestUri, String.format("decomission rack %s", rackId), Optional.absent(), user);
  }

  public void deleteDecomissioningRack(String rackId, Optional<String> user) {
    final String requestUri = String.format(RACKS_DELETE_DECOMISSIONING_FORMAT, getHost(), contextPath, rackId);

    delete(requestUri, "rack", rackId, user);
  }

  public void deleteDeadRack(String rackId, Optional<String> user) {
    final String requestUri = String.format(RACKS_DELETE_DEAD_FORMAT, getHost(), contextPath, rackId);

    delete(requestUri, "dead rack", rackId, user);
  }

  //
  // SLAVES
  //

  public Collection<SingularitySlave> getActiveSlaves() {
    return getSlaves(SLAVES_GET_ACTIVE_FORMAT, "active");
  }

  public Collection<SingularitySlave> getDeadSlaves() {
    return getSlaves(SLAVES_GET_DEAD_FORMAT, "dead");
  }

  public Collection<SingularitySlave> getDecomissioningSlaves() {
    return getSlaves(SLAVES_GET_DECOMISSIONING_FORMAT, "decomissioning");
  }

  private Collection<SingularitySlave> getSlaves(String format, String type) {
    final String requestUri = String.format(format, getHost(), contextPath);

    return getCollection(requestUri, type, SLAVES_COLLECTION);
  }

  public void decomissionSlave(String slaveId, Optional<String> user) {
    final String requestUri = String.format(SLAVES_DECOMISSION_FORMAT, getHost(), contextPath, slaveId);

    post(requestUri, String.format("decomission slave %s", slaveId), Optional.absent(), user);
  }

  public void deleteDecomissioningSlave(String slaveId, Optional<String> user) {
    final String requestUri = String.format(SLAVES_DELETE_DECOMISSIONING_FORMAT, getHost(), contextPath, slaveId);

    delete(requestUri, "decomissioning slave", slaveId, user);
  }

  public void deleteDeadSlave(String slaveId, Optional<String> user) {
    final String requestUri = String.format(SLAVES_DELETE_DEAD_FORMAT, getHost(), contextPath, slaveId);

    delete(requestUri, "dead slave", slaveId, user);
  }

  //
  // TASK HISTORY
  //

  public Optional<SingularityTaskHistory> getHistoryForTask(String taskId) {
    final String requestUri = String.format(TASK_HISTORY_FORMAT, getHost(), contextPath, taskId);

    return getSingle(requestUri, "task history", taskId, SingularityTaskHistory.class);
  }

  public Collection<SingularityTaskIdHistory> getActiveTaskHistoryForRequest(String requestId) {
    final String requestUri = String.format(REQUEST_ACTIVE_TASKS_HISTORY_FORMAT, getHost(), contextPath, requestId);

    final String type = String.format("active task history for %s", requestId);

    return getCollection(requestUri, type, TASKID_HISTORY_COLLECTION);
  }

  public Collection<SingularityTaskIdHistory> getInactiveTaskHistoryForRequest(String requestId) {
    final String requestUri = String.format(REQUEST_INACTIVE_TASKS_HISTORY_FORMAT, getHost(), contextPath, requestId);

    final String type = String.format("inactive (failed, killed, lost) task history for request %s", requestId);

    return getCollection(requestUri, type, TASKID_HISTORY_COLLECTION);
  }

  public Optional<SingularityDeployHistory> getHistoryForRequestDeploy(String requestId, String deployId) {
    final String requestUri = String.format(REQUEST_DEPLOY_HISTORY_FORMAT, getHost(), contextPath, requestId, deployId);

    return getSingle(requestUri, "deploy history", new SingularityDeployKey(requestId, deployId).getId(), SingularityDeployHistory.class);
  }

  //
  // WEBHOOKS
  //

  public Optional<SingularityCreateResult> addWebhook(SingularityWebhook webhook, Optional<String> user) {
    final String requestUri = String.format(WEBHOOKS_FORMAT, getHost(), contextPath);

    return post(requestUri, String.format("webhook %s", webhook.getUri()), Optional.of(webhook), user, Optional.of(SingularityCreateResult.class));
  }

  public Optional<SingularityDeleteResult> deleteWebhook(String webhookId, Optional<String> user) {
    final String requestUri = String.format(WEBHOOKS_DELETE_FORMAT, getHost(), contextPath, webhookId);

    return delete(requestUri, String.format("webhook with id %s", webhookId), webhookId, user, Optional.of(SingularityDeleteResult.class));
  }

  public Collection<SingularityWebhook> getActiveWebhook() {
    final String requestUri = String.format(WEBHOOKS_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "active webhooks", WEBHOOKS_COLLECTION);
  }

  public Collection<SingularityDeployWebhook> getQueuedDeployUpdates(String webhookId) {
    final String requestUri = String.format(WEBHOOKS_GET_QUEUED_DEPLOY_UPDATES_FORMAT, getHost(), contextPath, webhookId);

    return getCollection(requestUri, "deploy updates", DEPLOY_UPDATES_COLLECTION);
  }

  public Collection<SingularityRequestHistory> getQueuedRequestUpdates(String webhookId) {
    final String requestUri = String.format(WEBHOOKS_GET_QUEUED_REQUEST_UPDATES_FORMAT, getHost(), contextPath, webhookId);

    return getCollection(requestUri, "request updates", REQUEST_UPDATES_COLLECTION);
  }

  public Collection<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(String webhookId) {
    final String requestUri = String.format(WEBHOOKS_GET_QUEUED_TASK_UPDATES_FORMAT, getHost(), contextPath, webhookId);

    return getCollection(requestUri, "request updates", TASK_UPDATES_COLLECTION);
  }

}
