package com.hubspot.singularity.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class SingularityClient {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityClient.class);

  private static final String WEBHOOK_FORMAT = "http://%s/%s/webhooks";

  private static final String TASKS_FORMAT = "http://%s/%s/tasks";
  private static final String TASKS_GET_ACTIVE_FORMAT = TASKS_FORMAT + "/active";
  private static final String TASKS_GET_SCHEDULED_FORMAT = TASKS_FORMAT + "/scheduled";

  private static final String HISTORY_FORMAT = "http://%s/%s/history";
  private static final String TASK_HISTORY_FORMAT = HISTORY_FORMAT + "/task/%s";
  private static final String REQUEST_ACTIVE_TASKS_HISTORY_FORMAT = HISTORY_FORMAT + "/request/%s/tasks/active";
  
  private static final String REQUESTS_FORMAT = "http://%s/%s/requests";
  private static final String REQUESTS_GET_ACTIVE_FORMAT = REQUESTS_FORMAT + "/active";
  private static final String REQUESTS_GET_PAUSED_FORMAT = REQUESTS_FORMAT + "/paused";
  private static final String REQUEST_GET_FORMAT = REQUESTS_FORMAT + "/request/%s";
  private static final String REQUEST_DELETE_ACTIVE_FORMAT = REQUESTS_FORMAT + "/request/%s";
  private static final String REQUEST_DELETE_PAUSED_FORMAT = REQUESTS_FORMAT + "/request/%s/paused";
  private static final String REQUEST_BOUNCE_FORMAT = REQUESTS_FORMAT + "/request/%s/bounce";
  private static final String REQUEST_PAUSE_FORMAT = REQUESTS_FORMAT + "/request/%s/pause";
  private static final String REQUEST_CREATE_DEPLOY_FORMAT = REQUESTS_FORMAT + "/request/%s/deploy";
  private static final String REQUEST_DELETE_DEPLOY_FORMAT = REQUESTS_FORMAT + "/request/%s/deploy/%s";
  
  private static final String QUERY_PARAM_USER_FORMAT = "%s?user=%s";
  
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  private static final TypeReference<Collection<SingularityRequest>> REQUESTS_COLLECTION = new TypeReference<Collection<SingularityRequest>>() {};
  private static final TypeReference<Collection<SingularityTask>> TASKS_COLLECTION = new TypeReference<Collection<SingularityTask>>() {};
  private static final TypeReference<Collection<SingularityTaskIdHistory>> TASKID_HISTORY_COLLECTION = new TypeReference<Collection<SingularityTaskIdHistory>>() {};
  
  private final Random random;
  private final List<String> hosts;
  private final String contextPath;

  private final ObjectMapper objectMapper;
  private final AsyncHttpClient httpClient;
  
  @Inject
  public SingularityClient(@Named(SingularityClientModule.CONTEXT_PATH) String contextPath, @Named(SingularityClientModule.HTTP_CLIENT_NAME) AsyncHttpClient httpClient, @Named(SingularityClientModule.OBJECT_MAPPER_NAME) ObjectMapper objectMapper, @Named(SingularityClientModule.HOSTS_PROPERTY_NAME) List<String> hosts) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    this.contextPath = contextPath;
    
    this.hosts = hosts;
    this.random = new Random();
  }

  private String getHost() {
    return hosts.get(random.nextInt(hosts.size()));
  }
  
  private void checkResponse(String type, Response response) {
    if (!isSuccess(response)) {
      throw fail(type, response);
    }
  }
  
  private SingularityClientException fail(String type, Response response) {
    String body = "";
    
    try {
      body = response.getResponseBody();
    } catch (IOException ioe) {
      LOG.warn("Unable to read body", ioe);
    }
    
    String uri = "";
    
    try {
      uri = response.getUri().toString();
    } catch (MalformedURLException wtf) {
      LOG.warn("Unable to read uri", wtf);
    }
    
    throw new SingularityClientException(String.format("Failed %s action on Singularity (%s) - code: %s, %s", type, uri, response.getStatusCode(), body));
  }
  
  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }
  
  private Response deleteUri(String requestUri) {
    try {
      return httpClient.prepareDelete(requestUri).execute().get();
    } catch (Exception e) {
      throw new SingularityClientException("Failed to delete Singularity request due to exception", e);
    }
  }

  private Response getUri(String requestUri) {
    try {
      return httpClient.prepareGet(requestUri).execute().get();
    } catch (Exception e) {
      throw new SingularityClientException("Failed to delete Singularity request due to exception", e);
    }
  }

  private Response postUri(String requestUri) {
    try {
      return httpClient.preparePost(requestUri).execute().get();
    } catch (Exception e) {
      throw new SingularityClientException("Failed to POST due to exception", e);
    }
  }

  private Response postUri(String requestUri, Object data) {
    try {
      return httpClient.preparePost(requestUri)
          .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
          .setBody(objectMapper.writeValueAsBytes(data))
          .execute().get();
    } catch (Exception e) {
      throw new SingularityClientException("Failed to POST due to exception", e);
    }
  }
  
  private String finishUri(String uri, Optional<String> user) {
    if (!user.isPresent()) {
      return uri;
    }
    
    return String.format(QUERY_PARAM_USER_FORMAT, uri, user.get());
  }

  //
  // ACTIONS ON A SINGLE SINGULARITY REQUEST
  //

  public Optional<SingularityRequestParent> getSingularityRequest(String requestId) {
    checkNotNull(requestId, "You should provide a request id");
    final String singularityApiRequestUri = String.format(REQUEST_GET_FORMAT, getHost(), contextPath, requestId);

    LOG.info(String.format("Getting request with id: %s", requestId));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(singularityApiRequestUri);
    
    if (isSuccess(getResponse)) {
      LOG.info(String.format("Successfully got Singularity Request with id: '%s', in %sms", requestId, System.currentTimeMillis() - start));
      try {
        return Optional.fromNullable(objectMapper.readValue(getResponse.getResponseBodyAsStream(), SingularityRequestParent.class));
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    else if (getResponse.getStatusCode() == 404) {
      return Optional.<SingularityRequestParent>absent();
    }
    else {
      throw fail("Get 'Singularity Request' failed", getResponse);
    }

  }
  
  public void createOrUpdateSingularityRequest(SingularityRequest request) {
    checkNotNull(request.getId(), "Posted Singularity Request should have an id");
    final String singularityApiRequestsUri = String.format(REQUEST_GET_FORMAT, getHost(), contextPath);
    
    LOG.info(String.format("Posting new or updated request with id: %s", request.getId()));
    
    final long start = System.currentTimeMillis();
    
    Response postResponse = postUri(singularityApiRequestsUri, request);

    checkResponse("get Singularity Request", postResponse);
    
    LOG.info(String.format("Successfully posted Singularity Request with id: '%s', in %sms", request.getId(), System.currentTimeMillis() - start));
  }
  
  /**
   * Delete a singularity request that is active.
   * If the deletion is successful the deleted singularity request is returned.
   * If the request to be deleted is not found {@link Absent} is returned
   * If the singularity request to be deleted is paused the deletion will fail ({@link Absent} will be returned)
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
    final String requestUri = finishUri(String.format(REQUEST_DELETE_ACTIVE_FORMAT, getHost(), contextPath, requestId), user);

    LOG.info(String.format("Deleting active singularity request with id: '%s' - (DELETE %s)", requestId, requestUri));
  
    final long start = System.currentTimeMillis();
    
    Response deleteResponse = deleteUri(requestUri);

    if (deleteResponse.getStatusCode() == 404) {
      return Optional.absent();
    }
    
    checkResponse("delete active singularity request", deleteResponse);
    
    LOG.info(String.format("Successfully deleted active singularity request with id: '%s' from Singularity in %sms", requestId, System.currentTimeMillis() - start));

    try {
      return Optional.of(objectMapper.readValue(deleteResponse.getResponseBodyAsStream(), SingularityRequest.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  /**
   * use instead {@link SingularityClient#deleteActiveSingularityRequest(String, Optional)}
   */
  @Deprecated
  public Optional<SingularityRequest> removeActiveRequest(String id, Optional<String> user) {
    return deleteActiveSingularityRequest(id, user);
  }
  
  public Optional<SingularityRequest> deletePausedSingularityRequest(String requestId, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_DELETE_PAUSED_FORMAT, getHost(), contextPath, requestId), user);

    LOG.info(String.format("Deleting paused singularity request with id: '%s' - (DELETE %s)", requestId, requestUri));

    final long start = System.currentTimeMillis();

    Response deleteResponse = deleteUri(requestUri);

    if (deleteResponse.getStatusCode() == 404) {
      return Optional.absent();
    }

    checkResponse("delete paused singularity request", deleteResponse);

    LOG.info(String.format("Successfully deleted paused singularity request with id: '%s' from Singularity in %sms", requestId, System.currentTimeMillis() - start));

    try {
      return Optional.of(objectMapper.readValue(deleteResponse.getResponseBodyAsStream(), SingularityRequest.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  /**
   * use instead {@link SingularityClient#deletePausedSingularityRequest(String, Optional)}
   */
  @Deprecated
  public Optional<SingularityRequest> removePausedRequest(String id, Optional<String> user) {
    return deletePausedSingularityRequest(id, user);
  }
  
  public void pauseSingularityRequest(String requestId, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_PAUSE_FORMAT, getHost(), contextPath, requestId), user);

    LOG.info(String.format("Pausing singularity request with id:  '%s' - (POST %s)", requestId, requestUri));

    final long start = System.currentTimeMillis();

    Response postResponse = postUri(requestUri);

    checkResponse("pause singularity request", postResponse);

    LOG.info(String.format("Successfully paused singularity request with id: '%s' in %sms", requestId, System.currentTimeMillis() - start));
  }
  
  /**
   * use instead {@link SingularityClient#pauseSingularityRequest(String, Optional)}
   */
  @Deprecated
  public void pauseRequest(String id, Optional<String> user) {
    pauseSingularityRequest(id, user);
  }
  
  public void bounceSingularityRequest(String requestId) {
    final String requestUri = String.format(REQUEST_BOUNCE_FORMAT, getHost(), contextPath, requestId);

    LOG.info(String.format("Bouncing singularity request with id: '%s' - (POST %s)", requestId, requestUri));

    final long start = System.currentTimeMillis();

    Response response = postUri(requestUri);

    checkResponse("bounce", response);

    LOG.info(String.format("Successfully bounced singularity request with id: '%s' in %sms", requestId, System.currentTimeMillis() - start));
  }
  
  /**
   * use instead {@link SingularityClient#bounceSingularityRequest(String)}
   */
  @Deprecated
  public void bounce(String requestId) {
    bounceSingularityRequest(requestId);
  }
  
  //
  // ACTIONS ON A DEPLOY FOR A SINGULARITY REQUEST
  //
  
  public SingularityRequestParent createDeployForSingularityRequest(String requestId, SingularityDeploy pendingDeploy, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_CREATE_DEPLOY_FORMAT, getHost(), contextPath, requestId), user);

    LOG.info(String.format("Creating a new deploy instance in singularity request with id: '%s' - (POST %s)", requestId, requestUri));

    final long start = System.currentTimeMillis();

    Response response = postUri(requestUri, pendingDeploy);

    checkResponse("create deploy for singularity request", response);

    LOG.info(String.format("Successfully created new deploy for singularity request with id: '%s', in %sms", requestId, System.currentTimeMillis() - start));
    
    try {
      SingularityRequestParent singularityRequestParent = objectMapper.readValue(response.getResponseBodyAsStream(), SingularityRequestParent.class);
      
      String activeDeployId = (singularityRequestParent.getActiveDeploy().isPresent())? singularityRequestParent.getActiveDeploy().get().getId() : "No Active Deploy yet";
      String pendingDeployId = (singularityRequestParent.getPendingDeploy().isPresent())? singularityRequestParent.getPendingDeploy().get().getId() : "No Pending deploy (deploys for Scheduled requests become instantly active)";
      LOG.info(String.format("The status for the new deploy is the following: Singularity request id: '%s' -> pending deploy id: '%s', active deploy id: '%s'", 
          requestId, pendingDeployId, activeDeployId));
      
      return singularityRequestParent;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  public SingularityRequestParent cancelPendingDeployForSingularityRequest(String requestId, String deployId, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_DELETE_DEPLOY_FORMAT, getHost(), contextPath, requestId, deployId), user);

    LOG.info(String.format("Canceling pending deploy with id: '%s' for singularity request with id: '%s' - (DELETE %s)", deployId, requestId, requestUri));

    final long start = System.currentTimeMillis();

    Response response = deleteUri(requestUri);

    checkResponse("cancel pending deploy", response);

    LOG.info(String.format("Successfully canceled pending deploy with id: '%s' for singularity request with id: '%s', in %sms", deployId, requestId, System.currentTimeMillis() - start));
    
    try {
      SingularityRequestParent singularityRequestParent = objectMapper.readValue(response.getResponseBodyAsStream(), SingularityRequestParent.class);
      
      String activeDeployId = (singularityRequestParent.getActiveDeploy().isPresent())? singularityRequestParent.getActiveDeploy().get().getId() : "No Active Deploy";
      String pendingDeployId = (singularityRequestParent.getPendingDeploy().isPresent())? singularityRequestParent.getPendingDeploy().get().getId() : "No Pending deploy";
      LOG.info(String.format("The status for the canceled deploy is the following: Singularity request id: '%s' -> pending deploy id: '%s', active deploy id: '%s'", 
          requestId, pendingDeployId, activeDeployId));
      
      return singularityRequestParent;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  //
  // SINGULARITY REQUEST COLLECTIONS
  //
  
  //
  // ACTIVE REQUESTS
  //
  public Collection<SingularityRequest> getActiveSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_ACTIVE_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Getting active requests - (%s)", requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    checkResponse("get active requests", getResponse);

    LOG.info(String.format("Successfully got active requests from Singularity in %sms", System.currentTimeMillis() - start));

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), REQUESTS_COLLECTION);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  /**
   * use instead {@link SingularityClient#getActiveSingularityRequests()}
   */
  @Deprecated
  public Collection<SingularityRequest> getActiveRequests() {
    return getActiveSingularityRequests();
  }

  //
  // PAUSED REQUESTS
  //
  public Collection<SingularityRequest> getPausedSingularityRequests() {
    final String requestUri = String.format(REQUESTS_GET_PAUSED_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Getting paused requests - (%s)", requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    checkResponse("get paused requests", getResponse);

    LOG.info(String.format("Successfully got paused requests from Singularity in %sms", System.currentTimeMillis() - start));

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), REQUESTS_COLLECTION);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
  /**
   * use instead {@link SingularityClient#getPausedSingularityRequests()}
   */
  @Deprecated
  public Collection<SingularityRequest> getPausedRequests() {
    return getPausedSingularityRequests();
  }
  
  //
  // SINGULARITY TASK COLLECTIONS
  //
  
  //
  // ACTIVE TASKS
  //

  public Collection<SingularityTask> getActiveTasks() {
    final String requestUri = String.format(TASKS_GET_ACTIVE_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Getting active tasks - (%s)", requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    checkResponse("get active tasks", getResponse);

    LOG.info(String.format("Successfully got active tasks from Singularity in %sms", System.currentTimeMillis() - start));

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), TASKS_COLLECTION);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // SCHEDULED TASKS
  //

  public Collection<SingularityTask> getScheduledTasks() {
    final String requestUri = String.format(TASKS_GET_SCHEDULED_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Getting active tasks - (%s)", requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    checkResponse("get active tasks", getResponse);

    LOG.info(String.format("Successfully got active tasks from Singularity in %sms", System.currentTimeMillis() - start));

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), TASKS_COLLECTION);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // TASK HISTORY
  //

  public Optional<SingularityTaskHistory> getHistoryForTask(String taskId) {
    final String requestUri = String.format(TASK_HISTORY_FORMAT, getHost(), contextPath, taskId);

    LOG.info(String.format("Getting task history for %s - (%s)", taskId, requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    LOG.info(String.format("Got task history from Singularity in %sms", System.currentTimeMillis() - start));

    if (getResponse.getStatusCode() == 404) {
      return Optional.absent();
    }

    try {
      return Optional.of(objectMapper.readValue(getResponse.getResponseBodyAsStream(), SingularityTaskHistory.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<SingularityTaskIdHistory> getActiveTaskHistoryForRequest(String requestId) {
    final String requestUri = String.format(REQUEST_ACTIVE_TASKS_HISTORY_FORMAT, getHost(), contextPath, requestId);

    LOG.info(String.format("Getting active task history for request %s - (%s)", requestId, requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    LOG.info(String.format("Got active task history from Singularity in %sms", System.currentTimeMillis() - start));

    checkResponse("get active task history", getResponse);

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), TASKID_HISTORY_COLLECTION);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // WEBHOOKS
  //

  public void addWebhook(String url) {
    final String requestUri = String.format(WEBHOOK_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Adding webhook %s - (%s)", url, requestUri));

    final long start = System.currentTimeMillis();

    Response postResponse = postUri(requestUri, Arrays.asList(url));

    checkResponse("add webhook", postResponse);

    LOG.info(String.format("Successfully added webhook to Singularity in %sms", System.currentTimeMillis() - start));
  }
}
