package com.hubspot.singularity.auth.authenticator;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.WebhookAuthConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

@Singleton
public class SingularityWebhookAuthenticator implements SingularityAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityWebhookAuthenticator.class);

  private final AsyncHttpClient asyncHttpClient;
  private final WebhookAuthConfiguration webhookAuthConfiguration;
  private final Provider<HttpServletRequest> requestProvider;
  private final ObjectMapper objectMapper;
  private final Cache<String, SingularityUserPermissionsResponse> permissionsCache;

  @Inject
  public SingularityWebhookAuthenticator(AsyncHttpClient asyncHttpClient,
                                         SingularityConfiguration configuration,
                                         Provider<HttpServletRequest> requestProvider,
                                         ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.webhookAuthConfiguration = configuration.getWebhookAuthConfiguration();
    this.requestProvider = requestProvider;
    this.objectMapper = objectMapper;
    this.permissionsCache = CacheBuilder.<String, SingularityUserPermissionsResponse>newBuilder()
        .expireAfterWrite(webhookAuthConfiguration.getCacheValidationMs(), TimeUnit.MILLISECONDS)
        .build();
  }

  @Override
  public Optional<SingularityUser> get() {
    String authHeaderValue = extractAuthHeader(requestProvider.get());

    SingularityUserPermissionsResponse permissionsResponse = verify(authHeaderValue);
    LOG.trace("Verified permissions for user {}", permissionsResponse);

    return Optional.of(permissionsResponse.getUser());
  }

  private String extractAuthHeader(HttpServletRequest request) {
    String authHeaderValue = request.getHeader("Authorization");

    if (Strings.isNullOrEmpty(authHeaderValue)) {
      throw WebExceptions.unauthorized("No Authorization header present, please log in first");
    } else {
      return authHeaderValue;
    }
  }

  private SingularityUserPermissionsResponse verify(String authHeaderValue) {
    SingularityUserPermissionsResponse maybeCachedPermissions = permissionsCache.getIfPresent(authHeaderValue);
    if (maybeCachedPermissions != null) {
      return maybeCachedPermissions;
    } else {
      try {
        Response response = asyncHttpClient.prepareGet(webhookAuthConfiguration.getAuthVerificationUrl())
            .addHeader("Authorization", authHeaderValue)
            .execute()
            .get();
        if (response.getStatusCode() > 299) {
          throw WebExceptions.unauthorized(String.format("Got status code %d when verifying jwt", response.getStatusCode()));
        } else {
          String responseBody = response.getResponseBody();
          SingularityUserPermissionsResponse permissionsResponse = objectMapper.readValue(responseBody, SingularityUserPermissionsResponse.class);
          if (!permissionsResponse.isAuthenticated()) {
            throw WebExceptions.unauthorized(String.format("User %s not authenticated (error: %s)", permissionsResponse.getUser().getId(), permissionsResponse.getError()));
          }
          permissionsCache.put(authHeaderValue, permissionsResponse);
          return permissionsResponse;
        }
      } catch (IOException|ExecutionException|InterruptedException e) {
        LOG.error("Exception while verifying jwt", e);
        throw WebExceptions.unauthorized(String.format("Exception while verifying jwt: %s", e.getMessage()));
      }
    }
  }
}
