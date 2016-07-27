package com.hubspot.singularity;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityRequestGroup {
    private final String id;
    private final List<String> requestIds;

    @JsonCreator
    public SingularityRequestGroup(@JsonProperty("id") String id, @JsonProperty("requestIds") List<String> requestIds) {
        this.id = id;
        this.requestIds = requestIds;
    }

    public String getId() {
        return id;
    }

    public List<String> getRequestIds() {
        return requestIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SingularityRequestGroup that = (SingularityRequestGroup) o;
        return Objects.equals(id, that.id) &&
            Objects.equals(requestIds, that.requestIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestIds);
    }

    @Override
    public String toString() {
        return "SingularityRequestGroup{" +
            "id='" + id + '\'' +
            ", requestIds=" + requestIds +
            '}';
    }
}
