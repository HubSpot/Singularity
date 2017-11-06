package com.hubspot.mesos.protos;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class MesosURL {
  private final Optional<String> scheme;
  private final Optional<String> path;
  private final Optional<MesosAddress> address;
  private final Optional<String> fragment;
  private final List<MesosParameter> query;

  @JsonCreator

  public MesosURL(@JsonProperty("scheme") Optional<String> scheme,
                  @JsonProperty("path") Optional<String> path,
                  @JsonProperty("address") Optional<MesosAddress> address,
                  @JsonProperty("fragment") Optional<String> fragment,
                  @JsonProperty("query") List<MesosParameter> query) {
    this.scheme = scheme;
    this.path = path;
    this.address = address;
    this.fragment = fragment;
    this.query = query != null ? query : Collections.emptyList();
  }

  public String getScheme() {
    return scheme.orNull();
  }

  @JsonIgnore
  public boolean hasScheme() {
    return scheme.isPresent();
  }

  public String getPath() {
    return path.orNull();
  }

  @JsonIgnore
  public boolean hasPath() {
    return path.isPresent();
  }

  public MesosAddress getAddress() {
    return address.orNull();
  }

  @JsonIgnore
  public boolean hasAddress() {
    return address.isPresent();
  }

  public String getFragment() {
    return fragment.orNull();
  }

  @JsonIgnore
  public boolean hasFragment() {
    return fragment.isPresent();
  }

  public List<MesosParameter> getQuery() {
    return query;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof MesosURL) {
      final MesosURL that = (MesosURL) obj;
      return Objects.equals(this.scheme, that.scheme) &&
          Objects.equals(this.path, that.path) &&
          Objects.equals(this.address, that.address) &&
          Objects.equals(this.fragment, that.fragment) &&
          Objects.equals(this.query, that.query);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scheme, path, address, fragment, query);
  }

  @Override
  public String toString() {
    return "MesosURL{" +
        "scheme=" + scheme +
        ", path=" + path +
        ", address=" + address +
        ", fragment=" + fragment +
        ", query=" + query +
        '}';
  }
}
