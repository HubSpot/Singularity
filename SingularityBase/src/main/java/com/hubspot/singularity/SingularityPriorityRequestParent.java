package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityPriorityRequest;

public class SingularityPriorityRequestParent {
    private final SingularityPriorityRequest priorityRequest;
    private final long timestamp;
    private final Optional<String> user;

    @JsonCreator
    public SingularityPriorityRequestParent(@JsonProperty("priorityRequest") SingularityPriorityRequest priorityRequest, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user) {
        this.priorityRequest = priorityRequest;
        this.timestamp = timestamp;
        this.user = user;
    }

    public SingularityPriorityRequest getPriorityRequest() {
        return priorityRequest;
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
        SingularityPriorityRequestParent that = (SingularityPriorityRequestParent) o;
        return timestamp == that.timestamp &&
            Objects.equals(priorityRequest, that.priorityRequest) &&
            Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priorityRequest, timestamp, user);
    }

    @Override
    public String toString() {
        return "SingularityPriorityRequestParent[" +
            "priorityRequest=" + priorityRequest +
            ", timestamp=" + timestamp +
            ", user=" + user +
            ']';
    }
}
