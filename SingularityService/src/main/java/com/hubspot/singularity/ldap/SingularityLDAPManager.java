package com.hubspot.singularity.ldap;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionPool;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityLDAPManager {
  private final LdapConnectionPool connectionPool;
  private final SingularityConfiguration configuration;
  private final LoadingCache<String, Set<String>> userGroupCache;

  @Inject
  public SingularityLDAPManager(LdapConnectionPool connectionPool, SingularityConfiguration configuration) {
    this.connectionPool = connectionPool;
    this.configuration = configuration;

    this.userGroupCache = CacheBuilder.newBuilder()
            .expireAfterWrite(configuration.getLdapConfiguration().getCacheExpirationMs(), TimeUnit.MILLISECONDS)
            .build(new LDAPGroupCacheLoader());
  }

  public Set<String> getGroupsForUser(String user) {
    return userGroupCache.getUnchecked(user);
  }

  private Set<String> getGroupsForUserFromLDAP(String user) {
    if (!configuration.getLdapConfiguration().isEnabled()) {
      return Collections.emptySet();
    }

    final Set<String> groups = new HashSet<>();

    try {
      final LdapConnection connection = connectionPool.getConnection();
      checkState(connection.isConnected(), "not connected");
      checkState(connection.isAuthenticated(), "not authenticated");
      connection.bind();

      final EntryCursor cursor = connection.search(configuration.getLdapConfiguration().getGroupBaseDN(),
              String.format(configuration.getLdapConfiguration().getGroupFilter(), user),
              SearchScope.ONELEVEL, configuration.getLdapConfiguration().getGroupNameAttribute());

      while (cursor.next()) {
        groups.add(cursor.get().get(configuration.getLdapConfiguration().getGroupNameAttribute()).getString());
      }

      connection.unBind();

      return groups;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private class LDAPGroupCacheLoader extends CacheLoader<String, Set<String>> {
    @Override
    public Set<String> load(String key) throws Exception {
      return getGroupsForUserFromLDAP(key);
    }
  }
}
