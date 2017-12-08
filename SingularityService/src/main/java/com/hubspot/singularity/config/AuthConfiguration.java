package com.hubspot.singularity.config;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.hubspot.singularity.auth.SingularityAuthDatastoreClass;
import com.hubspot.singularity.auth.SingularityAuthenticatorClass;

public class AuthConfiguration {
  @JsonProperty
  private boolean enabled = false;

  @JsonProperty
  @NotNull
  @Deprecated
  private SingularityAuthenticatorClass authenticator = SingularityAuthenticatorClass.QUERYPARAM_PASSTHROUGH;

  @JsonProperty
  @NotNull
  private List<SingularityAuthenticatorClass> authenticators = Lists.newArrayList(SingularityAuthenticatorClass.QUERYPARAM_PASSTHROUGH);

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

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Deprecated
  public SingularityAuthenticatorClass getAuthenticator() {
    return authenticator;
  }

  public void setAuthenticator(SingularityAuthenticatorClass authenticator) {
    this.authenticator = authenticator;
    this.authenticators.add(authenticator);
  }

  public List<SingularityAuthenticatorClass> getAuthenticators() {
    return authenticators;
  }

  public void setAuthenticators(List<SingularityAuthenticatorClass> authenticators) {
    this.authenticators = authenticators;
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
}
