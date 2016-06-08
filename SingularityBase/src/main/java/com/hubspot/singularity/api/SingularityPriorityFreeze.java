package com.hubspot.singularity.api;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityPriorityFreeze {
    private final double minimumPriorityLevel;
    private final boolean killTasks;
    private final Optional<String> message;
    private final Optional<String> actionId;

    @JsonCreator
    public SingularityPriorityFreeze(@JsonProperty("minimumPriorityLevel") double minimumPriorityLevel, @JsonProperty("killTasks") boolean killTasks, @JsonProperty("message") Optional<String> message, @JsonProperty("actionId") Optional<String> actionId) {
        this.minimumPriorityLevel = minimumPriorityLevel;
        this.killTasks = killTasks;
        this.message = message;
        this.actionId = actionId;
    }

    public double getMinimumPriorityLevel() {
        return minimumPriorityLevel;
    }

    public boolean isKillTasks() {
        return killTasks;
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
        SingularityPriorityFreeze that = (SingularityPriorityFreeze) o;
        return Double.compare(that.minimumPriorityLevel, minimumPriorityLevel) == 0 &&
            Objects.equals(killTasks, that.killTasks) &&
            Objects.equals(message, that.message) &&
            Objects.equals(actionId, that.actionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minimumPriorityLevel, message, actionId);
    }

    @Override
    public String toString() {
        return "SingularityPriorityFreeze[" +
            "minimumPriorityLevel=" + minimumPriorityLevel +
            ", killTasks=" + killTasks +
            ", message=" + message +
            ", actionId=" + actionId +
            ']';
    }
}
