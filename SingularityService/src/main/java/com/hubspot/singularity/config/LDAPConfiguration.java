package com.hubspot.singularity.config;

import java.util.HashSet;
import java.util.Set;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LDAPConfiguration {
  public static LDAPConfiguration disabled() {
    final LDAPConfiguration configuration = new LDAPConfiguration();
    configuration.setEnabled(false);
    return configuration;
  }

  @JsonProperty
  private boolean enabled = true;

  @JsonProperty
  @NotNull
  private String hostname;

  @JsonProperty
  @Min(0)
  private int port = 389;

  @JsonProperty
  @NotEmpty
  private String bindDn = "";

  @JsonProperty
  @NotNull
  private String bindPassword;

  @JsonProperty
  @NotEmpty
  private String groupBaseDN = "";  // ex. ou=groups,ou=people,dc=example,dc=com

  @JsonProperty
  @NotEmpty
  private String groupFilter = ""; // ex. (memberUid=%s)

  @JsonProperty
  @NotEmpty
  private String groupNameAttribute = "cn";

  @JsonProperty
  @NotNull
  private Set<String> adminGroups = new HashSet<>();  // these are groups that can do everything

  @JsonProperty
  @NotNull
  private Set<String> requiredGroups = new HashSet<>();  // these are group(s) that any user must be a part of

  @JsonProperty
  private boolean stripUserEmailDomain = true;  // when authenticating, strip email domain from username

  @JsonProperty
  @Min(1)
  private long cacheExpirationMs = 5000;

  @JsonProperty
  @NotEmpty
  private String requestUserHeaderName = "X-Username";

  @JsonProperty
  private boolean poolTestOnBorrow = true;

  @JsonProperty
  private boolean poolTestOnReturn = true;

  @JsonProperty
  private boolean poolTestWhileIdle = true;

  @JsonProperty
  private int poolMaxActive = LdapConnectionPool.DEFAULT_MAX_ACTIVE;

  @JsonProperty
  private int poolMaxIdle = LdapConnectionPool.DEFAULT_MAX_IDLE;

  @JsonProperty
  private int poolMinIdle = LdapConnectionPool.DEFAULT_MIN_IDLE;

  @JsonProperty
  private long poolMaxWait = LdapConnectionPool.DEFAULT_MAX_WAIT;

  @Min(1)
  private int cacheThreads = 3;

  @JsonProperty
  @NotNull
  private LdapPoolWhenExhaustedAction poolWhenExhaustedAction = LdapPoolWhenExhaustedAction.BLOCK;

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getBindDn() {
    return bindDn;
  }

  public void setBindDn(String bindDn) {
    this.bindDn = bindDn;
  }

  public String getBindPassword() {
    return bindPassword;
  }

  public void setBindPassword(String bindPassword) {
    this.bindPassword = bindPassword;
  }

  public String getGroupBaseDN() {
    return groupBaseDN;
  }

  public void setGroupBaseDN(String groupBaseDN) {
    this.groupBaseDN = groupBaseDN;
  }

  public String getGroupFilter() {
    return groupFilter;
  }

  public void setGroupFilter(String groupFilter) {
    this.groupFilter = groupFilter;
  }

  public Set<String> getAdminGroups() {
    return adminGroups;
  }

  public void setAdminGroups(Set<String> adminGroups) {
    this.adminGroups = adminGroups;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getGroupNameAttribute() {
    return groupNameAttribute;
  }

  public void setGroupNameAttribute(String groupNameAttribute) {
    this.groupNameAttribute = groupNameAttribute;
  }

  public Set<String> getRequiredGroups() {
    return requiredGroups;
  }

  public void setRequiredGroups(Set<String> requiredGroups) {
    this.requiredGroups = requiredGroups;
  }

  public long getCacheExpirationMs() {
    return cacheExpirationMs;
  }

  public void setCacheExpirationMs(long cacheExpirationMs) {
    this.cacheExpirationMs = cacheExpirationMs;
  }

  public String getRequestUserHeaderName() {
    return requestUserHeaderName;
  }

  public void setRequestUserHeaderName(String requestUserHeaderName) {
    this.requestUserHeaderName = requestUserHeaderName;
  }

  public boolean isPoolTestOnBorrow() {
    return poolTestOnBorrow;
  }

  public void setPoolTestOnBorrow(boolean poolTestOnBorrow) {
    this.poolTestOnBorrow = poolTestOnBorrow;
  }

  public boolean isPoolTestOnReturn() {
    return poolTestOnReturn;
  }

  public void setPoolTestOnReturn(boolean poolTestOnReturn) {
    this.poolTestOnReturn = poolTestOnReturn;
  }

  public boolean isPoolTestWhileIdle() {
    return poolTestWhileIdle;
  }

  public void setPoolTestWhileIdle(boolean poolTestWhileIdle) {
    this.poolTestWhileIdle = poolTestWhileIdle;
  }

  public LdapPoolWhenExhaustedAction getPoolWhenExhaustedAction() {
    return poolWhenExhaustedAction;
  }

  public void setPoolWhenExhaustedAction(LdapPoolWhenExhaustedAction poolWhenExhaustedAction) {
    this.poolWhenExhaustedAction = poolWhenExhaustedAction;
  }

  public boolean isStripUserEmailDomain() {
    return stripUserEmailDomain;
  }

  public void setStripUserEmailDomain(boolean stripUserEmailDomain) {
    this.stripUserEmailDomain = stripUserEmailDomain;
  }

  public int getPoolMaxActive() {
    return poolMaxActive;
  }

  public void setPoolMaxActive(int poolMaxActive) {
    this.poolMaxActive = poolMaxActive;
  }

  public int getPoolMaxIdle() {
    return poolMaxIdle;
  }

  public void setPoolMaxIdle(int poolMaxIdle) {
    this.poolMaxIdle = poolMaxIdle;
  }

  public int getPoolMinIdle() {
    return poolMinIdle;
  }

  public void setPoolMinIdle(int poolMinIdle) {
    this.poolMinIdle = poolMinIdle;
  }

  public long getPoolMaxWait() {
    return poolMaxWait;
  }

  public void setPoolMaxWait(long poolMaxWait) {
    this.poolMaxWait = poolMaxWait;
  }

  public int getCacheThreads() {
    return cacheThreads;
  }

  public void setCacheThreads(int cacheThreads) {
    this.cacheThreads = cacheThreads;
  }

  public enum LdapPoolWhenExhaustedAction {
    BLOCK,
    FAIL,
    GROW
  }
}
