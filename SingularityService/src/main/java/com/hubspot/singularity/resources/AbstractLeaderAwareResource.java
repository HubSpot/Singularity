package com.hubspot.singularity.resources;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;

public class AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractLeaderAwareResource.class);

  protected final HttpClient httpClient;
  protected final LeaderLatch leaderLatch;
  protected final ObjectMapper objectMapper;

  public AbstractLeaderAwareResource(HttpClient httpClient, LeaderLatch leaderLatch, ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.leaderLatch = leaderLatch;
    this.objectMapper = objectMapper;
  }

  protected <T, Q> T proxyToLeader(HttpServletRequest request, Class<T> clazz, Q body) {
    String leaderUri;
    try {
      leaderUri = leaderLatch.getLeader().getId();
    } catch (Exception e) {
      throw new RuntimeException("Could not get leader uri to proxy request");
    }

    String url = "http://" + leaderUri + request.getContextPath() + request.getPathInfo();
    LOG.debug("Not the leader, proxying request to {}", url);
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(url)
        .setMethod(Method.valueOf(request.getMethod()));

    try {
      if (body != null) {
        requestBuilder.setBody(objectMapper.writeValueAsBytes(body));
        LOG.trace("Added body {} to reqeust", body);
      }
    } catch (JsonProcessingException jpe) {
      LOG.error("Could not write body from object {}", body);
    }

    copyHeadersAndParams(requestBuilder, request);
    HttpRequest httpRequest = requestBuilder.build();
    LOG.trace("Sending request to leader: {}", httpRequest);
    HttpResponse response = httpClient.execute(httpRequest);
    if (response.isError()) {
      throw new WebApplicationException(response.getAsString(), response.getStatusCode());
    } else {
      return response.getAs(clazz);
    }
  }

  private void copyHeadersAndParams(HttpRequest.Builder requestBuilder, HttpServletRequest request) {
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
        requestBuilder.setQueryParam(parameterName).to(request.getParameter(parameterName));
        LOG.trace("Copied query param {}={}", parameterName, request.getParameter(parameterName));
      }
    }
  }

  protected boolean useWebCache(Boolean useWebCache) {
    return useWebCache != null && useWebCache.booleanValue();
  }
}
