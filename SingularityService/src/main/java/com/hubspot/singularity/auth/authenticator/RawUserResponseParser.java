package com.hubspot.singularity.auth.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.Optional;

@Singleton
public class RawUserResponseParser extends WebhookResponseParser {
  private final ObjectMapper objectMapper;

  @Inject
  public RawUserResponseParser(@Singularity ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  SingularityUserPermissionsResponse parse(Response response) throws IOException {
    if (response.getStatusCode() > 299) {
      throw WebExceptions.unauthorized(
        String.format("Got status code %d when verifying jwt", response.getStatusCode())
      );
    } else {
      String responseBody = response.getResponseBody();
      SingularityUser user = objectMapper.readValue(responseBody, SingularityUser.class);
      return new SingularityUserPermissionsResponse(
        Optional.of(user.withAuthenticated()),
        Optional.empty()
      );
    }
  }
}
