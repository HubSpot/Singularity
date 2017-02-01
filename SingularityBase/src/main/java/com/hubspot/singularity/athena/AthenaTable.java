package com.hubspot.singularity.athena;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

public class AthenaTable {
  private final String name;
  private final AthenaDataFormat dataFormat;
  private final List<AthenaField> fields;
  private final List<AthenaPartitionType> partitions;
  private final List<String> allowedRequestIds;
  private final String rowFormat;
  private final String location;

  @JsonCreator
  public AthenaTable(@JsonProperty("name") String name,
                     @JsonProperty("dataFormat") AthenaDataFormat dataFormat,
                     @JsonProperty("fields") List<AthenaField> fields,
                     @JsonProperty("partitions") List<AthenaPartitionType> partitions,
                     @JsonProperty("allowedRequestIds") List<String> allowedRequestIds,
                     @JsonProperty("rowFormat") String rowFormat,
                     @JsonProperty("location") String location) {
    this.name = name;
    this.dataFormat = dataFormat;
    this.fields = Objects.firstNonNull(fields, Collections.<AthenaField>emptyList());
    this.partitions = Objects.firstNonNull(partitions, Collections.<AthenaPartitionType>emptyList());
    this.allowedRequestIds = Objects.firstNonNull(allowedRequestIds, Collections.<String>emptyList());
    this.rowFormat = rowFormat;
    this.location = location;
  }

  public String getName() {
    return name;
  }

  public AthenaDataFormat getDataFormat() {
    return dataFormat;
  }

  public List<AthenaField> getFields() {
    return fields;
  }

  public List<AthenaPartitionType> getPartitions() {
    return partitions;
  }

  public List<String> getAllowedRequestIds() {
    return allowedRequestIds;
  }

  public String getRowFormat() {
    return rowFormat;
  }

  public String getLocation() {
    return location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AthenaTable table = (AthenaTable) o;

    if (name != null ? !name.equals(table.name) : table.name != null) {
      return false;
    }
    if (dataFormat != table.dataFormat) {
      return false;
    }
    if (fields != null ? !fields.equals(table.fields) : table.fields != null) {
      return false;
    }
    if (partitions != null ? !partitions.equals(table.partitions) : table.partitions != null) {
      return false;
    }
    if (allowedRequestIds != null ? !allowedRequestIds.equals(table.allowedRequestIds) : table.allowedRequestIds != null) {
      return false;
    }
    if (rowFormat != null ? !rowFormat.equals(table.rowFormat) : table.rowFormat != null) {
      return false;
    }
    return location != null ? location.equals(table.location) : table.location == null;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (dataFormat != null ? dataFormat.hashCode() : 0);
    result = 31 * result + (fields != null ? fields.hashCode() : 0);
    result = 31 * result + (partitions != null ? partitions.hashCode() : 0);
    result = 31 * result + (allowedRequestIds != null ? allowedRequestIds.hashCode() : 0);
    result = 31 * result + (rowFormat != null ? rowFormat.hashCode() : 0);
    result = 31 * result + (location != null ? location.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "AthenaTable{" +
        "name='" + name + '\'' +
        ", fields=" + fields +
        ", partitions=" + partitions +
        ", allowedRequestIds=" + allowedRequestIds +
        ", rowFormat='" + rowFormat + '\'' +
        ", location='" + location + '\'' +
        '}';
  }
}
