package com.hubspot.singularity;

import static com.google.common.collect.ImmutableSet.copyOf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Schema(description = "Information about a user")
public class SingularityUser implements Principal {
  private final String id;
  private final Optional<String> name;
  private final Optional<String> email;
  private final Set<String> groups;
  private final Set<String> scopes;
  private final boolean authenticated;

  public static SingularityUser DEFAULT_USER = new SingularityUser(
    "singularity",
    Optional.empty(),
    Optional.empty(),
    Collections.emptySet(),
    false
  );

  public SingularityUser(
    String id,
    Optional<String> name,
    Optional<String> email,
    Set<String> groups
  ) {
    this(id, name, email, groups, true);
  }

  public SingularityUser(
    String id,
    Optional<String> name,
    Optional<String> email,
    Set<String> groups,
    boolean authenticated
  ) {
    this(id, name, email, groups, Collections.emptySet(), authenticated);
  }

  public SingularityUser withOnlyGroups() {
    Set<String> mergedGroups = new HashSet<>();
    mergedGroups.addAll(groups);
    mergedGroups.addAll(scopes);
    return new SingularityUser(
      id,
      name,
      email,
      mergedGroups,
      Collections.emptySet(),
      authenticated
    );
  }

  @JsonCreator
  public SingularityUser(
    @JsonProperty("id") String id,
    @JsonProperty("name") Optional<String> name,
    @JsonProperty("email") Optional<String> email,
    @JsonProperty("groups") Set<String> groups,
    @JsonProperty("scopes") Set<String> scopes,
    @JsonProperty("authenticated") boolean authenticated
  ) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.groups = copyOf(groups);
    this.scopes = scopes != null ? copyOf(scopes) : Collections.emptySet();
    this.authenticated = authenticated;
  }

  @Schema(description = "The user's id")
  public String getId() {
    return id;
  }

  @Schema(description = "The user's name, or id if name not specified")
  public String getName() {
    return name.orElse(id);
  }

  @Schema(description = "The user's email", nullable = true)
  public Optional<String> getEmail() {
    return email;
  }

  @Schema(description = "Groups this user is a part of")
  public Set<String> getGroups() {
    return groups;
  }

  @Schema(description = "Scopes this user has")
  public Set<String> getScopes() {
    return scopes;
  }

  @Schema(description = "True if the user was successfully authenticated")
  public boolean isAuthenticated() {
    return authenticated;
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
    return (
      authenticated == that.authenticated &&
      Objects.equals(id, that.id) &&
      Objects.equals(name, that.name) &&
      Objects.equals(email, that.email) &&
      Objects.equals(groups, that.groups) &&
      Objects.equals(scopes, that.scopes)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, email, groups, scopes, authenticated);
  }

  @Override
  public String toString() {
    return (
      "SingularityUser{" +
      "id='" +
      id +
      '\'' +
      ", name=" +
      name +
      ", email=" +
      email +
      ", groups=" +
      groups +
      ", scopes=" +
      scopes +
      ", authenticated=" +
      authenticated +
      '}'
    );
  }
}
