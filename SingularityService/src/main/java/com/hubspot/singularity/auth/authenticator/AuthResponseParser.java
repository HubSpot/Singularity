package com.hubspot.singularity.auth.authenticator;

public enum AuthResponseParser {
  WRAPPED, // conforms to SingularityUserPermissionsResponse
  RAW // conforms to SingularityUser
}
