package com.hubspot.singularity.auth;

import com.google.common.base.Optional;
import com.yammer.dropwizard.authenticator.LdapAuthenticator;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import static com.google.common.base.Preconditions.checkNotNull;

public class SingularityResourceAuthenticator implements Authenticator<BasicCredentials, SingularityUser> {
  private final LdapAuthenticator ldapAuthenticator;

  public SingularityResourceAuthenticator(LdapAuthenticator ldapAuthenticator) {
    this.ldapAuthenticator = checkNotNull(ldapAuthenticator);
  }

  @Override
  public Optional<SingularityUser> authenticate(BasicCredentials credentials) throws AuthenticationException {
    if (ldapAuthenticator.authenticate(credentials)) {
      return Optional.of(new SingularityUser(credentials.getUsername()));
    } else {
      return Optional.absent();
    }
  }
}
