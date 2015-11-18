package com.hubspot.singularity;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityAuthModule implements Module {
  private final SingularityConfiguration configuration;

  public SingularityAuthModule(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(SingularityAuthenticator.class).to(configuration.getAuthConfiguration().getAuthenticator().getAuthenticatorClass());
    binder.bind(SingularityAuthDatastore.class).to(configuration.getAuthConfiguration().getDatastore().getAuthDatastoreClass());
    binder.bind(new TypeLiteral<Optional<SingularityUser>>() {}).toProvider(SingularityAuthenticator.class);
    binder.bind(SingularityAuthorizationHelper.class).in(Scopes.SINGLETON);
  }
}
