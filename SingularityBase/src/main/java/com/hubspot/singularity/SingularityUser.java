package com.hubspot.singularity;

import static com.google.common.collect.ImmutableSet.copyOf;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityUser {
  private final String id;
  private final Optional<String> name;
  private final Optional<String> email;
  private final Set<String> groups;
  private final Optional<Long> lastUpdatedAt;

  @JsonCreator
  public SingularityUser(@JsonProperty("id") String id, @JsonProperty("name") Optional<String> name, @JsonProperty("email") Optional<String> email, @JsonProperty("groups") Set<String> groups, @JsonProperty("lastUpdated") Optional<Long> lastUpdatedAt) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.groups = copyOf(groups);
    this.lastUpdatedAt = lastUpdatedAt;
  }

  public String getId() {
    return id;
  }

  public Optional<String> getName() {
    return name;
  }

  public Optional<String> getEmail() {
    return email;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public Optional<Long> getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  @Override
  public String toString() {
    return "SingularityUser[" +
            "id='" + id + '\'' +
            ", name=" + name +
            ", email=" + email +
            ", groups=" + groups +
            ", lastUpdatedAt=" + lastUpdatedAt +
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
    SingularityUser that = (SingularityUser) o;
    return Objects.equals(id, that.id) &&
            Objects.equals(name, that.name) &&
            Objects.equals(email, that.email) &&
            Objects.equals(groups, that.groups) &&
            Objects.equals(lastUpdatedAt, that.lastUpdatedAt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, groups, lastUpdatedAt);
  }

  public SingularityUserBuilder toBuilder() {
    return new SingularityUserBuilder(id)
            .setName(name)
            .setEmail(email)
            .setGroups(groups)
            .setLastUpdatedAt(lastUpdatedAt);
  }
}
