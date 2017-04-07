package com.hubspot.singularity.resources;

import java.util.Enumeration;
import java.util.function.Supplier;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.horizon.RetryStrategy;
import com.hubspot.horizon.ning.NingHttpClient;

public class AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractLeaderAwareResource.class);

  protected final NingHttpClient httpClient;
  protected final LeaderLatch leaderLatch;
  protected final ObjectMapper objectMapper;

  public AbstractLeaderAwareResource(NingHttpClient httpClient, LeaderLatch leaderLatch, ObjectMapper objectMapper) {
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
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(url)
        .setRetryStrategy(RetryStrategy.NEVER_RETRY)
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
    if (response.isServerError() || response.isClientError()) {
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
    return useWebCache != null && useWebCache;
  }
}
