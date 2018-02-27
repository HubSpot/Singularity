package com.hubspot.singularity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Describes a request grouping")
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

    @Schema(
        title = "A unique id for this request group",
        description = "Max length is set in configuration yaml as maxRequestIdSize",
        pattern = "a-zA-Z0-9_-"
    )
    public String getId() {
        return id;
    }

    @Schema(description = "The list of request ids that belong to this group")
    public List<String> getRequestIds() {
        return requestIds;
    }

    @Schema(description = "Metadata related to this request group")
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
