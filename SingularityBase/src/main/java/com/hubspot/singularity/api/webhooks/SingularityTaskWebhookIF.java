package com.hubspot.singularity.api.webhooks;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;
import com.hubspot.singularity.api.task.SingularityTask;
import com.hubspot.singularity.api.task.SingularityTaskHistoryUpdate;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A webhook sent for a task status update")
public interface SingularityTaskWebhookIF {
  @Schema(description = "The task this webhook refers to.")
  SingularityTask getTask();

  @Schema(description = "The task history update this webhook refers to.")
  SingularityTaskHistoryUpdate getTaskUpdate();
}
