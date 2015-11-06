package com.hubspot.singularity.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;


public class LDAPConfiguration {
  //
  // LDAP CONNECTION
  //
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

  //
  // LDAP USER LOOKUP
  //
  @JsonProperty
  @NotEmpty
  private String userBaseDN = "";  // ex. ou=users,ou=people,dc=example,dc=com

  @JsonProperty
  @NotEmpty
  private String userFilter = "(uid=%s)";

  @JsonProperty
  @NotEmpty
  private String userNameAttribute = "cn";

  @JsonProperty
  @NotEmpty
  private String userEmailAttribute = "mail";

  //
  // LDAP GROUP LOOKUP
  //
  @JsonProperty
  @NotEmpty
  private String groupBaseDN = "";  // ex. ou=groups,ou=people,dc=example,dc=com

  @JsonProperty
  @NotEmpty
  private String groupFilter = "(memberUid=%s)";

  @JsonProperty
  @NotEmpty
  private String groupNameAttribute = "cn";

  @JsonProperty
  @NotNull
  private SearchScope groupSearchScope = SearchScope.SUBTREE;

  //
  // LDAP CONNECTION POOL
  //
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

  @JsonProperty
  @NotNull
  private LdapPoolWhenExhaustedAction poolWhenExhaustedAction = LdapPoolWhenExhaustedAction.BLOCK;

  //
  // MISC.
  //
  @JsonProperty
  private boolean stripUserEmailDomain = true;  // if true, tpetr@hubspot.com --> tpetr

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

  public String getUserBaseDN() {
    return userBaseDN;
  }

  public void setUserBaseDN(String userBaseDN) {
    this.userBaseDN = userBaseDN;
  }

  public String getUserFilter() {
    return userFilter;
  }

  public void setUserFilter(String userFilter) {
    this.userFilter = userFilter;
  }

  public String getUserNameAttribute() {
    return userNameAttribute;
  }

  public void setUserNameAttribute(String userNameAttribute) {
    this.userNameAttribute = userNameAttribute;
  }

  public String getUserEmailAttribute() {
    return userEmailAttribute;
  }

  public void setUserEmailAttribute(String userEmailAttribute) {
    this.userEmailAttribute = userEmailAttribute;
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

  public String getGroupNameAttribute() {
    return groupNameAttribute;
  }

  public void setGroupNameAttribute(String groupNameAttribute) {
    this.groupNameAttribute = groupNameAttribute;
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

  public SearchScope getGroupSearchScope() {
    return groupSearchScope;
  }

  public void setGroupSearchScope(SearchScope groupSearchScope) {
    this.groupSearchScope = groupSearchScope;
  }

  public enum LdapPoolWhenExhaustedAction {
    BLOCK,
    FAIL,
    GROW
  }
}
