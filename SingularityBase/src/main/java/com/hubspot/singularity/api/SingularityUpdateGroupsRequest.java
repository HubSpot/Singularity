package com.hubspot.singularity.api;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.SingularityUserFacingAction;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Schema(description = "Represents updates to the groups for a request")
public class SingularityUpdateGroupsRequest {

  private final Optional<String> group;
  private final Set<String> readWriteGroups;
  private final Set<String> readOnlyGroups;
  private final Optional<Map<String, Set<SingularityUserFacingAction>>> actionPermissions;
  private final Optional<String> message;

  @JsonCreator
  public SingularityUpdateGroupsRequest(
    @JsonProperty("group") Optional<String> group,
    @JsonProperty("readWriteGroups") Set<String> readWriteGroups,
    @JsonProperty("readOnlyGroups") Set<String> readOnlyGroups,
    @JsonProperty(
      "actionPermissions"
    ) Optional<Map<String, Set<SingularityUserFacingAction>>> actionPermissions,
    @JsonProperty("message") Optional<String> message
  ) {
    this.group = group;
    this.readWriteGroups =
      readWriteGroups != null ? readWriteGroups : Collections.emptySet();
    this.readOnlyGroups =
      readOnlyGroups != null ? readOnlyGroups : Collections.emptySet();
    this.actionPermissions = actionPermissions;
    this.message = message;
  }

  @Schema(description = "The primary request group", nullable = true)
  public Optional<String> getGroup() {
    return group;
  }

  @Schema(description = "Groups allowed read/write access to a request")
  public Set<String> getReadWriteGroups() {
    return readWriteGroups;
  }

  @Schema(description = "Groups allowed read only access to a request")
  public Set<String> getReadOnlyGroups() {
    return readOnlyGroups;
  }

  @Schema(description = "Overidden scopes for specific groups")
  public Optional<Map<String, Set<SingularityUserFacingAction>>> getActionPermissions() {
    return actionPermissions;
  }

  @Schema(
    description = "An option message detailing the reason for the group updates",
    nullable = true
  )
  public Optional<String> getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SingularityUpdateGroupsRequest) {
      final SingularityUpdateGroupsRequest that = (SingularityUpdateGroupsRequest) obj;
      return (
        Objects.equals(this.group, that.group) &&
        Objects.equals(this.readWriteGroups, that.readWriteGroups) &&
        Objects.equals(this.readOnlyGroups, that.readOnlyGroups) &&
        Objects.equals(this.actionPermissions, that.actionPermissions) &&
        Objects.equals(this.message, that.message)
      );
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      group,
      readWriteGroups,
      readOnlyGroups,
      actionPermissions,
      message
    );
  }

  @Override
  public String toString() {
    return (
      "SingularityUpdateGroupsRequest{" +
      "group=" +
      group +
      ", readWriteGroups=" +
      readWriteGroups +
      ", readOnlyGroups=" +
      readOnlyGroups +
      ", groupScopeOverrides=" +
      actionPermissions +
      ", message=" +
      message +
      '}'
    );
  }
}
