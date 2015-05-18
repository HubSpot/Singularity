package com.hubspot.singularity.client;

import java.util.Objects;

import com.hubspot.mesos.JavaUtils;

public class SingularityClientCredentials {
  private final String headerName;
  private final String token;

  public SingularityClientCredentials(String headerName, String token) {
    this.headerName = headerName;
    this.token = token;
  }

  public String getHeaderName() {
    return headerName;
  }

  public String getToken() {
    return token;
  }

  @Override
  public String toString() {
    return "SingularityClientCredentials[" +
            "headerName='" + headerName + '\'' +
            ", token='" + JavaUtils.obfuscateValue(token) + '\'' +
            ']';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SingularityClientCredentials that = (SingularityClientCredentials) o;
    return Objects.equals(headerName, that.headerName) &&
            Objects.equals(token, that.token);
  }

  @Override
  public int hashCode() {
    return Objects.hash(headerName, token);
  }
}
