package com.hubspot.singularity;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.auth.SingularityAuthFeature;
import com.hubspot.singularity.auth.SingularityAuthenticatorClass;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityAuthModule extends DropwizardAwareModule<SingularityConfiguration> {

  public SingularityAuthModule() {}

  @Override
  public void configure(Binder binder) {
    Multibinder<SingularityAuthenticator> multibinder = Multibinder.newSetBinder(binder, SingularityAuthenticator.class);
    for (SingularityAuthenticatorClass clazz : getConfiguration().getAuthConfiguration().getAuthenticators()) {
      multibinder.addBinding().to(clazz.getAuthenticatorClass());
    }

    binder.bind(SingularityAuthFeature.class);
    binder.bind(SingularityMultiMethodAuthenticator.class);
    binder.bind(SingularityAuthDatastore.class).to(getConfiguration().getAuthConfiguration().getDatastore().getAuthDatastoreClass());
    binder.bind(SingularityAuthorizationHelper.class).in(Scopes.SINGLETON);
  }
}
