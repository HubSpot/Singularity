package com.hubspot.singularity.proxy;

import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.SingularityAsyncHttpClient;
import com.hubspot.singularity.SingularityServiceBaseModule;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.ning.http.client.AsyncHttpClient;

import io.dropwizard.server.SimpleServerFactory;

public class SingularityClusterCoodinatorResourcesModule extends DropwizardAwareModule<ClusterCoordinatorConfiguration> {

  @Override
  public void configure(Binder binder) {
    binder.bind(AsyncHttpClient.class).to(SingularityAsyncHttpClient.class).in(Scopes.SINGLETON);

    binder.bind(DeployResource.class);
    binder.bind(HistoryResource.class);
    binder.bind(RackResource.class);
    binder.bind(RequestResource.class);
    binder.bind(S3LogResource.class);
    binder.bind(SandboxResource.class);
    binder.bind(SlaveResource.class);
    binder.bind(StateResource.class);
    binder.bind(TaskResource.class);
    binder.bind(WebhookResource.class);
    binder.bind(AuthResource.class);
    binder.bind(UserResource.class);
    binder.bind(DisastersResource.class);
    binder.bind(PriorityResource.class);
    binder.bind(UsageResource.class);
    binder.bind(RequestGroupResource.class);
    binder.bind(InactiveSlaveResource.class);
    binder.bind(TaskTrackerResource.class);

    binder.install(new SingularityServiceBaseModule(getConfiguration().getUiConfiguration()));
  }

  @Provides
  @Singleton
  public IndexViewConfiguration provideIndexViewConfiguration() {
    ClusterCoordinatorConfiguration configuration = getConfiguration();
    return new IndexViewConfiguration(
        configuration.getUiConfiguration(),
        configuration.getDefaultMemory(),
        configuration.getDefaultCpus(),
        configuration.getSlaveHttpPort(),
        configuration.getSlaveHttpsPort(),
        configuration.getBounceExpirationMinutes(),
        configuration.getHealthcheckIntervalSeconds(),
        configuration.getHealthcheckTimeoutSeconds(),
        configuration.getHealthcheckMaxRetries(),
        configuration.getStartupTimeoutSeconds(),
        configuration.isLoadBalancingEnabled(),
        configuration.getCommonHostnameSuffixToOmit(),
        configuration.getWarnIfScheduledJobIsRunningPastNextRunPct()
    );
  }

  @Provides
  @Named(SingularityServiceBaseModule.SINGULARITY_URI_BASE)
  String getSingularityUriBase() {
    final String singularityUiPrefix = getConfiguration().getUiConfiguration().getBaseUrl().or(((SimpleServerFactory) getConfiguration().getServerFactory()).getApplicationContextPath());
    return (singularityUiPrefix.endsWith("/")) ?  singularityUiPrefix.substring(0, singularityUiPrefix.length() - 1) : singularityUiPrefix;
  }
}
