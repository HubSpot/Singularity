package com.hubspot.singularity.auth;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hubspot.singularity.WebExceptions.checkForbidden;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Singleton
public class SingularityGroupsAuthorizer extends SingularityAuthorizer {
  private final ImmutableSet<String> adminGroups;
  private final ImmutableSet<String> requiredGroups;
  private final ImmutableSet<String> jitaGroups;
  private final ImmutableSet<String> defaultReadOnlyGroups;
  private final ImmutableSet<String> globalReadOnlyGroups;

  @Inject
  public SingularityGroupsAuthorizer(
    RequestManager requestManager,
    SingularityConfiguration configuration
  ) {
    super(requestManager, configuration.getAuthConfiguration().isEnabled());
    this.adminGroups = copyOf(configuration.getAuthConfiguration().getAdminGroups());
    this.requiredGroups =
      copyOf(configuration.getAuthConfiguration().getRequiredGroups());
    this.jitaGroups = copyOf(configuration.getAuthConfiguration().getJitaGroups());
    this.defaultReadOnlyGroups =
      copyOf(configuration.getAuthConfiguration().getDefaultReadOnlyGroups());
    this.globalReadOnlyGroups =
      copyOf(configuration.getAuthConfiguration().getGlobalReadOnlyGroups());
  }

  @Override
  public boolean hasAdminAuthorization(SingularityUser user) {
    // disabled auth == no rules!
    if (!authEnabled) {
      return true;
    }

    if (!user.isAuthenticated()) {
      return false;
    }

    // not authenticated, or no groups, or no admin groups == can't possibly be admin
    if (user.getGroups().isEmpty() || adminGroups.isEmpty()) {
      return false;
    }

    return groupsIntersect(user.getGroups(), adminGroups);
  }

  @Override
  public void checkAdminAuthorization(SingularityUser user) {
    if (authEnabled) {
      checkForbidden(user.isAuthenticated(), "Not Authenticated!");
      if (!adminGroups.isEmpty()) {
        checkForbidden(
          groupsIntersect(user.getGroups(), adminGroups),
          "%s must be part of one or more admin groups: %s",
          user.getId(),
          JavaUtils.COMMA_JOINER.join(adminGroups)
        );
      }
    }
  }

  @Override
  public void checkReadAuthorization(SingularityUser user) {
    if (authEnabled) {
      checkForbidden(user.isAuthenticated(), "Not Authenticated!");
      if (!adminGroups.isEmpty()) {
        final Set<String> userGroups = user.getGroups();
        final boolean userIsAdmin = groupsIntersect(userGroups, adminGroups);
        final boolean userIsJITA =
          !jitaGroups.isEmpty() && groupsIntersect(userGroups, jitaGroups);
        final boolean userIsReadOnlyUser =
          !globalReadOnlyGroups.isEmpty() &&
          groupsIntersect(userGroups, globalReadOnlyGroups);
        final boolean userIsPartOfRequiredGroups =
          requiredGroups.isEmpty() || groupsIntersect(userGroups, requiredGroups);
        if (!userIsAdmin) {
          checkForbidden(
            (userIsJITA || userIsReadOnlyUser) && userIsPartOfRequiredGroups,
            "%s must be part of one or more read only or jita groups: %s,%s",
            user.getId(),
            JavaUtils.COMMA_JOINER.join(jitaGroups),
            JavaUtils.COMMA_JOINER.join(globalReadOnlyGroups)
          );
        }
      }
    }
  }

  @Override
  public void checkUserInRequiredGroups(SingularityUser user) {
    if (authEnabled) {
      final Set<String> userGroups = user.getGroups();
      final boolean userIsAdmin =
        !adminGroups.isEmpty() && groupsIntersect(userGroups, adminGroups);
      final boolean userIsPartOfRequiredGroups =
        requiredGroups.isEmpty() || groupsIntersect(userGroups, requiredGroups);
      if (!userIsAdmin) {
        checkForbidden(
          userIsPartOfRequiredGroups,
          "%s must be part of one or more read only or jita groups: %s",
          user.getId(),
          JavaUtils.COMMA_JOINER.join(requiredGroups)
        );
      }
    }
  }

