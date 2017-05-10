package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.hubspot.immutables.style.SingularityStyle;

@Immutable
@SingularityStyle
@JsonDeserialize(as = MesosMasterMetricsSnapshotObject.class)
public interface MesosMasterMetricsSnapshotObjectIF {
  @JsonProperty("master/event_queue_messages")
  int getEventQueueMessages();
}
