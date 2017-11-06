package com.hubspot.singularity.auth.authenticator;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.ContainerRequestContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HttpHeaders;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.WebhookAuthConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

@Singleton
public class SingularityWebhookAuthenticator implements SingularityAuthenticator {
  private final AsyncHttpClient asyncHttpClient;
  private final WebhookAuthConfiguration webhookAuthConfiguration;
  private final ObjectMapper objectMapper;
  private final Cache<String, SingularityUserPermissionsResponse> permissionsCache;

  @Inject
  public SingularityWebhookAuthenticator(AsyncHttpClient asyncHttpClient,
                                         SingularityConfiguration configuration,
                                         ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.webhookAuthConfiguration = configuration.getWebhookAuthConfiguration();
    this.objectMapper = objectMapper;
    this.permissionsCache = CacheBuilder.<String, SingularityUserPermissionsResponse>newBuilder()
        .expireAfterWrite(webhookAuthConfiguration.getCacheValidationMs(), TimeUnit.MILLISECONDS)
        .build();
  }

  @Override
  public Optional<SingularityUser> getUser(ContainerRequestContext context) {
    String authHeaderValue = extractAuthHeader(context);

    SingularityUserPermissionsResponse permissionsResponse = verify(authHeaderValue);

    return permissionsResponse.getUser();
  }

  private String extractAuthHeader(ContainerRequestContext context) {
    final String authHeaderValue = context.getHeaderString(HttpHeaders.AUTHORIZATION);

    if (Strings.isNullOrEmpty(authHeaderValue)) {
      throw WebExceptions.unauthorized("(Webhook) No Authorization header present, please log in first");
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
          throw WebExceptions.unauthorized(String.format("(Webhook) Got status code %d when verifying jwt", response.getStatusCode()));
        } else {
          String responseBody = response.getResponseBody();
          SingularityUserPermissionsResponse permissionsResponse = objectMapper.readValue(responseBody, SingularityUserPermissionsResponse.class);
          if (!permissionsResponse.getUser().isPresent()) {
            throw WebExceptions.unauthorized(String.format("(Webhook) No user present in response %s", permissionsResponse));
          }
          if (!permissionsResponse.getUser().get().isAuthenticated()) {
            throw WebExceptions.unauthorized(String.format("(Webhook) User not authenticated (response: %s)", permissionsResponse));
          }
          permissionsCache.put(authHeaderValue, permissionsResponse);
          return permissionsResponse;
        }
      } catch (IOException|ExecutionException|InterruptedException e) {
        throw WebExceptions.unauthorized(String.format("(Webhook) Exception while verifying token: %s", e.getMessage()));
      }
    }
  }
}
