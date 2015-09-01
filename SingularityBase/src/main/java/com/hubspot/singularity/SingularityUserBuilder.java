package com.hubspot.singularity;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Optional;

public class SingularityUserBuilder {
  private final String id;
  private Optional<String> name;
  private Optional<String> email;
  private Set<String> groups;
  private Optional<Long> lastUpdatedAt;

  public SingularityUserBuilder(String id) {
    this.id = id;
    this.name = Optional.absent();
    this.email = Optional.absent();
    this.groups = Collections.emptySet();
    this.lastUpdatedAt = Optional.absent();
  }

  public String getId() {
    return id;
  }

  public Optional<String> getName() {
    return name;
  }

  public SingularityUserBuilder setName(Optional<String> name) {
    this.name = name;
    return this;
  }

  public Optional<String> getEmail() {
    return email;
  }

  public SingularityUserBuilder setEmail(Optional<String> email) {
    this.email = email;
    return this;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public SingularityUserBuilder setGroups(Set<String> groups) {
    this.groups = groups;
    return this;
  }

  public Optional<Long> getLastUpdatedAt() {
    return lastUpdatedAt;
  }

  public SingularityUserBuilder setLastUpdatedAt(Optional<Long> lastUpdatedAt) {
    this.lastUpdatedAt = lastUpdatedAt;
    return this;
  }

  public SingularityUser build() {
    return new SingularityUser(id, name, email, groups, lastUpdatedAt);
  }

  @Override
  public String toString() {
    return "SingularityUserBuilder[" +
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
    SingularityUserBuilder that = (SingularityUserBuilder) o;
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
}
