package com.hubspot.singularity.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityClientCredentials;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityS3Log;
import com.hubspot.singularity.SingularitySandbox;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanupResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.api.SingularityBounceRequest;
import com.hubspot.singularity.api.SingularityDeleteRequestRequest;
import com.hubspot.singularity.api.SingularityDeployRequest;
import com.hubspot.singularity.api.SingularityExitCooldownRequest;
import com.hubspot.singularity.api.SingularityKillTaskRequest;
import com.hubspot.singularity.api.SingularityPauseRequest;
import com.hubspot.singularity.api.SingularityRunNowRequest;
import com.hubspot.singularity.api.SingularityScaleRequest;
import com.hubspot.singularity.api.SingularityUnpauseRequest;

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
  private static final String SLAVES_DECOMISSION_FORMAT = SLAVES_FORMAT + "/slave/%s/decommission";
  private static final String SLAVES_DELETE_FORMAT = SLAVES_FORMAT + "/slave/%s/decomissioning";

  private static final String TASKS_FORMAT = "http://%s/%s/tasks";
  private static final String TASKS_KILL_TASK_FORMAT = TASKS_FORMAT + "/task/%s";
  private static final String TASKS_GET_ACTIVE_FORMAT = TASKS_FORMAT + "/active";
  private static final String TASKS_GET_ACTIVE_ON_SLAVE_FORMAT = TASKS_FORMAT + "/active/slave/%s";
  private static final String TASKS_GET_SCHEDULED_FORMAT = TASKS_FORMAT + "/scheduled";

  private static final String HISTORY_FORMAT = "http://%s/%s/history";
  private static final String TASK_HISTORY_FORMAT = HISTORY_FORMAT + "/task/%s";
  private static final String REQUEST_HISTORY_FORMAT = HISTORY_FORMAT + "/request/%s/requests";
  private static final String TASK_HISTORY_BY_RUN_ID_FORMAT = HISTORY_FORMAT + "/request/%s/run/%s";
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
  private static final String REQUEST_BOUNCE_FORMAT = REQUESTS_FORMAT + "/request/%s/bounce";
  private static final String REQUEST_PAUSE_FORMAT = REQUESTS_FORMAT + "/request/%s/pause";
  private static final String REQUEST_UNPAUSE_FORMAT = REQUESTS_FORMAT + "/request/%s/unpause";
  private static final String REQUEST_SCALE_FORMAT = REQUESTS_FORMAT + "/request/%s/scale";
  private static final String REQUEST_RUN_FORMAT = REQUESTS_FORMAT + "/request/%s/run";
  private static final String REQUEST_EXIT_COOLDOWN_FORMAT = REQUESTS_FORMAT + "/request/%s/exit-cooldown";

  private static final String DEPLOYS_FORMAT = "http://%s/%s/deploys";
  private static final String DELETE_DEPLOY_FORMAT = DEPLOYS_FORMAT + "/deploy/%s/request/%s";

  private static final String WEBHOOKS_FORMAT = "http://%s/%s/webhooks";
  private static final String WEBHOOKS_DELETE_FORMAT = WEBHOOKS_FORMAT +"?webhookId=%s";
  private static final String WEBHOOKS_GET_QUEUED_DEPLOY_UPDATES_FORMAT = WEBHOOKS_FORMAT + "/deploy?webhookId=%s";
  private static final String WEBHOOKS_GET_QUEUED_REQUEST_UPDATES_FORMAT = WEBHOOKS_FORMAT + "/request?webhookId=%s";
  private static final String WEBHOOKS_GET_QUEUED_TASK_UPDATES_FORMAT = WEBHOOKS_FORMAT + "/task?webhookId=%s";

  private static final String SANDBOX_FORMAT = "http://%s/%s/sandbox";
  private static final String SANDBOX_BROWSE_FORMAT = SANDBOX_FORMAT + "/%s/browse";
  private static final String SANDBOX_READ_FILE_FORMAT = SANDBOX_FORMAT + "/%s/read";

  private static final String S3_LOG_FORMAT = "http://%s/%s/logs";
  private static final String S3_LOG_GET_TASK_LOGS = S3_LOG_FORMAT + "/task/%s";
  private static final String S3_LOG_GET_REQUEST_LOGS = S3_LOG_FORMAT + "/request/%s";
  private static final String S3_LOG_GET_DEPLOY_LOGS = S3_LOG_FORMAT + "/request/%s/deploy/%s";

  private static final TypeReference<Collection<SingularityRequestParent>> REQUESTS_COLLECTION = new TypeReference<Collection<SingularityRequestParent>>() {};
  private static final TypeReference<Collection<SingularityPendingRequest>> PENDING_REQUESTS_COLLECTION = new TypeReference<Collection<SingularityPendingRequest>>() {};
  private static final TypeReference<Collection<SingularityRequestCleanup>> CLEANUP_REQUESTS_COLLECTION = new TypeReference<Collection<SingularityRequestCleanup>>() {};
  private static final TypeReference<Collection<SingularityTask>> TASKS_COLLECTION = new TypeReference<Collection<SingularityTask>>() {};
  private static final TypeReference<Collection<SingularityTaskIdHistory>> TASKID_HISTORY_COLLECTION = new TypeReference<Collection<SingularityTaskIdHistory>>() {};
  private static final TypeReference<Collection<SingularityRack>> RACKS_COLLECTION = new TypeReference<Collection<SingularityRack>>() {};
  private static final TypeReference<Collection<SingularitySlave>> SLAVES_COLLECTION = new TypeReference<Collection<SingularitySlave>>() {};
  private static final TypeReference<Collection<SingularityWebhook>> WEBHOOKS_COLLECTION = new TypeReference<Collection<SingularityWebhook>>() {};
  private static final TypeReference<Collection<SingularityDeployUpdate>> DEPLOY_UPDATES_COLLECTION = new TypeReference<Collection<SingularityDeployUpdate>>() {};
  private static final TypeReference<Collection<SingularityRequestHistory>> REQUEST_UPDATES_COLLECTION = new TypeReference<Collection<SingularityRequestHistory>>() {};
  private static final TypeReference<Collection<SingularityTaskHistoryUpdate>> TASK_UPDATES_COLLECTION = new TypeReference<Collection<SingularityTaskHistoryUpdate>>() {};
  private static final TypeReference<Collection<SingularityTaskRequest>> TASKS_REQUEST_COLLECTION = new TypeReference<Collection<SingularityTaskRequest>>() {};
  private static final TypeReference<Collection<SingularityS3Log>> S3_LOG_COLLECTION = new TypeReference<Collection<SingularityS3Log>>() {};
  private static final TypeReference<Collection<SingularityRequestHistory>> REQUEST_HISTORY_COLLECTION = new TypeReference<Collection<SingularityRequestHistory>>() {};

  private final Random random;
  private final Provider<List<String>> hostsProvider;
  private final String contextPath;

  private final HttpClient httpClient;
  private final Optional<SingularityClientCredentials> credentials;

  @Inject
  @Deprecated
  public SingularityClient(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath, @Named(SingularityClientModule.HTTP_CLIENT_NAME) HttpClient httpClient, @Named(SingularityClientModule.HOSTS_PROPERTY_NAME) String hosts) {
    this(contextPath, httpClient, Arrays.asList(hosts.split(",")), Optional.<SingularityClientCredentials>absent());
  }

  public SingularityClient(String contextPath, HttpClient httpClient, List<String> hosts, Optional<SingularityClientCredentials> credentials) {
    this(contextPath, httpClient, ProviderUtils.<List<String>>of(ImmutableList.copyOf(hosts)), credentials);
  }

  public SingularityClient(String contextPath, HttpClient httpClient, Provider<List<String>> hostsProvider, Optional<SingularityClientCredentials> credentials) {
    this.httpClient = httpClient;
    this.contextPath = contextPath;

    this.hostsProvider = hostsProvider;
    this.random = new Random();

    this.credentials = credentials;
  }

  private String getHost() {
    final List<String> hosts = hostsProvider.get();
    return hosts.get(random.nextInt(hosts.size()));
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
    return getSingleWithParams(uri, type, id, Optional.<Map<String, Object>>absent(), clazz);
  }

  private <T> Optional<T> getSingleWithParams(String uri, String type, String id, Optional<Map<String, Object>> queryParams, Class<T> clazz) {
    checkNotNull(id, String.format("Provide a %s id", type));

    LOG.info("Getting {} {} from {}", type, id, uri);

    final long start = System.currentTimeMillis();

    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(uri);

    if (queryParams.isPresent()) {
      addQueryParams(requestBuilder, queryParams.get());
    }

    addCredentials(requestBuilder);

    HttpResponse response = httpClient.execute(requestBuilder.build());

    if (response.getStatusCode() == 404) {
      return Optional.absent();
    }

    checkResponse(type, response);

    LOG.info("Got {} {} in {}ms", type, id, System.currentTimeMillis() - start);

    return Optional.fromNullable(response.getAs(clazz));
  }

  private <T> Collection<T> getCollection(String uri, String type, TypeReference<Collection<T>> typeReference) {
    return getCollectionWithParams(uri, type, Optional.<Map<String, Object>>absent(), typeReference);
  }

  private <T> Collection<T> getCollectionWithParams(String uri, String type, Optional<Map<String, Object>> queryParams, TypeReference<Collection<T>> typeReference) {
    LOG.info("Getting all {} from {}", type, uri);

    final long start = System.currentTimeMillis();

    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(uri);

    if (queryParams.isPresent()) {
      addQueryParams(requestBuilder, queryParams.get());
    }

    addCredentials(requestBuilder);

    HttpResponse response = httpClient.execute(requestBuilder.build());

    if (response.getStatusCode() == 404) {
      return ImmutableList.of();
    }

    checkResponse(type, response);

    LOG.info("Got {} in {}ms", type, System.currentTimeMillis() - start);

    return response.getAs(typeReference);
  }

  private void addQueryParams(HttpRequest.Builder requestBuilder, Map<String, Object> queryParams) {
    for (Entry<String, Object> queryParamEntry : queryParams.entrySet()) {
      if (queryParamEntry.getValue() instanceof String) {
        requestBuilder.setQueryParam(queryParamEntry.getKey()).to((String) queryParamEntry.getValue());
      } else if (queryParamEntry.getValue() instanceof Integer) {
        requestBuilder.setQueryParam(queryParamEntry.getKey()).to((Integer) queryParamEntry.getValue());
      } else if (queryParamEntry.getValue() instanceof Long) {
        requestBuilder.setQueryParam(queryParamEntry.getKey()).to((Long) queryParamEntry.getValue());
      } else if (queryParamEntry.getValue() instanceof Boolean) {
        requestBuilder.setQueryParam(queryParamEntry.getKey()).to((Boolean) queryParamEntry.getValue());
      } else {
        throw new RuntimeException(String.format("The type '%s' of query param %s is not supported. Only String, long, int and boolean values are supported",
            queryParamEntry.getValue().getClass().getName(), queryParamEntry.getKey()));
      }
    }
  }

  private void addCredentials(HttpRequest.Builder requestBuilder) {
    if (credentials.isPresent()) {
      requestBuilder.addHeader(credentials.get().getHeaderName(), credentials.get().getToken());
    }
  }

  private <T> void delete(String uri, String type, String id) {
    delete(uri, type, id, Optional.absent());
  }

  private <T> void delete(String uri, String type, String id, Optional<?> body) {
    delete(uri, type, id, body, Optional.<Class<T>> absent());
  }

  private <T> Optional<T> delete(String uri, String type, String id, Optional<?> body, Optional<Class<T>> clazz) {
    LOG.info("Deleting {} {} from {}", type, id, uri);

    final long start = System.currentTimeMillis();

    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.DELETE);

    if (body.isPresent()) {
      request.setBody(body.get());
    }

    addCredentials(request);

    HttpResponse response = httpClient.execute(request.build());

    if (response.getStatusCode() == 404) {
      LOG.info("{} ({}) was not found", type, id);
      return Optional.absent();
    }

    checkResponse(type, response);

    LOG.info("Deleted {} ({}) from Singularity in %sms", type, id, System.currentTimeMillis() - start);

    if (clazz.isPresent()) {
      return Optional.of(response.getAs(clazz.get()));
    }

    return Optional.absent();
  }

  private HttpResponse put(String uri, String type, Optional<?> body) {
    return executeRequest(uri, type, body, Method.PUT);
  }

  private <T> Optional<T> post(String uri, String type, Optional<?> body, Optional<Class<T>> clazz) {
    try {
      HttpResponse response = executeRequest(uri, type, body, Method.POST);

      if (clazz.isPresent()) {
        return Optional.of(response.getAs(clazz.get()));
      }
    } catch (Exception e) {
      LOG.warn("Http post failed", e);
    }

    return Optional.<T>absent();
  }

  private HttpResponse post(String uri, String type, Optional<?> body) {
    return executeRequest(uri, type, body, Method.POST);
  }

  private HttpResponse executeRequest(String uri, String type, Optional<?> body, Method method) {

    final long start = System.currentTimeMillis();

    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(method);

    if (body.isPresent()) {
      request.setBody(body.get());
    }

    addCredentials(request);

    HttpResponse response = httpClient.execute(request.build());

    checkResponse(type, response);

    LOG.info("Successfully {}ed {} in {}ms", method, type, System.currentTimeMillis() - start);

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
      request.setQueryParam("skipCache").to(skipCache.get().booleanValue());
    }
    if (includeRequestIds.isPresent()) {
      request.setQueryParam("includeRequestIds").to(includeRequestIds.get().booleanValue());
    }

    addCredentials(request);

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

  public void createOrUpdateSingularityRequest(SingularityRequest request) {
    checkNotNull(request.getId(), "A posted Singularity Request must have an id");

    final String requestUri = String.format(REQUEST_CREATE_OR_UPDATE_FORMAT, getHost(), contextPath);

    post(requestUri, String.format("request %s", request.getId()), Optional.of(request));
  }

  /**
   * Delete a singularity request .
   * If the deletion is successful the deleted singularity request is returned.
   * If the request to be deleted is not found {code Optional.absent()} is returned
   * If an error occurs during deletion an exception is returned
   * If the singularity request to be deleted is paused the deletion will fail with an exception
   * If you want to delete a paused singularity request use the provided {@link SingularityClient#deletePausedSingularityRequest}
   *
   * @param requestId
   *      the id of the singularity request to delete
   * @param user
   *      the ...
   * @return
   *      the singularity request that was deleted
   */
  public Optional<SingularityRequest> deleteSingularityRequest(String requestId, Optional<SingularityDeleteRequestRequest> deleteRequest) {
    final String requestUri = String.format(REQUEST_DELETE_ACTIVE_FORMAT, getHost(), contextPath, requestId);

    return delete(requestUri, "active request", requestId, deleteRequest, Optional.of(SingularityRequest.class));
  }

  public void pauseSingularityRequest(String requestId, Optional<SingularityPauseRequest> pauseRequest) {
    final String requestUri = String.format(REQUEST_PAUSE_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("pause of request %s", requestId), pauseRequest);
  }

  public void unpauseSingularityRequest(String requestId, Optional<SingularityUnpauseRequest> unpauseRequest) {
    final String requestUri = String.format(REQUEST_UNPAUSE_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("unpause of request %s", requestId), unpauseRequest);
  }

  public void scaleSingularityRequest(String requestId, SingularityScaleRequest scaleRequest) {
    final String requestUri = String.format(REQUEST_SCALE_FORMAT, getHost(), contextPath, requestId);
    put(requestUri, String.format("Scale of Request %s", requestId), Optional.of(scaleRequest));
  }

  public void runSingularityRequest(String requestId, Optional<SingularityRunNowRequest> runNowRequest) {
    final String requestUri = String.format(REQUEST_RUN_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("run of request %s", requestId), runNowRequest);
  }

  public void bounceSingularityRequest(String requestId, Optional<SingularityBounceRequest> bounceOptions) {
    final String requestUri = String.format(REQUEST_BOUNCE_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("bounce of request %s", requestId), bounceOptions);
  }

  public void exitCooldown(String requestId, Optional<SingularityExitCooldownRequest> exitCooldownRequest) {
    final String requestUri = String.format(REQUEST_EXIT_COOLDOWN_FORMAT, getHost(), contextPath, requestId);

    post(requestUri, String.format("exit cooldown of request %s", requestId), exitCooldownRequest);
  }

  //
  // ACTIONS ON A DEPLOY FOR A SINGULARITY REQUEST
  //

  public SingularityRequestParent createDeployForSingularityRequest(String requestId, SingularityDeploy pendingDeploy, Optional<Boolean> deployUnpause, Optional<String> message) {
    final String requestUri = String.format(DEPLOYS_FORMAT, getHost(), contextPath);

    HttpResponse response = post(requestUri, String.format("new deploy %s", new SingularityDeployKey(requestId, pendingDeploy.getId())),
        Optional.of(new SingularityDeployRequest(pendingDeploy, deployUnpause, message)));

    return getAndLogRequestAndDeployStatus(response.getAs(SingularityRequestParent.class));
  }

  private SingularityRequestParent getAndLogRequestAndDeployStatus(SingularityRequestParent singularityRequestParent) {
    String activeDeployId = singularityRequestParent.getActiveDeploy().isPresent() ? singularityRequestParent.getActiveDeploy().get().getId() : "No Active Deploy";
    String pendingDeployId = singularityRequestParent.getPendingDeploy().isPresent() ? singularityRequestParent.getPendingDeploy().get().getId() : "No Pending deploy";
    LOG.info("Deploy status: Singularity request {} -> pending deploy: '{}', active deploy: '{}'", singularityRequestParent.getRequest().getId(), pendingDeployId, activeDeployId);

    return singularityRequestParent;
  }

  public SingularityRequestParent cancelPendingDeployForSingularityRequest(String requestId, String deployId) {
    final String requestUri = String.format(DELETE_DEPLOY_FORMAT, getHost(), contextPath, deployId, requestId);

    SingularityRequestParent singularityRequestParent = delete(requestUri, "pending deploy", new SingularityDeployKey(requestId, deployId).getId(), Optional.absent(),
        Optional.of(SingularityRequestParent.class)).get();

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
   *    returns all the [ACTIVE, PAUSED, COOLDOWN] {@link SingularityRequestParent} instances.
   *
   */
  public Collection<SingularityRequestParent> getSingularityRequests() {
    final String requestUri = String.format(REQUESTS_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "[ACTIVE, PAUSED, COOLDOWN] requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that their state is ACTIVE
   *
   * @return
   *    All ACTIVE {@link SingularityRequestParent} instances
   */
  public Collection<SingularityRequestParent> getActiveSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_ACTIVE_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "ACTIVE requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that their state is PAUSED
   * ACTIVE requests are paused by users, which is equivalent to stop their tasks from running without undeploying them
   *
   * @return
   *    All PAUSED {@link SingularityRequestParent} instances
   */
  public Collection<SingularityRequestParent> getPausedSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_PAUSED_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "PAUSED requests", REQUESTS_COLLECTION);
  }

  /**
   * Get all requests that has been set to a COOLDOWN state by singularity
   *
   * @return
   *    All {@link SingularityRequestParent} instances that their state is COOLDOWN
   */
  public Collection<SingularityRequestParent> getCoolDownSingularityRequests() {
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

  public Collection<SingularityTask> getActiveTasksOnSlave(final String slaveId) {
    final String requestUri = String.format(TASKS_GET_ACTIVE_ON_SLAVE_FORMAT, getHost(), contextPath, slaveId);

    return getCollection(requestUri, String.format("active tasks on slave %s", slaveId), TASKS_COLLECTION);
  }

  public Optional<SingularityTaskCleanupResult> killTask(String taskId, Optional<SingularityKillTaskRequest> killTaskRequest) {
    final String requestUri = String.format(TASKS_KILL_TASK_FORMAT, getHost(), contextPath, taskId);

    return delete(requestUri, "task", taskId, killTaskRequest, Optional.of(SingularityTaskCleanupResult.class));
  }

  //
  // SCHEDULED TASKS
  //

  public Collection<SingularityTaskRequest> getScheduledTasks() {
    final String requestUri = String.format(TASKS_GET_SCHEDULED_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "scheduled tasks", TASKS_REQUEST_COLLECTION);
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

  public void decomissionRack(String rackId) {
    final String requestUri = String.format(RACKS_DECOMISSION_FORMAT, getHost(), contextPath, rackId);

    post(requestUri, String.format("decomission rack %s", rackId), Optional.absent());
  }

  public void deleteDecomissioningRack(String rackId) {
    final String requestUri = String.format(RACKS_DELETE_DECOMISSIONING_FORMAT, getHost(), contextPath, rackId);

    delete(requestUri, "rack", rackId);
  }

  public void deleteDeadRack(String rackId) {
    final String requestUri = String.format(RACKS_DELETE_DEAD_FORMAT, getHost(), contextPath, rackId);

    delete(requestUri, "dead rack", rackId);
  }

  //
  // SLAVES
  //

  /**
   * Use {@link getSlaves} specifying the desired slave state to filter by
   *
   */
  @Deprecated
  public Collection<SingularitySlave> getActiveSlaves() {
    return getSlaves(Optional.of(MachineState.ACTIVE));
  }

  /**
   * Use {@link getSlaves} specifying the desired slave state to filter by
   *
   */
  @Deprecated
  public Collection<SingularitySlave> getDeadSlaves() {
    return getSlaves(Optional.of(MachineState.DEAD));
  }

  /**
   * Use {@link getSlaves} specifying the desired slave state to filter by
   *
   */
  @Deprecated
  public Collection<SingularitySlave> getDecomissioningSlaves() {
    return getSlaves(Optional.of(MachineState.DECOMMISSIONING));
  }

  /**
   * Retrieve the list of all known slaves, optionally filtering by a particular slave state
   *
   * @param slaveState
   *    Optionally specify a particular state to filter slaves by
   * @return
   *    A collection of {@link SingularitySlave}
   */
  public Collection<SingularitySlave> getSlaves(Optional<MachineState> slaveState) {
    final String requestUri = String.format(SLAVES_FORMAT, getHost(), contextPath);

    Optional<Map<String, Object>> maybeQueryParams = Optional.<Map<String, Object>>absent();

    String type = "slaves";

    if (slaveState.isPresent()) {
      maybeQueryParams = Optional.<Map<String, Object>>of(ImmutableMap.<String, Object>of("state", slaveState.get().toString()));

      type = String.format("%s slaves", slaveState.get().toString());
    }

    return getCollectionWithParams(requestUri, type, maybeQueryParams, SLAVES_COLLECTION);
  }

  public void decomissionSlave(String slaveId) {
    final String requestUri = String.format(SLAVES_DECOMISSION_FORMAT, getHost(), contextPath, slaveId);

    post(requestUri, String.format("decomission slave %s", slaveId), Optional.absent());
  }

  public void deleteSlave(String slaveId) {
    final String requestUri = String.format(SLAVES_DELETE_FORMAT, getHost(), contextPath, slaveId);

    delete(requestUri, "deleting slave", slaveId);
  }

  //
  // REQUEST HISTORY
  //

  /**
   * Retrieve a paged list of updates for a particular {@link SingularityRequest}
   *
   * @param requestId
   *    Request ID to look up
   * @param count
   *    Number of items to return per page
   * @param page
   *    Which page of items to return
   * @return
   *    A list of {@link SingularityRequestHistory}
   */
  public Collection<SingularityRequestHistory> getHistoryForRequest(String requestId,  Optional<Integer> count, Optional<Integer> page) {
    final String requestUri = String.format(REQUEST_HISTORY_FORMAT, getHost(), contextPath, requestId);

    Optional<Map<String, Object>> maybeQueryParams = Optional.<Map<String, Object>>absent();

    ImmutableMap.Builder<String, Object> queryParamsBuilder = ImmutableMap.<String, Object>builder();

    if (count.isPresent() ) {
      queryParamsBuilder.put("count", count.get());
    }

    if (page.isPresent()) {
      queryParamsBuilder.put("page", page.get());
    }

    Map<String, Object> queryParams = queryParamsBuilder.build();
    if (!queryParams.isEmpty()) {
      maybeQueryParams = Optional.of(queryParams);
    }

    return getCollectionWithParams(requestUri, "request history", maybeQueryParams, REQUEST_HISTORY_COLLECTION);
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

  public Optional<SingularityTaskIdHistory> getHistoryForTask(String requestId, String runId) {
    final String requestUri = String.format(TASK_HISTORY_BY_RUN_ID_FORMAT, getHost(), contextPath, requestId, runId);

    return getSingle(requestUri, "task history", requestId, SingularityTaskIdHistory.class);
  }

  //
  // WEBHOOKS
  //

  public Optional<SingularityCreateResult> addWebhook(SingularityWebhook webhook) {
    final String requestUri = String.format(WEBHOOKS_FORMAT, getHost(), contextPath);

    return post(requestUri, String.format("webhook %s", webhook.getUri()), Optional.of(webhook), Optional.of(SingularityCreateResult.class));
  }

  public Optional<SingularityDeleteResult> deleteWebhook(String webhookId) {
    final String requestUri = String.format(WEBHOOKS_DELETE_FORMAT, getHost(), contextPath, webhookId);

    return delete(requestUri, String.format("webhook with id %s", webhookId), webhookId, Optional.absent(), Optional.of(SingularityDeleteResult.class));
  }

  public Collection<SingularityWebhook> getActiveWebhook() {
    final String requestUri = String.format(WEBHOOKS_FORMAT, getHost(), contextPath);

    return getCollection(requestUri, "active webhooks", WEBHOOKS_COLLECTION);
  }

  public Collection<SingularityDeployUpdate> getQueuedDeployUpdates(String webhookId) {
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

  //
  // SANDBOX
  //

  /**
   * Retrieve information about a specific task's sandbox
   *
   * @param taskId
   *    The task ID to browse
   * @param path
   *    The path to browse from.
   *    if not specified it will browse from the sandbox root.
   * @return
   *    A {@link SingularitySandbox} object that captures the information for the path to a specific task's Mesos sandbox
   */
  public Optional<SingularitySandbox> browseTaskSandBox(String taskId, String path) {
    final String requestUrl = String.format(SANDBOX_BROWSE_FORMAT, getHost(), contextPath, taskId);

    return getSingleWithParams(requestUrl, "browse sandbox for task", taskId, Optional.<Map<String, Object>>of(ImmutableMap.<String, Object>of("path", path)), SingularitySandbox.class);

  }

  /**
   * Retrieve part of the contents of a file in a specific task's sandbox.
   *
   * @param taskId
   *    The task ID of the sandbox to read from
   * @param path
   *    The path to the file to be read. Relative to the sandbox root (without a leading slash)
   * @param grep
   *    Optional string to grep for
   * @param offset
   *    Byte offset to start reading from
   * @param length
   *    Maximum number of bytes to read
   * @return
   *    A {@link MesosFileChunkObject} that contains the requested partial file contents
   */
  public Optional<MesosFileChunkObject> readSandBoxFile(String taskId, String path, Optional<String> grep, Optional<Long> offset, Optional<Long> length) {
    final String requestUrl = String.format(SANDBOX_READ_FILE_FORMAT, getHost(), contextPath, taskId);

    Builder<String, Object> queryParamBuider = ImmutableMap.<String, Object>builder().put("path", path);

    if (grep.isPresent()) {
      queryParamBuider.put("grep", grep.get());
    }
    if (offset.isPresent()) {
      queryParamBuider.put("offset", offset.get());
    }
    if (length.isPresent()) {
      queryParamBuider.put("length", length.get());
    }

    return getSingleWithParams(requestUrl, "Read sandbox file for task", taskId, Optional.<Map<String, Object>>of(queryParamBuider.build()), MesosFileChunkObject.class);
  }

  //
  // S3 LOGS
  //

  /**
   * Retrieve the list of logs stored in S3 for a specific task
   *
   * @param taskId
   *    The task ID to search for
   *
   * @return
   *    A collection of {@link SingularityS3Log}
   */
  public Collection<SingularityS3Log> getTaskLogs(String taskId) {
    final String requestUri = String.format(S3_LOG_GET_TASK_LOGS, getHost(), contextPath, taskId);

    final String type = String.format("S3 logs for task %s", taskId);

    return getCollection(requestUri, type, S3_LOG_COLLECTION);
  }

  /**
   * Retrieve the list of logs stored in S3 for a specific request
   *
   * @param requestId
   *    The request ID to search for
   *
   * @return
   *     A collection of {@link SingularityS3Log}
   */
  public Collection<SingularityS3Log> getRequestLogs(String requestId) {
    final String requestUri = String.format(S3_LOG_GET_REQUEST_LOGS, getHost(), contextPath, requestId);

    final String type = String.format("S3 logs for request %s", requestId);

    return getCollection(requestUri, type, S3_LOG_COLLECTION);
  }

  /**
   * Retrieve the list of logs stored in S3 for a specific deploy if a singularity request
   *
   * @param requestId
   *    The request ID to search for
   * @param deployId
   *    The deploy ID (within the specified request) to search for
   *
   * @return
   *    A collection of {@link SingularityS3Log}
   */
  public Collection<SingularityS3Log> getDeployLogs(String requestId, String deployId) {
    final String requestUri = String.format(S3_LOG_GET_DEPLOY_LOGS, getHost(), contextPath, requestId, deployId);

    final String type = String.format("S3 logs for deploy %s of request %s", deployId, requestId);

    return getCollection(requestUri, type, S3_LOG_COLLECTION);
  }

}
