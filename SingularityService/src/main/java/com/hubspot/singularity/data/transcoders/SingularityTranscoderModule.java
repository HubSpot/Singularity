package com.hubspot.singularity.data.transcoders;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;

public class SingularityTranscoderModule extends AbstractModule {

  @Override
  public void configure() {
    bind(SingularityDeployHistoryTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityKilledTaskIdRecordTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityLoadBalancerUpdateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityPendingTaskIdTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskCleanupTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHealthcheckResultTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskHistoryUpdateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployKeyTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployMarkerTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployStatisticsTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityPendingDeployTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityPendingRequestTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRackTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestCleanupTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestDeployStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestWithStateTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityRequestHistoryTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityDeployWebhookTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityWebhookTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularitySlaveTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskIdTranscoder.class).in(Scopes.SINGLETON);
    bind(SingularityTaskTranscoder.class).in(Scopes.SINGLETON);
  }
}
