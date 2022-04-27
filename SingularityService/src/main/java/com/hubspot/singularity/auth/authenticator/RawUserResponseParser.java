package com.hubspot.singularity.auth.authenticator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.WebhookAuthUser;
import com.hubspot.singularity.config.AuthConfiguration;
import com.ning.http.client.Response;
import java.io.IOException;
import java.util.Optional;

@Singleton
public class RawUserResponseParser extends WebhookResponseParser {

  private final ObjectMapper objectMapper;
  private final AuthConfiguration authConfiguration;

  @Inject
  public RawUserResponseParser(
    @Singularity ObjectMapper objectMapper,
    AuthConfiguration authConfiguration
  ) {
    this.objectMapper = objectMapper;
    this.authConfiguration = authConfiguration;
  }

  SingularityUserPermissionsResponse parse(Response response) throws IOException {
    if (response.getStatusCode() > 299) {
      throw WebExceptions.unauthorized(
        String.format("Got status code %d when verifying jwt", response.getStatusCode())
      );
    } else {
      String responseBody = response.getResponseBody();
      WebhookAuthUser user = objectMapper.readValue(responseBody, WebhookAuthUser.class);
      return new SingularityUserPermissionsResponse(
        Optional.of(
          new SingularityUser(
            user.getUid(),
            Optional.of(user.getUid()),
            authConfiguration
              .getDefaultEmailDomain()
              .map(d -> String.format("%s@%s", user.getUid(), d)),
            user.getGroups(),
            user.getScopes(),
            true
          )
        ),
        Optional.empty()
      );
    }
  }
}
