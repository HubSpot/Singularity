package com.hubspot.singularity.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityTask;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class SingularityClient {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityClient.class);

  private static final String WEBHOOK_FORMAT = "http://%s/%s/webhooks";

  private static final String TASK_FORMAT = "http://%s/%s/tasks";
  private static final String TASK_ACTIVE_FORMAT = TASK_FORMAT + "/active";

  private static final String REQUEST_FORMAT = "http://%s/%s/requests";
  private static final String REQUEST_UNDEPLOY_FORMAT = REQUEST_FORMAT + "/request/%s";
  private static final String REQUEST_ACTIVE_FORMAT = REQUEST_FORMAT + "/active";
  private static final String REQUEST_ADD_USER_FORMAT = "%s?user=%s";
  
  private static final String CONTENT_TYPE_JSON = "application/json";
  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  private static final TypeReference<Collection<SingularityRequest>> SINGULARITY_REQUESTS = new TypeReference<Collection<SingularityRequest>>() {};
  private static final TypeReference<Collection<SingularityTask>> SINGULARITY_TASKS = new TypeReference<Collection<SingularityTask>>() {};
  
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
  
  private Response deployToUri(String requestUri, SingularityRequest request) {
    try {
      return httpClient.preparePost(requestUri)
        .setBody(request.getAsBytes(objectMapper))
        .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
        .execute().get();
      
    } catch (Exception e) {
      throw new SingularityClientException("Failed to deploy to Singularity due to exception", e);
    }
  }
  
  public void deploy(SingularityRequest request, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_FORMAT, getHost(), contextPath), user);
    
    LOG.info(String.format("Deploying %s to (%s)", request.getId(), requestUri));
  
    final long start = System.currentTimeMillis();
    
    Response response = deployToUri(requestUri, request);
    
    checkResponse("deploy", response);
    
    LOG.info(String.format("Successfully deployed %s to Singularity in %sms", request.getId(), System.currentTimeMillis() - start));
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
    
    throw new SingularityClientException(String.format("Failed %s action on Singularity (%s) - code: %s, %s", uri, response.getStatusCode(), body));
  }
  
  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 || response.getStatusCode() < 300;
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

  private Response postJson(String requestUri, Object data) {
    try {
      return httpClient.preparePost(requestUri)
          .addHeader(HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON)
          .setBody(objectMapper.writeValueAsBytes(data))
          .execute().get();
    } catch (Exception e) {
      throw new SingularityClientException("Failed to delete Singularity request due to exception", e);
    }
  }
  
  private String finishUri(String uri, Optional<String> user) {
    if (!user.isPresent()) {
      return uri;
    }
    
    return String.format(REQUEST_ADD_USER_FORMAT, uri, user.get());
  }
  
  public void remove(String name, Optional<String> user) {
    final String requestUri = finishUri(String.format(REQUEST_UNDEPLOY_FORMAT, getHost(), contextPath, name), user);

    LOG.info(String.format("Removing %s - (%s)", name, requestUri));
  
    final long start = System.currentTimeMillis();
    
    Response deleteResponse = deleteUri(requestUri);
    
    checkResponse("remove", deleteResponse);
    
    LOG.info(String.format("Successfully removed %s from Singularity in %sms", name, System.currentTimeMillis() - start));
  }

  public Collection<SingularityRequest> getActiveRequests() {
    final String requestUri = String.format(REQUEST_ACTIVE_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Getting active requests - (%s)", requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    checkResponse("get active requests", getResponse);

    LOG.info(String.format("Successfully got active requests from Singularity in %sms", System.currentTimeMillis() - start));

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), SINGULARITY_REQUESTS);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<SingularityTask> getActiveTasks() {
    final String requestUri = String.format(TASK_ACTIVE_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Getting active tasks - (%s)", requestUri));

    final long start = System.currentTimeMillis();

    Response getResponse = getUri(requestUri);

    checkResponse("get active tasks", getResponse);

    LOG.info(String.format("Successfully got active tasks from Singularity in %sms", System.currentTimeMillis() - start));

    try {
      return objectMapper.readValue(getResponse.getResponseBodyAsStream(), SINGULARITY_TASKS);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void addWebhook(String url) {
    final String requestUri = String.format(WEBHOOK_FORMAT, getHost(), contextPath);

    LOG.info(String.format("Adding webhook %s - (%s)", url, requestUri));

    final long start = System.currentTimeMillis();

    Response postResponse = postJson(requestUri, Arrays.asList(url));

    checkResponse("add webhook", postResponse);

    LOG.info("Successfully added webhook to Singularity in %sms", System.currentTimeMillis() - start);
  }
}
