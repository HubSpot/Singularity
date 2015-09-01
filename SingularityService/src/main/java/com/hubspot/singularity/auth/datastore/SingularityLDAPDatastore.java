package com.hubspot.singularity.auth.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.LDAPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityLDAPDatastore implements SingularityAuthDatastore {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityLDAPDatastore.class);

  private final LdapConnectionPool connectionPool;
  private final LDAPConfiguration configuration;

  private static LdapConnectionPool createConnectionPool(LDAPConfiguration configuration) throws IOException {
    final LdapConnectionConfig config = new LdapConnectionConfig();
    config.setLdapHost(configuration.getHostname());
    config.setLdapPort(configuration.getPort());
    config.setName(configuration.getBindDn());
    config.setCredentials(configuration.getBindPassword());

    final DefaultPoolableLdapConnectionFactory factory = new DefaultPoolableLdapConnectionFactory(config);

    final LdapConnectionPool pool = new LdapConnectionPool(factory);
    pool.setTestOnBorrow(configuration.isPoolTestOnBorrow());
    pool.setTestOnReturn(configuration.isPoolTestOnReturn());
    pool.setTestWhileIdle(configuration.isPoolTestWhileIdle());

    pool.setMaxActive(configuration.getPoolMaxActive());
    pool.setMaxIdle(configuration.getPoolMaxIdle());
    pool.setMinIdle(configuration.getPoolMinIdle());
    pool.setMaxWait(configuration.getPoolMaxWait());

    switch (configuration.getPoolWhenExhaustedAction()) {
      case BLOCK:
        pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_BLOCK);
        break;
      case FAIL:
        pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_FAIL);
        break;
      case GROW:
        pool.setWhenExhaustedAction(LdapConnectionPool.WHEN_EXHAUSTED_GROW);
        break;
      default:
        pool.setWhenExhaustedAction(LdapConnectionPool.DEFAULT_WHEN_EXHAUSTED_ACTION);
    }

    return pool;
  }

  @Inject
  public SingularityLDAPDatastore(SingularityConfiguration configuration) throws IOException {
    checkArgument(configuration.getLdapConfiguration().isPresent(), "LDAP configuration not present");

    this.connectionPool = createConnectionPool(configuration.getLdapConfiguration().get());
    this.configuration = configuration.getLdapConfiguration().get();
  }

  @Override
  public Optional<Boolean> isHealthy() {
    try {
      final LdapConnection connection = connectionPool.getConnection();

      try {
        if (connection.isConnected() && connection.isAuthenticated()) {
          connection.bind();
          try {
            return Optional.of(true);
          } finally {
            connection.unBind();
          }
        }
      } finally {
        connectionPool.releaseConnection(connection);
      }
    } catch (LdapException e) {
      LOG.warn("LdapException caught when checking health", e);
    }
    return Optional.of(false);
  }

  @Override
  public List<SingularityUser> getUsers() {
    final Multimap<String, String> userToGroup = ArrayListMultimap.create();
    final List<SingularityUser> users = new ArrayList<>();

    try {
      final LdapConnection connection = connectionPool.getConnection();

      try {
        checkState(connection.isConnected(), "not connected");
        checkState(connection.isAuthenticated(), "not authenticated");
        connection.bind();

        try {
          final EntryCursor groupCursor = connection.search(configuration.getGroupBaseDN(), configuration.getValidGroupFilter(), SearchScope.SUBTREE, configuration.getGroupNameAttribute(), configuration.getGroupMemberAttribute());

          while (groupCursor.next()) {
            final Entry groupEntry = groupCursor.get();

            final String groupName = groupEntry.get(configuration.getGroupNameAttribute()).getString();

            if (groupEntry.containsAttribute(configuration.getGroupMemberAttribute())) {
              for (Value<?> userId : groupEntry.get(configuration.getGroupMemberAttribute())) {
                userToGroup.put(userId.getString(), groupName);
              }
            }
          }

          final EntryCursor userCursor = connection.search(configuration.getUserBaseDN(), configuration.getValidUserFilter(), SearchScope.ONELEVEL, configuration.getUserIdAttribute(), configuration.getUserNameAttribute(), configuration.getUserEmailAttribute());

          while (userCursor.next()) {
            final Entry userEntry = userCursor.get();

            final String userId = userEntry.get(configuration.getUserIdAttribute()).getString();

            final Set<String> groups = userToGroup.containsKey(userId) ? new HashSet<>(userToGroup.get(userId)) : Collections.<String>emptySet();

            users.add(new SingularityUser(userId, Optional.fromNullable(Strings.emptyToNull(userEntry.get(configuration.getUserNameAttribute()).getString())), Optional.fromNullable(Strings.emptyToNull(userEntry.get(configuration.getUserEmailAttribute()).getString())), groups, Optional.of(System.currentTimeMillis())));
          }
        } finally {
          connection.unBind();
        }
      } finally {
        connectionPool.releaseConnection(connection);
      }
    } catch (LdapException | CursorException e) {
      throw Throwables.propagate(e);
    }

    return users;
  }

  @Override
  public Optional<SingularityUser> getUser(String user) {
    if (configuration.isStripUserEmailDomain()) {
      user = user.split("@")[0];
    }

    final Set<String> groups = new HashSet<>();

    try {
      final LdapConnection connection = connectionPool.getConnection();

      try {
        checkState(connection.isConnected(), "not connected");
        checkState(connection.isAuthenticated(), "not authenticated");
        connection.bind();

        try {
          final EntryCursor userCursor = connection.search(configuration.getUserBaseDN(),
                  String.format(configuration.getUserFilter(), user),
                  SearchScope.ONELEVEL, configuration.getUserNameAttribute(), configuration.getUserEmailAttribute());

          if (!userCursor.next()) {
            return Optional.absent();
          }

          final Entry userEntry = userCursor.get();

          // get group info
          final EntryCursor cursor = connection.search(configuration.getGroupBaseDN(),
                  String.format(configuration.getGroupFilter(), user),
                  SearchScope.SUBTREE, configuration.getGroupNameAttribute());

          while (cursor.next()) {
            groups.add(cursor.get().get(configuration.getGroupNameAttribute()).getString());
          }

          return Optional.of(new SingularityUser(user,Optional.fromNullable(Strings.emptyToNull(userEntry.get(configuration.getUserNameAttribute()).getString())), Optional.fromNullable(Strings.emptyToNull(userEntry.get(configuration.getUserEmailAttribute()).getString())), groups, Optional.of(System.currentTimeMillis())));
        } finally {
          connection.unBind();
        }
      } finally {
        connectionPool.releaseConnection(connection);
      }
    } catch (LdapException | CursorException e) {
      throw Throwables.propagate(e);
    }
  }
}
