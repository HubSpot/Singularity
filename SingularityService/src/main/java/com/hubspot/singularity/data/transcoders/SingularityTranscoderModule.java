package com.hubspot.singularity.data.transcoders;

import static com.hubspot.singularity.data.transcoders.SingularityJsonTranscoderBinder.bindTranscoder;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityDeployMarker;
import com.hubspot.singularity.SingularityDeployResult;
import com.hubspot.singularity.SingularityDeployStatistics;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisasterDataPoints;
import com.hubspot.singularity.SingularityHostState;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityPriorityFreezeParent;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestLbCleanup;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.expiring.SingularityExpiringPause;
import com.hubspot.singularity.expiring.SingularityExpiringScale;
import com.hubspot.singularity.expiring.SingularityExpiringSkipHealthchecks;

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
    bindTranscoder(binder).asJson(SingularityPendingTask.class);
    bindTranscoder(binder).asJson(SingularityPendingRequest.class);
    bindTranscoder(binder).asJson(SingularityUpdatePendingDeployRequest.class);
    bindTranscoder(binder).asJson(SingularityHostState.class);
    bindTranscoder(binder).asJson(SingularityRack.class);
    bindTranscoder(binder).asJson(SingularityRequest.class);
    bindTranscoder(binder).asJson(SingularityRequestCleanup.class);
    bindTranscoder(binder).asJson(SingularityRequestLbCleanup.class);
    bindTranscoder(binder).asJson(SingularityRequestDeployState.class);
    bindTranscoder(binder).asJson(SingularityRequestWithState.class);
    bindTranscoder(binder).asJson(SingularitySlave.class);
    bindTranscoder(binder).asJson(SingularityTaskCleanup.class);
    bindTranscoder(binder).asJson(SingularityTaskHistoryUpdate.class);
    bindTranscoder(binder).asJson(SingularityWebhook.class);
    bindTranscoder(binder).asJson(SingularityMachineStateHistoryUpdate.class);
    bindTranscoder(binder).asJson(SingularityTaskShellCommandUpdate.class);
    bindTranscoder(binder).asJson(SingularityTaskShellCommandRequest.class);
    bindTranscoder(binder).asJson(SingularityExpiringBounce.class);
    bindTranscoder(binder).asJson(SingularityExpiringPause.class);
    bindTranscoder(binder).asJson(SingularityExpiringScale.class);
    bindTranscoder(binder).asJson(SingularityExpiringSkipHealthchecks.class);
    bindTranscoder(binder).asJson(SingularityTaskDestroyFrameworkMessage.class);
    bindTranscoder(binder).asJson(SingularityTaskReconciliationStatistics.class);
    bindTranscoder(binder).asJson(SingularityDisabledAction.class);
    bindTranscoder(binder).asJson(SingularityDisasterDataPoints.class);
    bindTranscoder(binder).asJson(SingularityRequestGroup.class);
    bindTranscoder(binder).asJson(SingularityExpiringMachineState.class);
    bindTranscoder(binder).asJson(SingularityUserSettings.class);
    bindTranscoder(binder).asJson(SingularitySlaveUsage.class);
    bindTranscoder(binder).asJson(SingularityTaskUsage.class);
    bindTranscoder(binder).asJson(SingularityClusterUtilization.class);
    bindTranscoder(binder).asJson(SingularityTaskCurrentUsage.class);

    bindTranscoder(binder).asCompressedJson(SingularityDeployHistory.class);
    bindTranscoder(binder).asCompressedJson(SingularityDeploy.class);
    bindTranscoder(binder).asCompressedJson(SingularityDeployUpdate.class);
    bindTranscoder(binder).asCompressedJson(SingularityRequestHistory.class);
    bindTranscoder(binder).asCompressedJson(SingularityState.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskHealthcheckResult.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskHistory.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskStatusHolder.class);
    bindTranscoder(binder).asCompressedJson(SingularityTask.class);
    bindTranscoder(binder).asCompressedJson(SingularityTaskMetadata.class);

    bindTranscoder(binder).asJson(SingularityPriorityFreezeParent.class);
  }
}
