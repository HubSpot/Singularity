package com.hubspot.singularity.ldap;

import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.HttpHeaders;

import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionPool;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityLDAPManager {
  private final LdapConnectionPool connectionPool;
  private final SingularityConfiguration configuration;

  @Inject
  public SingularityLDAPManager(LdapConnectionPool connectionPool, SingularityConfiguration configuration) {
    this.connectionPool = connectionPool;
    this.configuration = configuration;
  }

  public final Set<String> getGroupsForUser(String user) {
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

  public Optional<String> getUserFromHeaders(HttpHeaders headers) {
    final List<String> userValues = headers.getRequestHeader(configuration.getLdapConfiguration().getRequestUserHeaderName());

    if (userValues.isEmpty()) {
      return Optional.absent();
    }

    return Optional.fromNullable(Strings.emptyToNull(userValues.get(0)));
  }
}
