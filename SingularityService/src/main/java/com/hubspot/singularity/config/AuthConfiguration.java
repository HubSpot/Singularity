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
  private SingularityAuthenticatorClass authenticator = SingularityAuthenticatorClass.QUERYPARAM_PASSTHROUGH;

  @JsonProperty
  @NotNull
  private SingularityAuthDatastoreClass datastore = SingularityAuthDatastoreClass.DUMMY;

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
  private Set<String> defaultReadOnlyGroups = new HashSet<>();

  @JsonProperty
  @NotNull
  private Set<String> globalReadOnlyGroups = new HashSet<>();

  @JsonProperty
  @NotNull
  private String requestUserHeaderName = "X-Username";  // used by SingularityHeaderPassthroughAuthenticator

  @JsonProperty
  @NotNull
  private String jwtAuthVerificationUrl = ""; // used by SingularityExternalJWTAuthenticator

  @JsonProperty
  @NotNull
  private String jwtAuthCookieName = ""; // used by SingularityExternalJWTAuthenticator

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

  public Set<String> getDefaultReadOnlyGroups() {
    return defaultReadOnlyGroups;
  }

  public void setDefaultReadOnlyGroups(Set<String> defaultReadOnlyGroups) {
    this.defaultReadOnlyGroups = defaultReadOnlyGroups;
  }

  public Set<String> getGlobalReadOnlyGroups() {
    return globalReadOnlyGroups;
  }

  public void setGlobalReadOnlyGroups(Set<String> globalReadOnlyGroups) {
    this.globalReadOnlyGroups = globalReadOnlyGroups;
  }

  public String getRequestUserHeaderName() {
    return requestUserHeaderName;
  }

  public void setRequestUserHeaderName(String requestUserHeaderName) {
    this.requestUserHeaderName = requestUserHeaderName;
  }

  public String getJwtAuthVerificationUrl() {
    return jwtAuthVerificationUrl;
  }

  public AuthConfiguration setJwtAuthVerificationUrl(String jwtAuthVerificationUrl) {
    this.jwtAuthVerificationUrl = jwtAuthVerificationUrl;
    return this;
  }

  public String getJwtAuthCookieName() {
    return jwtAuthCookieName;
  }

  public AuthConfiguration setJwtAuthCookieName(String jwtAuthCookieName) {
    this.jwtAuthCookieName = jwtAuthCookieName;
    return this;
  }
}
