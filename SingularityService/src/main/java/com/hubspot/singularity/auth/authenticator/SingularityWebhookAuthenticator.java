package com.hubspot.singularity.auth.authenticator;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.container.ContainerRequestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.net.HttpHeaders;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityAuthModule;
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
  private final ObjectMapper objectMapper;
  private final LoadingCache<String, SingularityUserPermissionsResponse> permissionsCache;

  @Inject
  public SingularityWebhookAuthenticator(@Named(SingularityAuthModule.WEBHOOK_AUTH_HTTP_CLIENT) AsyncHttpClient asyncHttpClient,
                                         SingularityConfiguration configuration,
                                         ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.webhookAuthConfiguration = configuration.getWebhookAuthConfiguration();
    this.objectMapper = objectMapper;
    this.permissionsCache = CacheBuilder.<String, SingularityUserPermissionsResponse>newBuilder()
        .refreshAfterWrite(webhookAuthConfiguration.getCacheValidationMs(), TimeUnit.MILLISECONDS)
        .build(new CacheLoader<String, SingularityUserPermissionsResponse>() {
          @Override
          public SingularityUserPermissionsResponse load(String authHeaderValue) throws Exception {
            return verifyUncached(authHeaderValue);
          }

          @Override
          public ListenableFuture<SingularityUserPermissionsResponse> reload(String authHeaderVaule, SingularityUserPermissionsResponse oldVaule) {
            return ListenableFutureTask.create(() -> {
              try {
                return verifyUncached(authHeaderVaule);
              } catch (Throwable t) {
                LOG.warn("Unable to refresh user information", t);
                return oldVaule;
              }
            });
          }
        });
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
      throw WebExceptions.unauthorized("No Authorization header present, please log in first");
    } else {
      return authHeaderValue;
    }
  }

  private SingularityUserPermissionsResponse verify(String authHeaderValue) {
    try {
      return permissionsCache.get(authHeaderValue);
    } catch (Throwable t) {
      throw WebExceptions.unauthorized(String.format("Exception while verifying token: %s", t.getMessage()));
    }
  }

  private SingularityUserPermissionsResponse verifyUncached(String authHeaderValue) {
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
        if (!permissionsResponse.getUser().isPresent()) {
          throw WebExceptions.unauthorized(String.format("No user present in response %s", permissionsResponse));
        }
        if (!permissionsResponse.getUser().get().isAuthenticated()) {
          throw WebExceptions.unauthorized(String.format("User not authenticated (response: %s)", permissionsResponse));
        }
        permissionsCache.put(authHeaderValue, permissionsResponse);
        return permissionsResponse;
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
