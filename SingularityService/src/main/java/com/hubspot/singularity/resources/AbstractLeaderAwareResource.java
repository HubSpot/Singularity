package com.hubspot.singularity.resources;

import javax.servlet.http.HttpServletRequest;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.singularity.WebExceptions;

public class AbstractLeaderAwareResource {
  protected final HttpClient httpClient;
  protected final LeaderLatch leaderLatch;

  public AbstractLeaderAwareResource(HttpClient httpClient, LeaderLatch leaderLatch) {
    this.httpClient = httpClient;
    this.leaderLatch = leaderLatch;
  }

  protected <T> T proxyToLeader(HttpServletRequest request, Class<T> clazz) {
    String leaderUri;
    try {
      leaderUri = leaderLatch.getLeader().getId();
    } catch (Exception e) {
      throw new RuntimeException("Could not get leader uri to proxy request");
    }
    HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
        .setUrl(String.format("%s%s", leaderUri, request.getServletPath()))
        .setMethod(Method.valueOf(request.getMethod()));
    copyHeadersAndParams(requestBuilder, request);
    HttpResponse response = httpClient.execute(requestBuilder.build());
    if (response.isServerError()) {
      throw WebExceptions.badRequest(response.getAsString());
    } else if (response.isServerError()) {
      throw new RuntimeException(response.getAsString());
    } else {
      return response.getAs(clazz);
    }
  }

  private void copyHeadersAndParams(HttpRequest.Builder requestBuilder, HttpServletRequest request) {
    while (request.getHeaderNames().hasMoreElements()) {
      String headerName = request.getHeaderNames().nextElement();
      requestBuilder.addHeader(headerName, request.getHeader(headerName));
    }
    while (request.getParameterNames().hasMoreElements()) {
      String parameterName = request.getParameterNames().nextElement();
      requestBuilder.setQueryParam(parameterName).to(request.getParameter(parameterName));
    }
  }
}
