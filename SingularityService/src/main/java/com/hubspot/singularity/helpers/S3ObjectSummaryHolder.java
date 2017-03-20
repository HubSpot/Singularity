package com.hubspot.singularity.helpers;

import com.amazonaws.services.s3.model.S3ObjectSummary;

public class S3ObjectSummaryHolder {
  private final String group;
  private final S3ObjectSummary objectSummary;

  public S3ObjectSummaryHolder(String group, S3ObjectSummary objectSummary) {
    this.group = group;
    this.objectSummary = objectSummary;
  }

  public String getGroup() {
    return group;
  }

  public S3ObjectSummary getObjectSummary() {
    return objectSummary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    S3ObjectSummaryHolder that = (S3ObjectSummaryHolder) o;

    if (group != null ? !group.equals(that.group) : that.group != null) {
      return false;
    }
    return objectSummary != null ? objectSummary.equals(that.objectSummary) : that.objectSummary == null;
  }

  @Override
  public int hashCode() {
    int result = group != null ? group.hashCode() : 0;
    result = 31 * result + (objectSummary != null ? objectSummary.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "S3ObjectSummaryHolder{" +
        "group='" + group + '\'' +
        ", objectSummary=" + objectSummary +
        '}';
  }
}
