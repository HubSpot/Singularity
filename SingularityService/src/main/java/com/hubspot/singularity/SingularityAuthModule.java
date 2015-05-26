package com.hubspot.singularity;

import javax.servlet.http.HttpServletRequest;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.servlet.RequestScoped;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.ldap.SingularityLDAPManager;

public class SingularityAuthModule implements Module {
  private final SingularityConfiguration configuration;

  public SingularityAuthModule(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void configure(Binder binder) {

  }

  @Provides
  @RequestScoped
  public Optional<SingularityUser> providesUsername(HttpServletRequest request, SingularityLDAPManager ldapManager) {
    final Optional<String> maybeUsername = Optional.fromNullable(Strings.emptyToNull(request.getHeader(configuration.getLdapConfiguration().getRequestUserHeaderName())));

    if (!maybeUsername.isPresent()) {
      return Optional.absent();
    }

    return Optional.of(new SingularityUser(maybeUsername.get(), ldapManager.getGroupsForUser(maybeUsername.get())));
  }
}
