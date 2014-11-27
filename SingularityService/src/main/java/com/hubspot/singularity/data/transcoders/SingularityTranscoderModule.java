package com.hubspot.singularity.data.transcoders;

import static com.hubspot.singularity.data.transcoders.SingularityJsonTranscoderBinder.bindTranscoder;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployWebhook;
import com.hubspot.singularity.SingularityHostState;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.SingularityWebhook;

public class SingularityTranscoderModule implements Module {

  @Override
  public void configure(final Binder binder) {
    bindTranscoder(binder).asSingularityId(SingularityDeployKey.class);
    bindTranscoder(binder).asSingularityId(SingularityPendingTaskId.class);
    bindTranscoder(binder).asSingularityId(SingularityTaskId.class);

    bindTranscoder(binder).asJson(SingularityDeployMarker.class);
    bindTranscoder(binder).asJson(SingularityDeployResult.class);
    bindTranscoder(binder).asJson(SingularityDeployStatistics.class);
    bindTranscoder(binder).asJson(SingularityKilledTaskIdRecord.class);
    bindTranscoder(binder).asJson(SingularityLoadBalancerUpdate.class);
    bindTranscoder(binder).asJson(SingularityPendingDeploy.class);
    bindTranscoder(binder).asJson(SingularityPendingRequest.class);
    bindTranscoder(binder).asJson(SingularityHostState.class);
    bindTranscoder(binder).asJson(SingularityRack.class);
    bindTranscoder(binder).asJson(SingularityRequest.class);
    bindTranscoder(binder).asJson(SingularityRequestCleanup.class);
    bindTranscoder(binder).asJson(SingularityRequestDeployState.class);
    bindTranscoder(binder).asJson(SingularityRequestWithState.class);
    bindTranscoder(binder).asJson(SingularitySlave.class);
    bindTranscoder(binder).asJson(SingularityTaskCleanup.class);
    bindTranscoder(binder).asJson(SingularityTaskHistoryUpdate.class);
    bindTranscoder(binder).asJson(SingularityWebhook.class);

    bindTranscoder(binder).asCompressedJson(SingularityDeployHistory.class);
    bindTranscoder(binder).asCompressedJson(SingularityDeploy.class);
    bindTranscoder(binder).asCompressedJson(SingularityDeployWebhook.class);
    bindTranscoder(binder).asCompressedJson(SingularityRequestHistory.class);
    bindTranscoder(binder).asCompressedJson(SingularityState.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskHealthcheckResult.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskHistory.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskStatusHolder.class);
    bindTranscoder(binder).asCompressedJson(SingularityTask.class);
  }
}
