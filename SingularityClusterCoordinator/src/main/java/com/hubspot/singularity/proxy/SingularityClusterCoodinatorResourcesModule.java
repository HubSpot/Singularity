package com.hubspot.singularity.proxy;

import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.hubspot.dropwizard.guicier.DropwizardAwareModule;
import com.hubspot.singularity.SingularityAsyncHttpClient;
import com.hubspot.singularity.SingularityServiceBaseModule;
import com.hubspot.singularity.config.ClusterCoordinatorConfiguration;
import com.ning.http.client.AsyncHttpClient;

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
}
