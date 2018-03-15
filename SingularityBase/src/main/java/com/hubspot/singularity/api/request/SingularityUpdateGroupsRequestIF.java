package com.hubspot.singularity.api.request;

import java.util.Optional;
import java.util.Set;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "Represents updates to the groups for a request")
public interface SingularityUpdateGroupsRequestIF {
  @Schema(description = "The primary request group", nullable = true)
  Optional<String> getGroup();

  @Schema(description = "Groups allowed read/write access to a request")
  Set<String> getReadWriteGroups();

  @Schema(description = "Groups allowed read only access to a request")
  Set<String> getReadOnlyGroups();

  @Schema(description = "An option message detailing the reason for the group updates", nullable = true)
  Optional<String> getMessage();
}
