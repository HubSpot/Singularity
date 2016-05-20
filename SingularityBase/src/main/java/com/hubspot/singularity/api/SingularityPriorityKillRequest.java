package com.hubspot.singularity.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPriorityKillRequest {
    private final double minimumPriorityLevel;
    private final Optional<String> message;
    private final Optional<String> actionId;

    @JsonCreator
    public SingularityPriorityKillRequest(@JsonProperty("minimumPriorityLevel") double minimumPriorityLevel, @JsonProperty("message") Optional<String> message, @JsonProperty("actionId") Optional<String> actionId) {
        this.minimumPriorityLevel = minimumPriorityLevel;
        this.message = message;
        this.actionId = actionId;
    }

    public double getMinimumPriorityLevel() {
        return minimumPriorityLevel;
    }

    public Optional<String> getMessage() {
        return message;
    }

    public Optional<String> getActionId() {
        return actionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SingularityPriorityKillRequest that = (SingularityPriorityKillRequest) o;
        return Double.compare(that.minimumPriorityLevel, minimumPriorityLevel) == 0 &&
            Objects.equals(message, that.message) &&
            Objects.equals(actionId, that.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimumPriorityLevel, message, actionId);
    }

    @Override
    public String toString() {
        return "SingularityPriorityKillRequest[" +
            "minimumPriorityLevel=" + minimumPriorityLevel +
            ", message=" + message +
            ", actionId=" + actionId +
            ']';
    }
}
