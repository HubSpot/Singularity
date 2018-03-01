package com.hubspot.singularity.data.transcoders;

import static com.hubspot.singularity.data.transcoders.SingularityJsonTranscoderBinder.bindTranscoder;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.hubspot.singularity.api.auth.SingularityUserSettings;
import com.hubspot.singularity.api.common.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.api.common.SingularityState;
import com.hubspot.singularity.api.deploy.SingularityDeploy;
import com.hubspot.singularity.api.deploy.SingularityDeployHistory;
import com.hubspot.singularity.api.deploy.SingularityDeployKey;
import com.hubspot.singularity.api.deploy.SingularityDeployMarker;
import com.hubspot.singularity.api.deploy.SingularityDeployResult;
import com.hubspot.singularity.api.deploy.SingularityDeployStatistics;
import com.hubspot.singularity.api.deploy.SingularityDeployUpdate;
import com.hubspot.singularity.api.disasters.SingularityDisabledAction;
import com.hubspot.singularity.api.disasters.SingularityDisasterDataPoints;
import com.hubspot.singularity.api.disasters.SingularityPriorityFreezeParent;
import com.hubspot.singularity.api.expiring.SingularityExpiringBounce;
import com.hubspot.singularity.api.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.api.expiring.SingularityExpiringPause;
import com.hubspot.singularity.api.expiring.SingularityExpiringScale;
import com.hubspot.singularity.api.expiring.SingularityExpiringSkipHealthchecks;
import com.hubspot.singularity.api.machines.SingularityClusterUtilization;
import com.hubspot.singularity.api.machines.SingularityHostState;
import com.hubspot.singularity.api.machines.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.api.machines.SingularityRack;
import com.hubspot.singularity.api.machines.SingularitySlave;
import com.hubspot.singularity.api.machines.SingularitySlaveUsage;
import com.hubspot.singularity.api.request.SingularityPendingDeploy;
import com.hubspot.singularity.api.request.SingularityPendingRequest;
import com.hubspot.singularity.api.request.SingularityRequest;
import com.hubspot.singularity.api.request.SingularityRequestCleanup;
import com.hubspot.singularity.api.request.SingularityRequestDeployState;
import com.hubspot.singularity.api.request.SingularityRequestGroup;
import com.hubspot.singularity.api.request.SingularityRequestHistory;
import com.hubspot.singularity.api.request.SingularityRequestLbCleanup;
import com.hubspot.singularity.api.request.SingularityRequestWithState;
import com.hubspot.singularity.api.request.SingularityUpdatePendingDeployRequest;
import com.hubspot.singularity.api.task.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.api.task.SingularityPendingTask;
import com.hubspot.singularity.api.task.SingularityPendingTaskId;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskCleanup;
import com.hubspot.singularity.api.task.SingularityTaskCurrentUsage;
import com.hubspot.singularity.api.task.SingularityTaskDestroyFrameworkMessage;
import com.hubspot.singularity.api.task.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.api.task.SingularityTaskHistory;
import com.hubspot.singularity.api.task.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.api.task.SingularityTaskId;
import com.hubspot.singularity.api.task.SingularityTaskMetadata;
import com.hubspot.singularity.api.task.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.api.task.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.api.task.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.api.task.SingularityTaskStatusHolder;
import com.hubspot.singularity.api.task.SingularityTaskUsage;
import com.hubspot.singularity.api.webhooks.SingularityWebhook;

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
