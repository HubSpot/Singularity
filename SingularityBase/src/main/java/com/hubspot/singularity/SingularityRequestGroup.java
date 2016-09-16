package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SingularityRequestGroup {
    private final String id;
    private final List<String> requestIds;
    private final Map<String, String> metadata;

    @JsonCreator
    public SingularityRequestGroup(@JsonProperty("id") String id, @JsonProperty("requestIds") List<String> requestIds, @JsonProperty("metadata") Map<String, String> metadata) {
        this.id = id;
        this.requestIds = requestIds;
        this.metadata = metadata != null ? metadata : Collections.<String, String>emptyMap();
    }

    public String getId() {
        return id;
    }

    public List<String> getRequestIds() {
        return requestIds;
    }

    public Map<String, String> getMetadata() {
        return metadata;
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
            Objects.equals(requestIds, that.requestIds) &&
            Objects.equals(metadata, that.metadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, requestIds, metadata);
    }

    @Override
    public String toString() {
        return "SingularityRequestGroup{" +
            "id='" + id + '\'' +
            ", requestIds=" + requestIds +
            ", metadata=" + metadata +
            '}';
    }
}
