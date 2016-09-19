package com.hubspot.singularity.auth.datastore;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
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
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.LDAPConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

@Singleton
public class SingularityLDAPDatastore implements SingularityAuthDatastore {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityLDAPDatastore.class);

  private final LdapConnectionPool connectionPool;
  private final LDAPConfiguration configuration;
  private final SingularityExceptionNotifier exceptionNotifier;

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
  public SingularityLDAPDatastore(SingularityConfiguration configuration,
                                  SingularityExceptionNotifier exceptionNotifier) throws IOException {
    checkArgument(configuration.getLdapConfiguration().isPresent(), "LDAP configuration not present");

    this.connectionPool = createConnectionPool(configuration.getLdapConfiguration().get());
    this.configuration = configuration.getLdapConfiguration().get();
    this.exceptionNotifier = exceptionNotifier;
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
      exceptionNotifier.notify(String.format("LdapException caught when checking health (%s)", e.getMessage()), e);
    }
    return Optional.of(false);
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

        final long startTime = System.currentTimeMillis();
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
                  configuration.getGroupSearchScope(), configuration.getGroupNameAttribute());

          while (cursor.next()) {
            groups.add(cursor.get().get(configuration.getGroupNameAttribute()).getString());
          }

          return Optional.of(new SingularityUser(user, Optional.fromNullable(Strings.emptyToNull(userEntry.get(configuration.getUserNameAttribute()).getString())), Optional.fromNullable(Strings.emptyToNull(userEntry.get(configuration.getUserEmailAttribute()).getString())), groups));
        } finally {
          LOG.info("Loaded {}'s user data in {}ms", user, System.currentTimeMillis() - startTime);
          connection.unBind();
        }
      } finally {
        connectionPool.releaseConnection(connection);
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
