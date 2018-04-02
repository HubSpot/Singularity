package com.hubspot.singularity;

import static com.google.common.collect.ImmutableSet.copyOf;

import java.security.Principal;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Information about a user")
public class SingularityUser implements Principal {
  private final String id;
  private final Optional<String> name;
  private final Optional<String> email;
  private final Set<String> groups;
  private final boolean authenticated;

  public static SingularityUser DEFAULT_USER = new SingularityUser("singularity", Optional.absent(), Optional.absent(), Collections.emptySet(), false);

  public SingularityUser(String id, Optional<String> name, Optional<String> email, Set<String> groups) {
    this(id, name, email, groups, true);
  }

  @JsonCreator
  public SingularityUser(@JsonProperty("id") String id,
                         @JsonProperty("name") Optional<String> name,
                         @JsonProperty("email") Optional<String> email,
                         @JsonProperty("groups") Set<String> groups,
                         @JsonProperty("authenticated") boolean authenticated) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.groups = copyOf(groups);
    this.authenticated = authenticated;
  }

  @Schema(description = "The user's id")
  public String getId() {
    return id;
  }

  @Schema(description = "The user's name, or id if name not specified")
  public String getName() {
    return name.or(id);
  }

  @Schema(description = "The user's email", nullable = true)
  public Optional<String> getEmail() {
    return email;
  }

  @Schema(description = "Groups this user is a part of")
  public Set<String> getGroups() {
    return groups;
  }

  @Schema(description = "True if the user was successfully authenticated")
  public boolean isAuthenticated() {
    return authenticated;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof SingularityUser) {
      final SingularityUser that = (SingularityUser) obj;
      return Objects.equals(this.authenticated, that.authenticated) &&
          Objects.equals(this.id, that.id) &&
          Objects.equals(this.name, that.name) &&
          Objects.equals(this.email, that.email) &&
          Objects.equals(this.groups, that.groups);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, groups, authenticated);
  }

  @Override
  public String toString() {
    return "SingularityUser{" +
        "id='" + id + '\'' +
        ", name=" + name +
        ", email=" + email +
        ", groups=" + groups +
        ", authenticated=" + authenticated +
        '}';
  }
}
