package com.hubspot.singularity.proxy;

import java.util.List;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosTaskStatisticsObject;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityPaginatedResponse;
import com.hubspot.singularity.SingularityPendingDeploy;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingRequestParent;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestCleanup;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestParent;
import com.hubspot.singularity.SingularityS3LogMetadata;
import com.hubspot.singularity.SingularitySandbox;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsage;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.SingularityUserHolder;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

public class TypeRefs {
  public static final TypeReference<List<String>> LIST_STRING_REF = new TypeReference<List<String>>() {};
  public static final TypeReference<List<List<String>>> LIST_LIST_STRING_REF = new TypeReference<List<List<String>>>() {};

  public static final TypeReference<Response> RESPONSE_REF = new TypeReference<Response>() {};

  public static final TypeReference<SingularityUserHolder> USER_HOLDER_TYPE_REF = new TypeReference<SingularityUserHolder>() {};
  public static final TypeReference<SingularityUserSettings> USER_SETTINGS_REF = new TypeReference<SingularityUserSettings>() {};

  public static final TypeReference<List<SingularityPendingDeploy>> PENDING_DEPLOY_LIST_REF = new TypeReference<List<SingularityPendingDeploy>>() {};
  public static final TypeReference<SingularityDeployHistory> DEPLOY_HISTORY_REF = new TypeReference<SingularityDeployHistory>() {};
  public static final TypeReference<List<SingularityDeployHistory>> DEPLOY_HISTORY_LIST_REF = new TypeReference<List<SingularityDeployHistory>>() {};
  public static final TypeReference<SingularityPaginatedResponse<SingularityDeployHistory>> PAGINATED_DEPLOY_HISTORY_REF = new TypeReference<SingularityPaginatedResponse<SingularityDeployHistory>>() {};

  public static final TypeReference<SingularityPendingRequestParent> PENDING_REQUEST_PARENT_REF = new TypeReference<SingularityPendingRequestParent>() {};
  public static final TypeReference<List<SingularityPendingRequest>> PENDING_REQUEST_LIST_REF = new TypeReference<List<SingularityPendingRequest>>() {};
  public static final TypeReference<List<SingularityRequestCleanup>> REQUEST_CLEANUP_LIST_REF = new TypeReference<List<SingularityRequestCleanup>>() {};
  public static final TypeReference<SingularityRequestParent> REQUEST_PARENT_REF = new TypeReference<SingularityRequestParent>() {};
  public static final TypeReference<SingularityRequest> REQUEST_REF = new TypeReference<SingularityRequest>() {};
  public static final TypeReference<List<SingularityRequestParent>> REQUEST_PARENT_LIST_REF = new TypeReference<List<SingularityRequestParent>>() {};
  public static final TypeReference<List<SingularityRequestHistory>> REQUEST_HISTORY_LIST_REF = new TypeReference<List<SingularityRequestHistory>>() {};
  public static final TypeReference<SingularityPaginatedResponse<SingularityRequestHistory>> PAGINATED_REQUEST_HISTORY_REF = new TypeReference<SingularityPaginatedResponse<SingularityRequestHistory>>() {};

  public static final TypeReference<Optional<SingularityRequestGroup>> OPTIONAL_REQUEST_GROUP_REF = new TypeReference<Optional<SingularityRequestGroup>>() {};
  public static final TypeReference<SingularityRequestGroup> REQUEST_GROUP_REF = new TypeReference<SingularityRequestGroup>() {};
  public static final TypeReference<List<SingularityRequestGroup>> REQUEST_GROUP_LIST_REF = new TypeReference<List<SingularityRequestGroup>>() {};

