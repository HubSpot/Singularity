package com.hubspot.singularity;

import com.google.common.base.Optional;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.singularity.auth.SingularityAuthenticatorClass;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityUserProvider;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityAuthModule implements Module {
  private final SingularityConfiguration configuration;

  public SingularityAuthModule(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void configure(Binder binder) {
    Multibinder<SingularityAuthenticator> multibinder = Multibinder.newSetBinder(binder, SingularityAuthenticator.class);
    for (SingularityAuthenticatorClass clazz : configuration.getAuthConfiguration().getAuthenticators()) {
      multibinder.addBinding().to(clazz.getAuthenticatorClass());
    }

    binder.bind(SingularityAuthDatastore.class).to(configuration.getAuthConfiguration().getDatastore().getAuthDatastoreClass());
    binder.bind(new TypeLiteral<Optional<SingularityUser>>() {}).toProvider(SingularityUserProvider.class);
    binder.bind(SingularityAuthorizationHelper.class).in(Scopes.SINGLETON);
  }
}
