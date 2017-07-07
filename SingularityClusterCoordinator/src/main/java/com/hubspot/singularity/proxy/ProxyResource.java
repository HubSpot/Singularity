package com.hubspot.singularity.proxy;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Request;
import com.ning.http.client.Response;

public class ProxyResource {
  private static final Logger LOG = LoggerFactory.getLogger(ProxyResource.class);

  private final AsyncHttpClient httpClient;
  private final ClusterCoordinatorConfiguration configuration;
  private final ObjectMapper objectMapper;

  @Inject
  public ProxyResource(ClusterCoordinatorConfiguration configuration, AsyncHttpClient httpClient, ObjectMapper objectMapper) {
    this.configuration = configuration;
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
  }

  /*
   * For items which need to be collected from each configured dataCenter.
   * Fan out requests to each and collect them as a map of dataCenter -> result
   */
  public <T> Map<String, T> getNamespacedResult(HttpServletRequest requestContext, TypeReference<T> clazz) {
    return null;
  }

  /*
   * For items where the dataCenter is part of the object, or where a full list is desired.
   * Collect and merge results from each configured dataCenter
   */
  public <T> List<T> getMergedResult(HttpServletRequest requestContext, TypeReference<T> clazz) {
    return null;
  }

  /*
   * Route a request to a particular dataCenter using the requestId to locate the correct Singularity cluster
   */
  public <T> T routeByRequestId(HttpServletRequest requestContext, String requestId, TypeReference<T> clazz) {
    return null;
  }

  /*
   * Route a request to a particular dataCenter using the slaveId to locate the correct Singularity cluster
   */
  public <T> T routeBySlaveId(HttpServletRequest requestContext, String slaveId, TypeReference<T> clazz) {
    return null;
  }

  /*
   * Route a request to a particular dataCenter by name, failing if it is not present
   */
  public <T> T routeByDataCenter(HttpServletRequest requestContext, String dataCenterName, TypeReference<T> clazz) {
    return null;
  }

  /*
   * Get from the cloest singularity instance
   */
  public <T> T getFromClosestDataCenter(HttpServletRequest requestContext, TypeReference<T> clazz) {
    return null;
  }

  public <T, Q> T proxyRequest(HttpServletRequest request,  TypeReference<T> clazz, Q body, String uri) {
    String url = "http://" + uri + request.getContextPath() + request.getPathInfo();

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
}
