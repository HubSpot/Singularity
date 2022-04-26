package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;
import java.util.Set;

public class WebhookAuthUser {

  private final String uid;
  private final Set<String> groups;
  private final Set<String> scopes;

  @JsonCreator
  public WebhookAuthUser(
    @JsonProperty("uid") String uid,
    @JsonProperty("groups") Set<String> groups,
    @JsonProperty("scopes") Set<String> scopes
  ) {
    this.uid = uid;
    this.groups = groups;
    this.scopes = scopes;
  }

  public String getUid() {
    return uid;
  }

  public Set<String> getGroups() {
    return groups;
  }

  public Set<String> getScopes() {
    return scopes;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WebhookAuthUser that = (WebhookAuthUser) o;
    return (
      Objects.equals(uid, that.uid) &&
      Objects.equals(groups, that.groups) &&
      Objects.equals(scopes, that.scopes)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(uid, groups, scopes);
  }

  @Override
  public String toString() {
    return (
      "WebhookAuthUser{" +
      "uid='" +
      uid +
      '\'' +
      ", groups=" +
      groups +
      ", scopes=" +
      scopes +
      '}'
    );
  }
}
