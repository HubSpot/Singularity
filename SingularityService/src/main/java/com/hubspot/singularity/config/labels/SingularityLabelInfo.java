package com.hubspot.singularity.config.labels;

import java.util.EnumSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.RequestType;

public class SingularityLabelInfo {
    private final String labelName;
    private final String labelTitle;
    private final String labelDefaultValue;
    private final Set<RequestType> requestTypes;

    @JsonCreator
    public SingularityLabelInfo(@JsonProperty("labelName") String labelName, @JsonProperty("labelTitle") Optional<String> labelTitle, @JsonProperty("labelDefaultValue") Optional<String> labelDefaultValue, @JsonProperty("requestTypes") Optional<Set<RequestType>> requestTypes) {
        this.labelName = labelName;
        this.labelTitle = labelTitle.or(labelName);
        this.labelDefaultValue = labelDefaultValue.or("");
        this.requestTypes = requestTypes.or(EnumSet.allOf(RequestType.class));
    }

    public String getLabelName() {
        return labelName;
    }

    public String getLabelTitle() {
        return labelTitle;
    }

    public String getLabelDefaultValue() {
        return labelDefaultValue;
    }

    public Set<RequestType> getRequestTypes() {
        return requestTypes;
    }

    @Override
    public String toString() {
        return "SingularityLabelInfo{" +
            "labelName='" + labelName + '\'' +
            ", labelTitle=" + labelTitle +
            ", labelDefaultValue=" + labelDefaultValue +
            ", requestTypes=" + requestTypes +
            '}';
    }
}
