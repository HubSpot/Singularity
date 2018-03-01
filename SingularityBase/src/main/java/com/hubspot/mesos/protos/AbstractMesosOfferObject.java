package com.hubspot.mesos.protos;

import java.util.Map;

import javax.annotation.Nullable;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

/*
 * Mirrors the mesos Offer object, with the exception that slaveId can be read into agentId
 */
@Immutable
@SingularityStyle
@Schema(description = "The mesos protos representation of an offer")
public abstract class AbstractMesosOfferObject {
  @Nullable
  public abstract MesosStringValue getAgentId();

  @Nullable
  public abstract MesosStringValue getSlaveId();

  @Nullable
  public abstract MesosStringValue getFrameworkId();

  @Nullable
  public abstract String getHostname();

  @Nullable
  public abstract MesosStringValue getId();

  // Unknown fields
  @JsonAnyGetter
  public abstract Map<String, Object> getAllOtherFields();

  @JsonIgnore
  public MesosOfferObject sizeOptimized() {
    return MesosOfferObject.builder()
        .setAgentId(getAgentId())
        .setFrameworkId(getFrameworkId())
        .setHostname(getHostname())
        .setId(getId())
        .setAllOtherFields(ImmutableMap.of("url", getAllOtherFields().get("url")))
        .build();
  }
}
