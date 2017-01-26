package com.hubspot.singularity.athena;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AthenaTable {
  private final String name;
  private final List<AthenaField> fields;
  private final List<AthenaField> partitions;
  private final String rowFormat;
  private final String location;

  @JsonCreator
  public AthenaTable(@JsonProperty("name") String name, @JsonProperty("fields") List<AthenaField> fields, @JsonProperty("partitions") List<AthenaField> partitions, @JsonProperty("rowFormat") String rowFormat, @JsonProperty("location") String location) {
    this.name = name;
    this.fields = Objects.firstNonNull(fields, Collections.<AthenaField>emptyList());
    this.partitions = Objects.firstNonNull(partitions, Collections.<AthenaField>emptyList());
    this.rowFormat = rowFormat;
    this.location = location;
  }

  public String getName() {
    return name;
  }

  public List<AthenaField> getFields() {
    return fields;
  }

  public List<AthenaField> getPartitions() {
    return partitions;
  }

  public String getRowFormat() {
    return rowFormat;
  }

  public String getLocation() {
    return location;
  }

  @JsonIgnore
  public String getPartitionLocationFormat() {
    StringBuilder builder = new StringBuilder(location);
    for (AthenaField partition : partitions) {
      builder.append("%s/");
    }
    return builder.toString();
  }
}
