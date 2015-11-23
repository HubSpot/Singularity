package com.hubspot.singularity.jersey;

import com.google.inject.Inject;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

public class RequestStashFilter implements ContainerRequestFilter, ContainerResponseFilter {
  @Inject
  public RequestStashFilter() {
  }

  @Override
  public ContainerRequest filter(ContainerRequest request) {
    RequestStash.INSTANCE.setUrl(request.getPath());

    return request;
  }

  @Override
  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
    RequestStash.INSTANCE.clearUrl();

    return response;
  }
}
