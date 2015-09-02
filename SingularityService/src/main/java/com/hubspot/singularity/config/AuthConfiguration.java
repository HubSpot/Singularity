package com.hubspot.singularity.config;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.auth.SingularityAuthDatastoreClass;
import com.hubspot.singularity.auth.SingularityAuthenticatorClass;

public class AuthConfiguration {
  @JsonProperty
  private boolean enabled = false;

  @JsonProperty
  @NotNull
  private SingularityAuthenticatorClass authenticator = SingularityAuthenticatorClass.DISABLED;

  @JsonProperty
  @NotNull
  private SingularityAuthDatastoreClass datastore = SingularityAuthDatastoreClass.DISABLED;

  @JsonProperty
  @NotNull
  private Set<String> requiredGroups = new HashSet<>();

  @JsonProperty
  @NotNull
  private Set<String> adminGroups = new HashSet<>();

  @JsonProperty
  @NotNull
  private Set<String> jitaGroups = new HashSet<>();

  @JsonProperty
  @NotNull
  private String requestUserHeaderName = "X-Username";  // used by SingularityHeaderPassthroughAuthenticator

  @JsonProperty
  private long purgeOldUsersAfterDays = 2;

  @JsonProperty
  private long updateUsersEveryMinutes = 10;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public SingularityAuthenticatorClass getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(SingularityAuthenticatorClass authenticator) {
    this.authenticator = authenticator;
  }

  public SingularityAuthDatastoreClass getDatastore() {
    return datastore;
  }

  public void setDatastore(SingularityAuthDatastoreClass datastore) {
    this.datastore = datastore;
  }

  public Set<String> getRequiredGroups() {
    return requiredGroups;
  }

  public void setRequiredGroups(Set<String> requiredGroups) {
    this.requiredGroups = requiredGroups;
  }

  public Set<String> getAdminGroups() {
    return adminGroups;
  }

  public void setAdminGroups(Set<String> adminGroups) {
    this.adminGroups = adminGroups;
  }

  public Set<String> getJitaGroups() {
    return jitaGroups;
  }

  public void setJitaGroups(Set<String> jitaGroups) {
    this.jitaGroups = jitaGroups;
  }

  public String getRequestUserHeaderName() {
    return requestUserHeaderName;
  }

  public void setRequestUserHeaderName(String requestUserHeaderName) {
    this.requestUserHeaderName = requestUserHeaderName;
  }

  public long getPurgeOldUsersAfterDays() {
    return purgeOldUsersAfterDays;
  }

  public void setPurgeOldUsersAfterDays(long purgeOldUsersAfterDays) {
    this.purgeOldUsersAfterDays = purgeOldUsersAfterDays;
  }

  public long getUpdateUsersEveryMinutes() {
    return updateUsersEveryMinutes;
  }

  public void setUpdateUsersEveryMinutes(long updateUsersEveryMinutes) {
    this.updateUsersEveryMinutes = updateUsersEveryMinutes;
  }
}
