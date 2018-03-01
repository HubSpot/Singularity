package com.hubspot.mesos.json;

import org.immutables.value.Value.Immutable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.annotations.SingularityStyle;

@Immutable
@SingularityStyle
public interface MesosMasterMetricsSnapshotObjectIF {
    @JsonProperty("master/event_queue_messages")
    int getEventQueueMessages();
}
