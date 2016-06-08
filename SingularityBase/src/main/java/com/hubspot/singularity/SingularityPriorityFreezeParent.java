package com.hubspot.singularity;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.api.SingularityPriorityFreeze;

public class SingularityPriorityFreezeParent {
    private final SingularityPriorityFreeze priorityFreeze;
    private final long timestamp;
    private final Optional<String> user;

    @JsonCreator
    public SingularityPriorityFreezeParent(@JsonProperty("priorityFreeze") SingularityPriorityFreeze priorityFreeze, @JsonProperty("timestamp") long timestamp, @JsonProperty("user") Optional<String> user) {
        this.priorityFreeze = priorityFreeze;
        this.timestamp = timestamp;
        this.user = user;
    }

    public SingularityPriorityFreeze getPriorityFreeze() {
        return priorityFreeze;
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
        SingularityPriorityFreezeParent that = (SingularityPriorityFreezeParent) o;
        return timestamp == that.timestamp &&
            Objects.equals(priorityFreeze, that.priorityFreeze) &&
            Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(priorityFreeze, timestamp, user);
    }

    @Override
    public String toString() {
        return "SingularityPriorityFreezeParent[" +
            "priorityFreeze=" + priorityFreeze +
            ", timestamp=" + timestamp +
            ", user=" + user +
            ']';
    }
}
