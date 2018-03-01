package com.hubspot.singularity.api.task;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A description of the current resource usage of a task")
public abstract class AbstractSingularityTaskCurrentUsage implements SingularityTaskUsageBase {}
