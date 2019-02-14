package com.hubspot.singularity.auth.authenticator;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.AuthTokenManager;

@Singleton
public class SingularityTokenAuthenticator implements SingularityAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityTokenAuthenticator.class);

  private final AuthTokenManager authTokenManager;
  private final LoadingCache<String, SingularityUser> tokenCache;

  @Inject
  public SingularityTokenAuthenticator(AuthTokenManager authTokenManager) {
    this.authTokenManager = authTokenManager;
    this.tokenCache = CacheBuilder.<String, SingularityUserPermissionsResponse>newBuilder()
        .expireAfterWrite(15, TimeUnit.SECONDS)
        .build(new CacheLoader<String, SingularityUser>() {
          @Override
          public SingularityUser load(String token) {
            return verifyUncached(token);
          }
        });
  }

  @Override
  public java.util.Optional<SingularityUser> getUser(ContainerRequestContext context) {
    String token = extractToken(context);
    try {
      return java.util.Optional.of(tokenCache.get(token));
    } catch (Throwable t) {
      throw WebExceptions.unauthorized(String.format("Exception while verifying token: %s", t.getMessage()));
    }
  }

  private SingularityUser verifyUncached(String token) {
    SingularityUser verified = authTokenManager.getUserIfValidToken(token);

    if (verified == null) {
      throw WebExceptions.unauthorized("No matching token found");
    } else {
      return verified;
    }
  }

  private String extractToken(ContainerRequestContext context) {
    final String authHeaderValue = context.getHeaderString(HttpHeaders.AUTHORIZATION);
    if (Strings.isNullOrEmpty(authHeaderValue)) {
      throw WebExceptions.unauthorized("No Authorization header present, please log in first");
    } else {
      if (!authHeaderValue.startsWith("Token")) {
        throw WebExceptions.unauthorized("No Token specified in authorization header");
      }
      return authHeaderValue.replaceFirst("Token\\s+", "");
    }
  }
}
