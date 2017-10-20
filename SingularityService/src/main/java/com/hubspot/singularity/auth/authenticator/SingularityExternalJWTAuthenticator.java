package com.hubspot.singularity.auth.authenticator;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAsyncHttpClient;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityExternalJWTAuthenticator implements SingularityAuthenticator {
  private final SingularityAsyncHttpClient asyncHttpClient;
  private final AuthConfiguration authConfiguration;
  private final Provider<HttpServletRequest> requestProvider;
  private final Cache<String, UserPermissions> permissionsCache;

  @Inject
  public SingularityExternalJWTAuthenticator(SingularityAsyncHttpClient asyncHttpClient, SingularityConfiguration configuration, Provider<HttpServletRequest> requestProvider) {
    this.asyncHttpClient = asyncHttpClient;
    this.authConfiguration = configuration.getAuthConfiguration();
    this.requestProvider = requestProvider;
    this.permissionsCache = CacheBuilder.<String, UserPermissions>newBuilder()
        .expireAfterWrite("")
        .build(new CacheLoader<String, UserPermissions>() {
          @Override
          public UserPermissions load(String key) throws Exception {
            return null;
          }
        })
  }

  @Override
  public Optional<SingularityUser> get() {
    String jwt = extractJWT(requestProvider.get());

    UserPermissions permissions = verifyToken(jwt);

    return Optional.of(new SingularityUser("", "", "", ""));
  }

  private String extractJWT(HttpServletRequest request) {
    String cookieValue = null;
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equals(authConfiguration.getJwtAuthCookieName())) {
        cookieValue = cookie.getValue();
        break;
      }
    }

    if (Strings.isNullOrEmpty(cookieValue)) {
      throw WebExceptions.unauthorized(String.format("No %s cookie present, please log in first", authConfiguration.getJwtAuthCookieName()));
    } else {
      return cookieValue;
    }
  }

  private UserPermissions verifyToken(String jwt) {
    // http request to verify, returns user permissions/email/id
  }


  private class UserPermissions {
    // mimic janus Permissions class
  }
}
