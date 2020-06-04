package com.hubspot.singularity.auth.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.WebExceptions;
import com.ning.http.client.Response;
import java.io.IOException;

@Singleton
public class WrappedUserResponseParser extends WebhookResponseParser {
  private final ObjectMapper objectMapper;

  @Inject
  public WrappedUserResponseParser(@Singularity ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  SingularityUserPermissionsResponse parse(Response response) throws IOException {
    if (response.getStatusCode() > 299) {
      throw WebExceptions.unauthorized(
        String.format("Got status code %d when verifying jwt", response.getStatusCode())
      );
    } else {
      String responseBody = response.getResponseBody();
      SingularityUserPermissionsResponse permissionsResponse = objectMapper.readValue(
        responseBody,
        SingularityUserPermissionsResponse.class
      );
      if (!permissionsResponse.getUser().isPresent()) {
        throw WebExceptions.unauthorized(
          String.format("No user present in response %s", permissionsResponse)
        );
      }
      if (!permissionsResponse.getUser().get().isAuthenticated()) {
        throw WebExceptions.unauthorized(
          String.format("User not authenticated (response: %s)", permissionsResponse)
        );
      }
      return permissionsResponse;
    }
  }
}
