package com.hubspot.singularity;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.auth.SingularityAuthFeature;
import com.hubspot.singularity.auth.SingularityAuthenticatorClass;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.auth.authenticator.SingularityAuthenticator;
import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;
import com.hubspot.singularity.auth.datastore.SingularityAuthDatastore;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

public class SingularityAuthModule extends DropwizardAwareModule<SingularityConfiguration> {
  public static final String WEBHOOK_AUTH_HTTP_CLIENT = "singularity.webhook.auth.http.client";

  public SingularityAuthModule() {}

  @Override
  public void configure(Binder binder) {
    Multibinder<SingularityAuthenticator> multibinder = Multibinder.newSetBinder(binder, SingularityAuthenticator.class);
    for (SingularityAuthenticatorClass clazz : getConfiguration().getAuthConfiguration().getAuthenticators()) {
      multibinder.addBinding().to(clazz.getAuthenticatorClass());
      if (clazz == SingularityAuthenticatorClass.WEBHOOK) {
        AuthConfiguration authConfiguration = getConfiguration().getAuthConfiguration();
        AsyncHttpClientConfig clientConfig = new AsyncHttpClientConfig.Builder()
            .setConnectionTimeoutInMs(authConfiguration.getWebhookAuthConnectTimeoutMs())
            .setRequestTimeoutInMs(authConfiguration.getWebhookAuthRequestTimeoutMs())
            .setMaxRequestRetry(authConfiguration.getWebhookAuthRetries())
            .build();
        SingularityAsyncHttpClient webhookAsyncHttpClient = new SingularityAsyncHttpClient(clientConfig);
        binder.bind(AsyncHttpClient.class).annotatedWith(Names.named(WEBHOOK_AUTH_HTTP_CLIENT)).toInstance(webhookAsyncHttpClient);
      }
    }

    binder.bind(SingularityAuthFeature.class);
    binder.bind(SingularityMultiMethodAuthenticator.class);
    binder.bind(SingularityAuthDatastore.class).to(getConfiguration().getAuthConfiguration().getDatastore().getAuthDatastoreClass());
    binder.bind(SingularityAuthorizationHelper.class).in(Scopes.SINGLETON);
  }
}