  public static final TypeReference<Optional<SingularityTaskId>> OPTIONAL_TASK_ID_REF = new TypeReference<Optional<SingularityTaskId>>() {};
  public static final TypeReference<List<SingularityTaskId>> TASK_ID_LIST_REF = new TypeReference<List<SingularityTaskId>>() {};
  public static final TypeReference<SingularityTaskHistory> TASK_HISTORY_REF = new TypeReference<SingularityTaskHistory>() {};
  public static final TypeReference<List<SingularityTaskIdHistory>> TASK_ID_HISTORY_LIST_REF = new TypeReference<List<SingularityTaskIdHistory>>() {};
  public static final TypeReference<SingularityPaginatedResponse<SingularityTaskIdHistory>> PAGINATED_TASK_ID_HISTORY_REF = new TypeReference<SingularityPaginatedResponse<SingularityTaskIdHistory>>() {};
  public static final TypeReference<Optional<SingularityTaskIdHistory>> OPTIONAL_TASK_ID_HISTORY_REF = new TypeReference<Optional<SingularityTaskIdHistory>>() {};
  public static final TypeReference<List<SingularityTaskRequest>> TASK_REQUEST_LIST_REF = new TypeReference<List<SingularityTaskRequest>>() {};
  public static final TypeReference<List<SingularityPendingTaskId>> PENDING_TASK_ID_LIST_REF = new TypeReference<List<SingularityPendingTaskId>>() {};
  public static final TypeReference<SingularityTaskRequest> TASK_REQUEST_REF = new TypeReference<SingularityTaskRequest>() {};
  public static final TypeReference<List<SingularityTask>> TASK_LIST_REF = new TypeReference<List<SingularityTask>>() {};
  public static final TypeReference<SingularityTask> TASK_REF = new TypeReference<SingularityTask>() {};
  public static final TypeReference<MesosTaskStatisticsObject> TASK_STATISTICS_REF = new TypeReference<MesosTaskStatisticsObject>() {};
  public static final TypeReference<SingularityTaskCleanup> TASK_CLEANUP_REF = new TypeReference<SingularityTaskCleanup>() {};
  public static final TypeReference<Optional<SingularityTaskCleanup>> OPTIONAL_TASK_CLEANUP_REF = new TypeReference<Optional<SingularityTaskCleanup>>() {};
  public static final TypeReference<List<SingularityTaskCleanup>> TASK_CLEANUP_LIST_REF = new TypeReference<List<SingularityTaskCleanup>>() {};
  public static final TypeReference<List<SingularityKilledTaskIdRecord>> TASK_KILLED_LIST_REF = new TypeReference<List<SingularityKilledTaskIdRecord>>() {};

  public static final TypeReference<SingularityTaskShellCommandRequest> SHELL_COMMAND_REF = new TypeReference<SingularityTaskShellCommandRequest>() {};
  public static final TypeReference<List<SingularityTaskShellCommandRequest>> SHELL_COMMAND_LIST_REF = new TypeReference<List<SingularityTaskShellCommandRequest>>() {};

  public static final TypeReference<Optional<SingularityTaskState>> OPTIONAL_TASK_STATE_REF = new TypeReference<Optional<SingularityTaskState>>() {};

  public static final TypeReference<List<SingularityRack>> RACK_LIST_REF = new TypeReference<List<SingularityRack>>() {};
  public static final TypeReference<List<SingularitySlave>> SLAVE_LIST_REF = new TypeReference<List<SingularitySlave>>() {};
  public static final TypeReference<Optional<SingularitySlave>> OPTIONAL_SLAVE_REF = new TypeReference<Optional<SingularitySlave>>() {};
  public static final TypeReference<List<SingularityMachineStateHistoryUpdate>> MACHINE_UPDATE_LIST_REF = new TypeReference<List<SingularityMachineStateHistoryUpdate>>() {};
  public static final TypeReference<List<SingularityExpiringMachineState>> EXPIRING_MACHINE_STATE_LIST_REF = new TypeReference<List<SingularityExpiringMachineState>>() {};

  public static final TypeReference<List<SingularitySlaveUsageWithId>> SLAVE_USAGE_WITH_ID_LIST_REF = new TypeReference<List<SingularitySlaveUsageWithId>>() {};
  public static final TypeReference<List<SingularityTaskCurrentUsageWithId>> SLAVE_TASK_USAGE_WITH_ID_LIST_REF = new TypeReference<List<SingularityTaskCurrentUsageWithId>>() {};
  public static final TypeReference<List<SingularitySlaveUsage>> SLAVE_USAGE_LIST_REF = new TypeReference<List<SingularitySlaveUsage>>() {};
  public static final TypeReference<List<SingularityTaskUsage>> TASK_USAGE_LIST_REF = new TypeReference<List<SingularityTaskUsage>>() {};

  public static final TypeReference<List<SingularityS3LogMetadata>> LOG_METADATA_LIST_REF = new TypeReference<List<SingularityS3LogMetadata>>() {};

  public static final TypeReference<SingularitySandbox> SANDBOX_REF = new TypeReference<SingularitySandbox>() {};
  public static final TypeReference<MesosFileChunkObject> FILE_CHUNK_REF = new TypeReference<MesosFileChunkObject>() {};
}
