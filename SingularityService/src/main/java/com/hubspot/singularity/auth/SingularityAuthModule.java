package com.hubspot.singularity.auth;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.SingularityAsyncHttpClient;
import com.hubspot.singularity.auth.authenticator.RawUserResponseParser;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;
import com.hubspot.singularity.auth.authenticator.WebhookResponseParser;
import com.hubspot.singularity.auth.authenticator.WrappedUserResponseParser;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.auth.dw.SingularityAuthFeature;
import com.hubspot.singularity.auth.dw.SingularityAuthenticatorClass;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import java.util.HashSet;
import java.util.Set;

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
    // don't double bind, but maintain ordering
    Set<SingularityAuthenticatorClass> bound = new HashSet<>();
    for (SingularityAuthenticatorClass clazz : getConfiguration()
      .getAuthConfiguration()
      .getAuthenticators()) {
      if (bound.contains(clazz)) {
        continue;
      }
      bound.add(clazz);
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

    switch (getConfiguration().getAuthConfiguration().getAuthMode()) {
      case GROUPS_SCOPES:
        binder
          .bind(SingularityAuthorizer.class)
          .to(SingularityGroupsScopesAuthorizer.class)
          .in(Scopes.SINGLETON);
        break;
      case GROUPS_LOG_SCOPES:
        binder
          .bind(SingularityAuthorizer.class)
          .to(SingularityDualAuthorizer.class)
          .in(Scopes.SINGLETON);
        break;
      case GROUPS:
      default:
        binder
          .bind(SingularityAuthorizer.class)
          .to(SingularityGroupsAuthorizer.class)
          .in(Scopes.SINGLETON);
        break;
    }

    switch (getConfiguration().getAuthConfiguration().getAuthResponseParser()) {
      case RAW:
        binder
          .bind(WebhookResponseParser.class)
          .to(RawUserResponseParser.class)
          .in(Scopes.SINGLETON);
        break;
      case WRAPPED:
      default:
        binder
          .bind(WebhookResponseParser.class)
          .to(WrappedUserResponseParser.class)
          .in(Scopes.SINGLETON);
        break;
    }

    binder.bind(SingularityAuthFeature.class);
    binder.bind(SingularityMultiMethodAuthenticator.class);
    binder
      .bind(SingularityAuthDatastore.class)
      .to(
        getConfiguration().getAuthConfiguration().getDatastore().getAuthDatastoreClass()
      );
  }
}
