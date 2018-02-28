package com.hubspot.singularity.auth;

import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityDisabledAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityWebhookAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityHeaderPassthroughAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityQueryParamAuthenticator;

public enum SingularityAuthenticatorClass {
  DISABLED(SingularityDisabledAuthenticator.class),
  HEADER_PASSTHROUGH(SingularityHeaderPassthroughAuthenticator.class),
  QUERYPARAM_PASSTHROUGH(SingularityQueryParamAuthenticator.class),
  WEBHOOK(SingularityWebhookAuthenticator.class);

  private final Class<? extends SingularityAuthenticator> authenticatorClass;

  SingularityAuthenticatorClass(Class<? extends SingularityAuthenticator> authenticatorClass) {
    this.authenticatorClass = authenticatorClass;
  }

  public Class<? extends SingularityAuthenticator> getAuthenticatorClass() {
    return authenticatorClass;
  }
}
