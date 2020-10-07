package com.hubspot.singularity.auth;

import static com.hubspot.singularity.WebExceptions.checkForbidden;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserFacingAction;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.config.ScopesConfiguration;
import com.hubspot.singularity.data.RequestManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
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
    AuthConfiguration authConfiguration
  ) {
    super(requestManager, authConfiguration.isEnabled());
    this.authConfiguration = authConfiguration;
    this.scopesConfiguration = authConfiguration.getScopes();
  }

  @Override
  public boolean hasAdminAuthorization(SingularityUser user) {
    return !authEnabled || (user.isAuthenticated() && isAdmin(user));
  }

  @Override
  public void checkAdminAuthorization(SingularityUser user) {
    if (!authEnabled) {
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
    if (!authEnabled || isAdmin(user)) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    checkScope(user, SingularityAuthorizationScope.READ);
  }

  @Override
  public void checkGlobalReadAuthorization(SingularityUser user) {
    if (!authEnabled || isAdmin(user)) {
      return;
    }
    Set<String> allowedReadGroups = new HashSet<>(
      authConfiguration.getGlobalReadWriteGroups()
    );
    allowedReadGroups.addAll(authConfiguration.getGlobalReadOnlyGroups());

    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    checkForbiddenForGroups(
      user,
      allowedReadGroups,
      "all",
      SingularityAuthorizationScope.READ
    );
    checkScope(user, SingularityAuthorizationScope.READ);
  }

  @Override
  protected void checkForAuthorization(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope,
    Optional<SingularityUserFacingAction> action
  ) {
    if (!authEnabled || isAdmin(user)) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");

    if (action.isPresent() && request.getActionPermissions().isPresent()) {
      checkForbidden(
        isActionAllowed(request, user, action.get()),
        String.format("%s not allowed", action.get())
      );
    }
    switch (scope) {
      case READ:
      case WRITE:
        Set<String> allowedGroups = getGroups(request, scope);
        checkForbiddenForGroups(user, allowedGroups, request.getId(), scope);
        checkScope(user, scope);
        break;
      case ADMIN:
      default:
        checkAdminAuthorization(user);
    }
  }

  @Override
  protected boolean isAuthorizedForRequest(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope,
    Optional<SingularityUserFacingAction> action
  ) {
    if (!authEnabled || isAdmin(user)) {
      return true;
    }
    if (!user.isAuthenticated()) {
      return false;
    }
    if (action.isPresent() && request.getActionPermissions().isPresent()) {
      if (!isActionAllowed(request, user, action.get())) {
        return false;
      }
    }
    switch (scope) {
      case READ:
      case WRITE:
        if (!hasScope(user, scope)) {
          return false;
        }
        if (groupsIntersect(getGroups(request, scope), user.getGroups())) {
          return true;
        }
        if (isJita(user)) {
          warnJita(user, scope, request.getId());
          return true;
        }
        return false;
      case ADMIN:
      default:
        return hasAdminAuthorization(user);
    }
  }

  private boolean isActionAllowed(
    SingularityRequest request,
    SingularityUser user,
    SingularityUserFacingAction action
  ) {
    for (String group : user.getGroups()) {
      if (
        request
          .getActionPermissions()
          .get()
          .getOrDefault(group, Collections.emptySet())
          .contains(action)
      ) {
        return true;
      }
    }
    return false;
  }

  private void warnJita(
    SingularityUser user,
    SingularityAuthorizationScope scope,
    String id
  ) {
    LOG.warn(
      "JITA ACTION - User: {}, RequestId: {}, Scope Used: {}",
      user.getId(),
      id,
      scope
    );
  }

  @Override
  public void checkForAuthorizedChanges(
    SingularityRequest request,
    SingularityRequest oldRequest,
    SingularityUser user
  ) {
    if (!authEnabled) {
      return;
    }
    checkForbidden(user.isAuthenticated(), "Not Authenticated!");
    if (isAdmin(user)) {
      return;
    }
    if (
      !oldRequest.getReadWriteGroups().equals(request.getReadWriteGroups()) ||
      !oldRequest.getReadOnlyGroups().equals(request.getReadOnlyGroups()) ||
      !oldRequest.getGroup().equals(request.getGroup())
    ) {
      // If group or readWriteGroups are changing, a user must be authorized for at least the old one
      checkForAuthorization(oldRequest, user, SingularityAuthorizationScope.WRITE);
    }
  }

  private boolean isJita(SingularityUser user) {
    return groupsIntersect(authConfiguration.getJitaGroups(), user.getGroups());
  }

  private boolean isAdmin(SingularityUser user) {
    return hasScope(user, SingularityAuthorizationScope.ADMIN);
  }

  private Set<String> getScopes(SingularityAuthorizationScope scope) {
    switch (scope) {
      case READ:
        return Sets.union(scopesConfiguration.getRead(), scopesConfiguration.getWrite());
      case WRITE:
        return scopesConfiguration.getWrite();
      case ADMIN:
      default:
        return scopesConfiguration.getAdmin();
    }
  }

  private boolean hasScope(SingularityUser user, SingularityAuthorizationScope scope) {
    return groupsIntersect(user.getScopes(), getScopes(scope));
  }

  private void checkScope(SingularityUser user, SingularityAuthorizationScope scope) {
    checkForbidden(
      hasScope(user, scope),
      "%s must have one or more scopes to %s: %s",
      user.getId(),
      scope.name(),
      getScopes(scope)
    );
  }

  private void checkForbiddenForGroups(
    SingularityUser user,
    Set<String> allowedGroups,
    String requestId,
    SingularityAuthorizationScope scope
  ) {
    boolean inNormalGroups = groupsIntersect(allowedGroups, user.getGroups());
    if (inNormalGroups) {
      return;
    }
    boolean inJitaGroups = groupsIntersect(
      authConfiguration.getJitaGroups(),
      user.getGroups()
    );
    if (inJitaGroups) {
      warnJita(user, scope, requestId);
    }
    checkForbidden(
      inJitaGroups,
      "%s must be part of one or more groups: %s",
      user.getId(),
      allowedGroups
    );
  }

  private Set<String> getGroups(
    SingularityRequest request,
    SingularityAuthorizationScope scope
  ) {
    switch (scope) {
      case READ:
        return getReadGroups(request);
      case WRITE:
        return getWriteGroups(request);
      case ADMIN:
      default:
        return Collections.emptySet();
    }
  }

  private Set<String> getReadGroups(SingularityRequest request) {
    Set<String> allowedReadGroups = new HashSet<>(
      authConfiguration.getGlobalReadOnlyGroups()
    );
    allowedReadGroups.addAll(authConfiguration.getGlobalReadWriteGroups());

    if (!request.getReadOnlyGroups().isPresent()) {
      allowedReadGroups.addAll(authConfiguration.getDefaultReadOnlyGroups());
    }
    request.getReadOnlyGroups().ifPresent(allowedReadGroups::addAll);
    request.getReadWriteGroups().ifPresent(allowedReadGroups::addAll);

    request.getGroup().ifPresent(allowedReadGroups::add);

    if (allowedReadGroups.isEmpty()) {
      LOG.warn("No read-enabled groups set for {}", request.getId());
    }
    return allowedReadGroups;
  }

  private Set<String> getWriteGroups(SingularityRequest request) {
    Set<String> allowedWriteGroups = new HashSet<>(
      authConfiguration.getGlobalReadWriteGroups()
    );

    request.getReadWriteGroups().ifPresent(allowedWriteGroups::addAll);
    request.getGroup().ifPresent(allowedWriteGroups::add);

    // If one of the above is also set as a read-only group, assume the strictest
    // possible meaning and disallow writing.
    request.getReadOnlyGroups().ifPresent(allowedWriteGroups::removeAll);
    if (allowedWriteGroups.isEmpty()) {
      LOG.warn("No read/write-enabled groups set for {}", request.getId());
    }
    return allowedWriteGroups;
  }
}
