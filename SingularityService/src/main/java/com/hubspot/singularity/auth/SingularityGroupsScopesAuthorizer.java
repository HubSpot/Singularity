package com.hubspot.singularity.auth;

import static com.hubspot.singularity.WebExceptions.checkForbidden;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.ScopesConfiguration;
import com.hubspot.singularity.data.RequestManager;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SingularityGroupsScopesAuthorizer extends SingularityAuthorizer {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityGroupsScopesAuthorizer.class
  );

  private final AuthConfiguration authConfiguration;
  private final ScopesConfiguration scopesConfiguration;

  @Inject
  public SingularityGroupsScopesAuthorizer(
    RequestManager requestManager,
    boolean authEnabled,
    AuthConfiguration authConfiguration
  ) {
    super(requestManager, authEnabled);
    this.authConfiguration = authConfiguration;
    this.scopesConfiguration = authConfiguration.getScopes();
  }

  @Override
  public boolean hasAdminAuthorization(SingularityUser user) {
    return !authConfiguration.isEnabled() || (user.isAuthenticated() && isAdmin(user));
  }

  @Override
  public void checkAdminAuthorization(SingularityUser user) {
    if (!authConfiguration.isEnabled()) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    checkForbidden(
      isAdmin(user),
      "%s must be part of one or more groups: %s",
      user.getId(),
      scopesConfiguration.getAdmin()
    );
  }

  @Override
  public void checkReadAuthorization(SingularityUser user) {
    if (!authConfiguration.isEnabled() || isAdmin(user)) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    checkReadScope(user);
  }

  @Override
  public void checkForAuthorization(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    if (!authConfiguration.isEnabled() || isAdmin(user)) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    switch (scope) {
      case READ:
        Set<String> allowedReadGroups = request
          .getReadOnlyGroups()
          .orElseGet(authConfiguration::getDefaultReadOnlyGroups);
        checkForbiddenForGroups(user, allowedReadGroups);
        checkReadScope(user);
        break;
      case WRITE:
        Set<String> allowedWriteGroups = request
          .getReadWriteGroups()
          .orElseGet(HashSet::new);
        request.getGroup().ifPresent(allowedWriteGroups::add);
        if (allowedWriteGroups.isEmpty()) {
          LOG.warn("No write-enabled groups set for {}", request.getId());
          return;
        }
        checkForbiddenForGroups(user, allowedWriteGroups);
        checkWriteScope(user);
        break;
      case ADMIN:
      default:
        checkAdminAuthorization(user);
    }
  }

  @Override
  public boolean isAuthorizedForRequest(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    if (!authConfiguration.isEnabled() || isAdmin(user)) {
      return true;
    }
    if (!user.isAuthenticated()) {
      return false;
    }
    if (isAdmin(user)) {
      return true;
    }
    switch (scope) {
      case READ:
        Set<String> allowedReadGroups = request
          .getReadOnlyGroups()
          .orElseGet(authConfiguration::getDefaultReadOnlyGroups);
        return groupsIntersect(allowedReadGroups, user.getGroups()) && hasReadScope(user);
      case WRITE:
        Set<String> allowedWriteGroups = request
          .getReadWriteGroups()
          .orElseGet(HashSet::new);
        request.getGroup().ifPresent(allowedWriteGroups::add);
        if (allowedWriteGroups.isEmpty()) {
          LOG.warn("No write-enabled groups set for {}", request.getId());
          return true;
        }
        return (
          groupsIntersect(allowedWriteGroups, user.getGroups()) && hasWriteScope(user)
        );
      case ADMIN:
      default:
        return hasAdminAuthorization(user);
    }
  }

  @Override
  public void checkForAuthorizedChanges(
    SingularityRequest request,
    SingularityRequest oldRequest,
    SingularityUser user
  ) {
    if (!authConfiguration.isEnabled()) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    if (isAdmin(user)) {
      return;
    }
    if (
      !oldRequest.getReadWriteGroups().equals(request.getReadWriteGroups()) ||
      !oldRequest.getGroup().equals(request.getGroup())
    ) {
      // If group or readWriteGroups are changing, a user must be authorized for both the old and new request groups
      checkForAuthorization(oldRequest, user, SingularityAuthorizationScope.WRITE);
      checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);
    }
  }

  private boolean isAdmin(SingularityUser user) {
    return groupsIntersect(scopesConfiguration.getAdmin(), user.getScopes());
  }

  private boolean hasReadScope(SingularityUser user) {
    return groupsIntersect(scopesConfiguration.getRead(), user.getScopes());
  }

  private void checkReadScope(SingularityUser user) {
    checkForbidden(
      hasReadScope(user),
      "%s must have one or more scopes to READ: %s",
      user.getId(),
      scopesConfiguration.getRead()
    );
  }

  private boolean hasWriteScope(SingularityUser user) {
    return groupsIntersect(scopesConfiguration.getRead(), user.getScopes());
  }

  private void checkWriteScope(SingularityUser user) {
    checkForbidden(
      hasWriteScope(user),
      "%s must have one or more scopes to WRITE: %s",
      user.getId(),
      scopesConfiguration.getWrite()
    );
  }

  private void checkForbiddenForGroups(SingularityUser user, Set<String> allowedGroups) {
    checkForbidden(
      groupsIntersect(allowedGroups, user.getGroups()),
      "%s must be part of one or more groups: %s",
      user.getId(),
      allowedGroups
    );
  }
}
