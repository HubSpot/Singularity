package com.hubspot.mesos;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;

public class SingularityDockerParameter {
    private final String key;
    private final String value;

    @JsonCreator
    public static SingularityDockerParameter fromString(String value) {
        return new SingularityDockerParameter(value, "");
    }

    @JsonCreator
    public SingularityDockerParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "SingularityDockerParameter{" +
            "key='" + key + '\'' +
            ", value=" + value +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SingularityDockerParameter that = (SingularityDockerParameter) o;
        return Objects.equals(key, that.key) &&
            Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }
}
