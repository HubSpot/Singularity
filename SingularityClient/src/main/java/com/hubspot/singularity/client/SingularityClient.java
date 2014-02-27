package com.hubspot.singularity.client;

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
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

public class SingularityClient {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityClient.class);

  private static final String WEBHOOK_FORMAT = "http://%s/%s/webhooks";

  private static final String TASK_FORMAT = "http://%s/%s/tasks";
  private static final String TASK_ACTIVE_FORMAT = TASK_FORMAT + "/active";
  private static final String TASK_SCHEDULED_FORMAT = TASK_FORMAT + "/scheduled";

  private static final String HISTORY_FORMAT = "http://%s/%s/history";
  private static final String TASK_HISTORY_FORMAT = HISTORY_FORMAT + "/task/%s";
  private static final String REQUEST_ACTIVE_TASKS_HISTORY_FORMAT = HISTORY_FORMAT + "/request/%s/tasks/active";

  private static final String REQUEST_FORMAT = "http://%s/%s/requests";
  private static final String REQUEST_BOUNCE_FORMAT = REQUEST_FORMAT + "/request/%s/bounce";
  private static final String REQUEST_ACTIVE_UNDEPLOY_FORMAT = REQUEST_FORMAT + "/request/%s";
  private static final String REQUEST_ACTIVE_FORMAT = REQUEST_FORMAT + "/active";
  private static final String REQUEST_PAUSED_FORMAT = REQUEST_FORMAT + "/paused";
  private static final String REQUEST_PAUSED_UNDEPLOY_FORMAT = REQUEST_FORMAT + "/request/%s/paused";
  private static final String REQUEST_ADD_USER_FORMAT = "%s?user=%s";
  
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
    
    return String.format(REQUEST_ADD_USER_FORMAT, uri, user.get());
  }

  //
  // DEPLOY
  //
  public void deploy(SingularityRequest request, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_FORMAT, getHost(), contextPath), user);

    LOG.info(String.format("Deploying %s to (%s)", request.getId(), requestUri));

    final long start = System.currentTimeMillis();

    Response response = postUri(requestUri, request);

    checkResponse("deploy", response);

    LOG.info(String.format("Successfully deployed %s to Singularity in %sms", request.getId(), System.currentTimeMillis() - start));
  }

  public void bounce(String requestId) {
    final String requestUri = String.format(REQUEST_BOUNCE_FORMAT, getHost(), contextPath, requestId);

    LOG.info(String.format("Bouncing %s", requestId));

    final long start = System.currentTimeMillis();

    Response response = postUri(requestUri);

    checkResponse("bounce", response);

    LOG.info(String.format("Successfully bounced %s in %sms", requestId, System.currentTimeMillis() - start));
  }

  //
  // ACTIVE REQUESTS
  //
  public Collection<SingularityRequest> getActiveRequests() {
    final String requestUri = String.format(REQUEST_ACTIVE_FORMAT, getHost(), contextPath);

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
  
  public Optional<SingularityRequest> removeActiveRequest(String id, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_ACTIVE_UNDEPLOY_FORMAT, getHost(), contextPath, id), user);

    LOG.info(String.format("Removing active request ID %s - (%s)", id, requestUri));
  
    final long start = System.currentTimeMillis();
    
    Response deleteResponse = deleteUri(requestUri);

    if (deleteResponse.getStatusCode() == 404) {
      return Optional.absent();
    }
    
    checkResponse("remove active request", deleteResponse);
    
    LOG.info(String.format("Successfully removed active request ID %s from Singularity in %sms", id, System.currentTimeMillis() - start));

    try {
      return Optional.of(objectMapper.readValue(deleteResponse.getResponseBodyAsStream(), SingularityRequest.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // PAUSED REQUESTS
  //
  public Collection<SingularityRequest> getPausedRequests() {
    final String requestUri = String.format(REQUEST_PAUSED_FORMAT, getHost(), contextPath);

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

  public Optional<SingularityRequest> removePausedRequest(String id, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_PAUSED_UNDEPLOY_FORMAT, getHost(), contextPath, id), user);

    LOG.info(String.format("Removing paused request ID %s - (%s)", id, requestUri));

    final long start = System.currentTimeMillis();

    Response deleteResponse = deleteUri(requestUri);

    if (deleteResponse.getStatusCode() == 404) {
      return Optional.absent();
    }

    checkResponse("remove paused request", deleteResponse);

    LOG.info(String.format("Successfully removed paused request ID %s from Singularity in %sms", id, System.currentTimeMillis() - start));

    try {
      return Optional.of(objectMapper.readValue(deleteResponse.getResponseBodyAsStream(), SingularityRequest.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  //
  // ACTIVE TASKS
  //

  public Collection<SingularityTask> getActiveTasks() {
    final String requestUri = String.format(TASK_ACTIVE_FORMAT, getHost(), contextPath);

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
    final String requestUri = String.format(TASK_SCHEDULED_FORMAT, getHost(), contextPath);

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
