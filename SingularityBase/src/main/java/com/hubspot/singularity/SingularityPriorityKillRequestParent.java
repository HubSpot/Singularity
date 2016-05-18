package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityPriorityKillRequest;

public class SingularityPriorityKillRequestParent {
    private final SingularityPriorityKillRequest priorityKillRequest;
    private final long timestamp;
    private final Optional<String> user;

    @JsonCreator
    public SingularityPriorityKillRequestParent(@JsonProperty("priorityKillRequest") SingularityPriorityKillRequest priorityKillRequest, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user) {
        this.priorityKillRequest = priorityKillRequest;
        this.timestamp = timestamp;
        this.user = user;
    }

    public SingularityPriorityKillRequest getPriorityKillRequest() {
        return priorityKillRequest;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Optional<String> getUser() {
        return user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SingularityPriorityKillRequestParent that = (SingularityPriorityKillRequestParent) o;
        return timestamp == that.timestamp &&
            Objects.equals(priorityKillRequest, that.priorityKillRequest) &&
            Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priorityKillRequest, timestamp, user);
    }

    @Override
    public String toString() {
        return "SingularityPriorityKillRequestParent[" +
            "priorityKillRequest=" + priorityKillRequest +
            ", timestamp=" + timestamp +
            ", user=" + user +
            ']';
    }
}
