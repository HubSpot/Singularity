package com.hubspot.singularity;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.curator.framework.recipes.leader.LeaderLatch;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import com.sun.jersey.spi.container.ResourceFilter;
import com.sun.jersey.spi.container.ResourceFilterFactory;

public class LeaderRedirectorFilterFactory implements ResourceFilterFactory {
  private final LeaderLatch leaderLatch;
  private final Boolean highAvailability;

  @Inject
  public LeaderRedirectorFilterFactory(LeaderLatch leaderLatch, @Named(SingularityModule.HA_PROPERTY) Boolean highAvailability) {
    this.leaderLatch = leaderLatch;
    this.highAvailability = highAvailability;
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    if (highAvailability) {
      return Collections.<ResourceFilter>singletonList(new Filter());
    } else {
      return null;
    }
  }

  private class Filter implements ResourceFilter, ContainerRequestFilter {
    protected Filter() { }

    @Override
    public ContainerRequestFilter getRequestFilter() {
      return this;
    }

    @Override
    public ContainerResponseFilter getResponseFilter() {
      return null;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
      if (!leaderLatch.hasLeadership()) {
        try {
          final String leader = leaderLatch.getLeader().getId();
          final String path = request.getAbsolutePath().getPath();
          throw new WebApplicationException(Response.temporaryRedirect(new URI(String.format("http://%s%s", leader, path))).build());
        } catch (WebApplicationException e) {
          throw e;  // OMFG this is stupid
        } catch (Exception e) {
          // TODO: should we fail the request? in what situations will this happen?
          // for now, just let the request go through.
          return request;
        }
      }

      return request;
    }
  }
}
