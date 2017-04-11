package com.hubspot.singularity.resources;

import java.io.IOException;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractLeaderAwareResource.class);

  protected final AsyncHttpClient httpClient;
  protected final LeaderLatch leaderLatch;
  protected final ObjectMapper objectMapper;

  public AbstractLeaderAwareResource(AsyncHttpClient httpClient, LeaderLatch leaderLatch, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.leaderLatch = leaderLatch;
    this.objectMapper = objectMapper;
  }

  protected <T, Q> T maybeProxyToLeader(HttpServletRequest request, Class<T> clazz, Q body, Supplier<T> runnable) {
    if (leaderLatch.hasLeadership()) {
      return runnable.get();
    }

    String leaderUri;
    try {
      leaderUri = leaderLatch.getLeader().getId();
    } catch (Exception e) {
      throw new RuntimeException("Could not get leader uri to proxy request");
    }

    if (leaderUri.equals(leaderLatch.getId())) {
      LOG.warn("Got own leader id when not the leader! There is likely no leader, will not proxy");
      return runnable.get();
    }

    String url = "http://" + leaderUri + request.getContextPath() + request.getPathInfo();
    LOG.debug("Not the leader, proxying request to {}", url);

    BoundRequestBuilder requestBuilder;
    switch (request.getMethod().toUpperCase()) {
      case "POST":
        requestBuilder = httpClient.preparePost(url);
        break;
      case "PUT":
        requestBuilder = httpClient.preparePut(url);
        break;
      case "DELETE":
        requestBuilder = httpClient.prepareDelete(url);
        break;
      default:
        throw new WebApplicationException(String.format("Not meant to proxy request of method %s", request.getMethod()), 400);
    }

    try {
      if (body != null) {
        requestBuilder.setBody(objectMapper.writeValueAsBytes(body));
        LOG.trace("Added body {} to reqeust", body);
      }
    } catch (JsonProcessingException jpe) {
      LOG.error("Could not write body from object {}", body);
      throw new WebApplicationException(jpe, 500);
    }

    copyHeadersAndParams(requestBuilder, request);
    Request httpRequest = requestBuilder.build();

    Response response;
    try {
      LOG.trace("Sending request to leader: {}", httpRequest);
      response = httpClient.executeRequest(httpRequest).get();
    } catch (IOException|ExecutionException|InterruptedException e) {
      LOG.error("Could not proxy request {} to leader", e);
      throw new WebApplicationException(e, 500);
    }

    try {
      if (response.getStatusCode() > 399) {
        throw new WebApplicationException(response.getResponseBody(Charsets.UTF_8.toString()), response.getStatusCode());
      } else {
        return objectMapper.readValue(response.getResponseBodyAsStream(), clazz);
      }
    } catch (IOException ioe) {
      String message = String.format("Request to leader succeeded with status %s, but could not interpret response", response.getStatusCode());
      LOG.error(message, ioe);
      throw new WebApplicationException(message, ioe, 500);
    }
  }

  private void copyHeadersAndParams(BoundRequestBuilder requestBuilder, HttpServletRequest request) {
    Enumeration<String> headerNames = request.getHeaderNames();
    if (headerNames != null) {
      while (headerNames.hasMoreElements()) {
        String headerName = headerNames.nextElement();
        requestBuilder.addHeader(headerName, request.getHeader(headerName));
        LOG.trace("Copied header {}:{}", headerName, request.getHeader(headerName));
      }
    }
    Enumeration<String> parameterNames = request.getParameterNames();
    if (parameterNames != null) {
      while (parameterNames.hasMoreElements()) {
        String parameterName = parameterNames.nextElement();
        requestBuilder.addQueryParameter(parameterName, request.getParameter(parameterName));
        LOG.trace("Copied query param {}={}", parameterName, request.getParameter(parameterName));
      }
    }
  }

  protected boolean useWebCache(Boolean useWebCache) {
    return useWebCache != null && useWebCache;
  }
}
