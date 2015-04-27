package com.hubspot.singularity.ldap;

import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityAuthManager {
  private final SingularityConfiguration configuration;
  private final HttpHeaders headers;
  private final Optional<String> queryUser;

  @Inject
  public SingularityAuthManager(SingularityConfiguration configuration, @Context HttpHeaders headers, @Context UriInfo uriInfo) {
    this.configuration = configuration;
    this.headers = headers;
    this.queryUser = Optional.fromNullable(Strings.emptyToNull(uriInfo.getQueryParameters(true).getFirst("user")));
  }

  public Optional<String> getUser() {
    final List<String> headerValues = headers.getRequestHeader(configuration.getLdapConfiguration().getRequestUserHeaderName());

    if (headerValues.isEmpty()) {
      return queryUser;
    }

    return Optional.fromNullable(Strings.emptyToNull(headerValues.get(0))).or(queryUser);
  }
}
