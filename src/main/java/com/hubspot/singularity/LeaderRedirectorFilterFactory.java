package com.hubspot.singularity;

import com.google.inject.Inject;
import com.sun.jersey.api.model.AbstractMethod;
import com.sun.jersey.spi.container.*;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

public class LeaderRedirectorFilterFactory implements ResourceFilterFactory {
  private final LeaderLatch leaderLatch;

  @Inject
  public LeaderRedirectorFilterFactory(LeaderLatch leaderLatch) {
    this.leaderLatch = leaderLatch;
  }

  @Override
  public List<ResourceFilter> create(AbstractMethod am) {
    return Collections.<ResourceFilter>singletonList(new Filter());
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
