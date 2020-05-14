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
        Set<String> allowedReadGroups = getReadGroups(request);
        checkForbiddenForGroups(user, allowedReadGroups);
        checkReadScope(user);
        break;
      case WRITE:
        Set<String> allowedWriteGroups = getWriteGroups(request);
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
        return (
          groupsIntersect(getReadGroups(request), user.getGroups()) && hasReadScope(user)
        );
      case WRITE:
        return (
          groupsIntersect(getWriteGroups(request), user.getGroups()) &&
          hasWriteScope(user)
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
    return (
      groupsIntersect(scopesConfiguration.getRead(), user.getScopes()) ||
      groupsIntersect(scopesConfiguration.getWrite(), user.getScopes())
    );
  }

  private void checkReadScope(SingularityUser user) {
    checkForbidden(
      hasReadScope(user),
      "%s must have one or more scopes to READ: %s, %s",
      user.getId(),
      scopesConfiguration.getRead(),
      scopesConfiguration.getWrite()
    );
  }

  private boolean hasWriteScope(SingularityUser user) {
    return groupsIntersect(scopesConfiguration.getWrite(), user.getScopes());
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

  private Set<String> getReadGroups(SingularityRequest request) {
    Set<String> allowedReadGroups = new HashSet<>();
    request.getReadOnlyGroups().ifPresent(allowedReadGroups::addAll);
    request.getReadWriteGroups().ifPresent(allowedReadGroups::addAll);
    request.getGroup().ifPresent(allowedReadGroups::add);
    if (allowedReadGroups.isEmpty()) {
      LOG.warn("No read-enabled groups set for {}", request.getId());
    }
    return allowedReadGroups;
  }

  private Set<String> getWriteGroups(SingularityRequest request) {
    Set<String> allowedWriteGroups = new HashSet<>();
    request.getReadWriteGroups().ifPresent(allowedWriteGroups::addAll);
    request.getGroup().ifPresent(allowedWriteGroups::add);
    if (allowedWriteGroups.isEmpty()) {
      LOG.warn("No read/write-enabled groups set for {}", request.getId());
    }
    return allowedWriteGroups;
  }
}