  @Override
  public boolean isAuthorizedForRequest(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    if (!authEnabled) {
      return true; // no auth == no rules!
    }

    if (!user.isAuthenticated()) {
      return false;
    }

    final Set<String> userGroups = user.getGroups();
    final Set<String> readWriteGroups = Sets.union(
      request.getGroup().map(Collections::singleton).orElse(Collections.emptySet()),
      request.getReadWriteGroups().orElse(Collections.emptySet())
    );
    final Set<String> readOnlyGroups = request
      .getReadOnlyGroups()
      .orElse(defaultReadOnlyGroups);

    final boolean userIsAdmin =
      !adminGroups.isEmpty() && groupsIntersect(userGroups, adminGroups);
    final boolean userIsJITA =
      !jitaGroups.isEmpty() && groupsIntersect(userGroups, jitaGroups);
    final boolean userIsReadWriteUser =
      readWriteGroups.isEmpty() || groupsIntersect(userGroups, readWriteGroups);
    final boolean userIsReadOnlyUser =
      groupsIntersect(userGroups, readOnlyGroups) ||
      (
        !globalReadOnlyGroups.isEmpty() &&
        groupsIntersect(userGroups, globalReadOnlyGroups)
      );
    final boolean userIsPartOfRequiredGroups =
      requiredGroups.isEmpty() || groupsIntersect(userGroups, requiredGroups);

    if (userIsAdmin) {
      return true; // Admins Rule Everything Around Me
    } else if (scope == SingularityAuthorizationScope.READ) {
      return (
        (userIsReadOnlyUser || userIsReadWriteUser || userIsJITA) &&
        userIsPartOfRequiredGroups
      );
    } else if (scope == SingularityAuthorizationScope.WRITE) {
      return (userIsReadWriteUser || userIsJITA) && userIsPartOfRequiredGroups;
    } else {
      return false;
    }
  }

  @Override
  public void checkForAuthorization(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    if (!authEnabled) {
      return;
    }

    checkForbidden(user.isAuthenticated(), "Not authenticated!");

    final Set<String> readWriteGroups = Sets.union(
      request.getGroup().map(Collections::singleton).orElse(Collections.emptySet()),
      request.getReadWriteGroups().orElse(Collections.emptySet())
    );
    final Set<String> readOnlyGroups = request
      .getReadOnlyGroups()
      .orElse(defaultReadOnlyGroups);

    checkForAuthorization(
      user,
      readWriteGroups,
      readOnlyGroups,
      scope,
      Optional.of(request.getId())
    );
  }

  private void checkForAuthorization(
    SingularityUser user,
    Set<String> readWriteGroups,
    Set<String> readOnlyGroups,
    SingularityAuthorizationScope scope,
    Optional<String> requestId
  ) {
    final Set<String> userGroups = user.getGroups();

    final boolean userIsAdmin =
      !adminGroups.isEmpty() && groupsIntersect(userGroups, adminGroups);
    final boolean userIsJITA =
      !jitaGroups.isEmpty() && groupsIntersect(userGroups, jitaGroups);
    final boolean userIsReadWriteUser =
      readWriteGroups.isEmpty() || groupsIntersect(userGroups, readWriteGroups);
    final boolean userIsReadOnlyUser =
      groupsIntersect(userGroups, readOnlyGroups) ||
      (
        !globalReadOnlyGroups.isEmpty() &&
        groupsIntersect(userGroups, globalReadOnlyGroups)
      );
    final boolean userIsPartOfRequiredGroups =
      requiredGroups.isEmpty() || groupsIntersect(userGroups, requiredGroups);

    if (userIsAdmin) {
      return; // Admins Rule Everything Around Me
    }

    checkForbidden(
      userIsPartOfRequiredGroups,
      "%s must be a member of one or more required groups: %s",
      user.getId(),
      JavaUtils.COMMA_JOINER.join(requiredGroups)
    );

    if (scope == SingularityAuthorizationScope.READ) {
      checkForbidden(
        userIsReadOnlyUser || userIsReadWriteUser || userIsJITA,
        "%s must be a member of one or more groups to %s %s: %s",
        user.getId(),
        scope.name(),
        requestId,
        JavaUtils.COMMA_JOINER.join(
          Iterables.concat(readOnlyGroups, readWriteGroups, jitaGroups)
        )
      );
    } else if (scope == SingularityAuthorizationScope.WRITE) {
      checkForbidden(
        userIsReadWriteUser || userIsJITA,
        "%s must be a member of one or more groups to %s %s: %s",
        user.getId(),
        scope.name(),
        requestId,
        JavaUtils.COMMA_JOINER.join(Iterables.concat(readWriteGroups, jitaGroups))
      );
    } else if (scope == SingularityAuthorizationScope.ADMIN) {
      checkForbidden(
        userIsAdmin,
        "%s must be a member of one or more groups to %s %s: %s",
        user.getId(),
        scope.name(),
        requestId,
        JavaUtils.COMMA_JOINER.join(adminGroups)
      );
    }
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

    if (
      !oldRequest.getReadWriteGroups().equals(request.getReadWriteGroups()) ||
      !oldRequest.getGroup().equals(request.getGroup())
    ) {
      // If group or readWriteGroups are changing, a user must be authorized for both the old and new request groups
      checkForAuthorization(oldRequest, user, SingularityAuthorizationScope.WRITE);
      checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);
    }
  }
}
