package com.hubspot.singularity.auth;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.SingularityAsyncHttpClient;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.auth.dw.SingularityAuthFeature;
import com.hubspot.singularity.auth.dw.SingularityAuthenticatorClass;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class SingularityAuthModule
  extends DropwizardAwareModule<SingularityConfiguration> {
  public static final String WEBHOOK_AUTH_HTTP_CLIENT =
    "singularity.webhook.auth.http.client";

  public SingularityAuthModule() {}

  @Override
  public void configure(Binder binder) {
    binder
      .bind(AuthConfiguration.class)
      .toInstance(getConfiguration().getAuthConfiguration());
    Multibinder<SingularityAuthenticator> multibinder = Multibinder.newSetBinder(
      binder,
      SingularityAuthenticator.class
    );
    for (SingularityAuthenticatorClass clazz : getConfiguration()
      .getAuthConfiguration()
      .getAuthenticators()) {
      multibinder.addBinding().to(clazz.getAuthenticatorClass());
      if (clazz == SingularityAuthenticatorClass.WEBHOOK) {
        AuthConfiguration authConfiguration = getConfiguration().getAuthConfiguration();
        AsyncHttpClientConfig clientConfig = new AsyncHttpClientConfig.Builder()
          .setConnectTimeout(authConfiguration.getWebhookAuthConnectTimeoutMs())
          .setRequestTimeout(authConfiguration.getWebhookAuthRequestTimeoutMs())
          .setMaxRequestRetry(authConfiguration.getWebhookAuthRetries())
          .build();
        SingularityAsyncHttpClient webhookAsyncHttpClient = new SingularityAsyncHttpClient(
          clientConfig
        );
        binder
          .bind(AsyncHttpClient.class)
          .annotatedWith(Names.named(WEBHOOK_AUTH_HTTP_CLIENT))
          .toInstance(webhookAsyncHttpClient);
      }
    }

    if (getConfiguration().getAuthConfiguration().isEnableScopes()) {
      binder
        .bind(SingularityAuthorizer.class)
        .to(SingularityGroupsScopesAuthorizer.class)
        .in(Scopes.SINGLETON);
    } else {
      binder
        .bind(SingularityAuthorizer.class)
        .to(SingularityGroupsAuthorizer.class)
        .in(Scopes.SINGLETON);
    }

    binder.bind(SingularityAuthFeature.class);
    binder.bind(SingularityMultiMethodAuthenticator.class);
    binder
      .bind(SingularityAuthDatastore.class)
      .to(
        getConfiguration().getAuthConfiguration().getDatastore().getAuthDatastoreClass()
      );
    binder.bind(SingularityAuthorizer.class).in(Scopes.SINGLETON);
  }
}
