package com.hubspot.mesos.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MesosMasterMetricsSnapshotObject {
    private final int eventQueueMessages;

    @JsonCreator
    public MesosMasterMetricsSnapshotObject(@JsonProperty("master/event_queue_messages") int eventQueueMessages) {
        this.eventQueueMessages = eventQueueMessages;
    }

    public int getEventQueueMessages() {
        return eventQueueMessages;
    }

    @Override
    public String toString() {
        return "MesosMasterMetricsSnapshotObject{" +
            "eventQueueMessages=" + eventQueueMessages +
            '}';
    }
}
