package com.hubspot.singularity.proxy;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.horizon.AsyncHttpClient;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingAsyncHttpClient;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.singularity.SingularityClientCredentials;
import com.hubspot.singularity.SingularityServiceBaseModule;
import com.hubspot.singularity.client.SingularityClient;
import com.hubspot.singularity.client.SingularityClientProvider;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.hubspot.singularity.config.IndexViewConfiguration;

import io.dropwizard.server.SimpleServerFactory;

public class SingularityClusterCoodinatorResourcesModule extends AbstractModule {
  public static final String ASYNC_HTTP_CLIENT = "singularity.async.http.client";

  private final ClusterCoordinatorConfiguration configuration;

  public SingularityClusterCoodinatorResourcesModule(ClusterCoordinatorConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public void configure() {
    bind(DataCenterLocator.class).in(Scopes.SINGLETON);

    bind(DeployResource.class);
    bind(HistoryResource.class);
    bind(RackResource.class);
    bind(RequestResource.class);
    bind(S3LogResource.class);
    bind(SandboxResource.class);
    bind(SlaveResource.class);
    bind(StateResource.class);
    bind(TaskResource.class);
    bind(WebhookResource.class);
    bind(AuthResource.class);
    bind(UserResource.class);
    bind(DisastersResource.class);
    bind(PriorityResource.class);
    bind(UsageResource.class);
    bind(RequestGroupResource.class);
    bind(InactiveSlaveResource.class);
    bind(TaskTrackerResource.class);

    install(new SingularityServiceBaseModule(configuration.getUiConfiguration()));
  }

  @Provides
  @Singleton
  public IndexViewConfiguration provideIndexViewConfiguration() {
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
  public String getSingularityUriBase() {
    final String singularityUiPrefix = configuration.getUiConfiguration().getBaseUrl().or(((SimpleServerFactory) configuration.getServerFactory()).getApplicationContextPath());
    return (singularityUiPrefix.endsWith("/")) ?  singularityUiPrefix.substring(0, singularityUiPrefix.length() - 1) : singularityUiPrefix;
  }

  @Provides
  @Singleton
  @Named(ASYNC_HTTP_CLIENT)
  public AsyncHttpClient provideAsyncHttpClient(ObjectMapper objectMapper) {
    return new NingAsyncHttpClient(HttpConfig.newBuilder().setObjectMapper(objectMapper).build());
  }

  @Provides
  @Singleton
  public Map<String, SingularityClient> provideClients(ObjectMapper objectMapper) {
    HttpClient httpClient = new NingHttpClient(HttpConfig.newBuilder().setObjectMapper(objectMapper).build());
    SingularityClientProvider clientProvider = new SingularityClientProvider(httpClient);
    Map<String, SingularityClient> clients = new HashMap<>();
    configuration.getDataCenters().forEach((dc) -> {
      clientProvider.setHosts(dc.getHosts());
      clientProvider.setContextPath(dc.getContextPath());
      clientProvider.setSsl(dc.getScheme().equals("https"));
      Optional<SingularityClientCredentials> maybeCredentials = dc.getClientCredentials().or(configuration.getDefaultClientCredentials());
      clients.put(dc.getName(), clientProvider.get(maybeCredentials));
    });
    return clients;
  }
}
