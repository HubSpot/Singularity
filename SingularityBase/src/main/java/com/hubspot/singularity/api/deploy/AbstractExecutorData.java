package com.hubspot.singularity.api.deploy;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Custom executor settings")
public abstract class AbstractExecutorData implements ExecutorDataBase {}
