package com.hubspot.singularity.auth.authenticator;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.JWTConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

@Singleton
public class SingularityExternalJWTAuthenticator implements SingularityAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityExternalJWTAuthenticator.class);

  private final AsyncHttpClient asyncHttpClient;
  private final JWTConfiguration jwtConfiguration;
  private final Provider<HttpServletRequest> requestProvider;
  private final ObjectMapper objectMapper;
  private final Cache<String, UserPermissions> permissionsCache;

  @Inject
  public SingularityExternalJWTAuthenticator(AsyncHttpClient asyncHttpClient,
                                             SingularityConfiguration configuration,
                                             Provider<HttpServletRequest> requestProvider,
                                             ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.jwtConfiguration = configuration.getJwtConfiguration();
    this.requestProvider = requestProvider;
    this.objectMapper = objectMapper;
    this.permissionsCache = CacheBuilder.<String, UserPermissions>newBuilder()
        .expireAfterWrite(jwtConfiguration.getCacheValidationMs(), TimeUnit.MILLISECONDS)
        .build();
  }

  @Override
  public Optional<SingularityUser> get() {
    String jwt = extractJWT(requestProvider.get());

    UserPermissions permissions = verifyToken(jwt);

    Set<String> groups = new HashSet<>();
    groups.addAll(permissions.getGroups());
    groups.addAll(permissions.getScopes());
    return Optional.of(new SingularityUser(permissions.getUid(), Optional.of(permissions.getUid()), Optional.of(permissions.getUid()), groups));
  }

  private String extractJWT(HttpServletRequest request) {
    String cookieValue = null;
    for (Cookie cookie : request.getCookies()) {
      if (cookie.getName().equals(jwtConfiguration.getAuthCookieName())) {
        cookieValue = cookie.getValue();
        break;
      }
    }

    if (Strings.isNullOrEmpty(cookieValue)) {
      throw WebExceptions.unauthorized(String.format("No %s cookie present, please log in first", jwtConfiguration.getAuthCookieName()));
    } else {
      return cookieValue;
    }
  }

  private UserPermissions verifyToken(String jwt) {
    UserPermissions maybeCachedPermissions = permissionsCache.getIfPresent(jwt);
    if (maybeCachedPermissions != null) {
      return maybeCachedPermissions;
    } else {
      try {
        Response response = asyncHttpClient.prepareGet(jwtConfiguration.getAuthVerificationUrl())
            .addHeader("Authorization", String.format("Bearer: %s", jwt))
            .execute()
            .get();
        if (response.getStatusCode() > 299) {
          throw WebExceptions.unauthorized(String.format("Got status code %d when verifying jwt", response.getStatusCode()));
        } else {
          String responseBody = response.getResponseBody();
          UserPermissions permissions = objectMapper.readValue(responseBody, UserPermissions.class);
          permissionsCache.put(jwt, permissions);
          return permissions;
        }
      } catch (IOException|ExecutionException|InterruptedException e) {
        LOG.error("Exception while verifying jwt", e);
        throw WebExceptions.unauthorized(String.format("Exception while verifying jwt: %s", e.getMessage()));
      }
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private class UserPermissions {
    private final String uid;
    private final Set<String> groups;
    private final Set<String> scopes;

    @JsonCreator
    public UserPermissions(@JsonProperty("uid") String uid, @JsonProperty("groups") Set<String> groups, @JsonProperty("scopes") Set<String> scopes) {
      this.uid = uid;
      this.groups = groups;
      this.scopes = scopes;
    }

    public String getUid() {
      return uid;
    }

    public Set<String> getGroups() {
      return groups;
    }

    public Set<String> getScopes() {
      return scopes;
    }

    @Override
    public String toString() {
      return "UserPermissions{" +
          "uid='" + uid + '\'' +
          ", groups=" + groups +
          ", scopes=" + scopes +
          '}';
    }
  }
}
