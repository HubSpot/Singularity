package com.hubspot.singularity.auth;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkForbidden;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.RequestManager;

@Singleton
public class SingularityAuthorizationHelper {
  private final RequestManager requestManager;
  private final ImmutableSet<String> adminGroups;
  private final ImmutableSet<String> requiredGroups;
  private final ImmutableSet<String> jitaGroups;
  private final ImmutableSet<String> defaultReadOnlyGroups;
  private final ImmutableSet<String> globalReadOnlyGroups;
  private final boolean authEnabled;

  @Inject
  public SingularityAuthorizationHelper(RequestManager requestManager, SingularityConfiguration configuration) {
    this.requestManager = requestManager;
    this.adminGroups = copyOf(configuration.getAuthConfiguration().getAdminGroups());
    this.requiredGroups = copyOf(configuration.getAuthConfiguration().getRequiredGroups());
    this.jitaGroups = copyOf(configuration.getAuthConfiguration().getJitaGroups());
    this.defaultReadOnlyGroups = copyOf(configuration.getAuthConfiguration().getDefaultReadOnlyGroups());
    this.globalReadOnlyGroups = copyOf(configuration.getAuthConfiguration().getGlobalReadOnlyGroups());
    this.authEnabled = configuration.getAuthConfiguration().isEnabled();
  }

  public static boolean groupsIntersect(Set<String> a, Set<String> b) {
    return !Sets.intersection(a, b).isEmpty();
  }

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

  public void checkAdminAuthorization(SingularityUser user) {
    if (authEnabled) {
      checkForbidden(user.isAuthenticated(), "Not Authenticated!");
      if (!adminGroups.isEmpty()) {
        checkForbidden(groupsIntersect(user.getGroups(), adminGroups), "%s must be part of one or more admin groups: %s", user.getId(), JavaUtils.COMMA_JOINER.join(adminGroups));
      }
    }
  }

  public void checkForAuthorizationByTaskId(String taskId, SingularityUser user, SingularityAuthorizationScope scope) {
    if (authEnabled) {
      checkForbidden(user.isAuthenticated(), "Not Authenticated!");
      try {
        final SingularityTaskId taskIdObj = SingularityTaskId.valueOf(taskId);

        final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(taskIdObj.getRequestId());

        if (maybeRequest.isPresent()) {
          checkForAuthorization(maybeRequest.get().getRequest(), user, scope);
        }
      } catch (InvalidSingularityTaskIdException e) {
        badRequest(e.getMessage());
      }
    }
  }

  public void checkForAuthorizationByRequestId(String requestId, SingularityUser user, SingularityAuthorizationScope scope) {
    if (authEnabled) {

      final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(requestId);

      if (maybeRequest.isPresent()) {
        checkForAuthorization(maybeRequest.get().getRequest(), user, scope);
      }
    }
  }

  public boolean isAuthorizedForRequest(SingularityRequest request, SingularityUser user, SingularityAuthorizationScope scope) {
    if (!authEnabled) {
      return true;  // no auth == no rules!
    }

    if (!user.isAuthenticated()) {
      return false;
    }

    final Set<String> userGroups = user.getGroups();
    final Set<String> readWriteGroups = Sets.union(request.getGroup().asSet(), request.getReadWriteGroups().or(Collections.<String>emptySet()));
    final Set<String> readOnlyGroups = request.getReadOnlyGroups().or(defaultReadOnlyGroups);

    final boolean userIsAdmin = !adminGroups.isEmpty() && groupsIntersect(userGroups, adminGroups);
    final boolean userIsJITA = !jitaGroups.isEmpty() && groupsIntersect(userGroups, jitaGroups);
    final boolean userIsReadWriteUser = readWriteGroups.isEmpty() || groupsIntersect(userGroups, readWriteGroups);
    final boolean userIsReadOnlyUser = groupsIntersect(userGroups, readOnlyGroups) || (!globalReadOnlyGroups.isEmpty() && groupsIntersect(userGroups, globalReadOnlyGroups));
    final boolean userIsPartOfRequiredGroups = requiredGroups.isEmpty() || groupsIntersect(userGroups, requiredGroups);

    if (userIsAdmin) {
      return true;  // Admins Rule Everything Around Me
    } else if (scope == SingularityAuthorizationScope.READ) {
      return (userIsReadOnlyUser || userIsReadWriteUser || userIsJITA) && userIsPartOfRequiredGroups;
    } else if (scope == SingularityAuthorizationScope.WRITE) {
      return (userIsReadWriteUser || userIsJITA) && userIsPartOfRequiredGroups;
    } else {
      return false;
    }
  }

  public void checkForAuthorization(SingularityRequest request, SingularityUser user, SingularityAuthorizationScope scope) {
    if (!authEnabled) {
      return;
    }

    checkForbidden(user.isAuthenticated(), "Not authenticated!");

    final Set<String> readWriteGroups = Sets.union(request.getGroup().asSet(), request.getReadWriteGroups().or(Collections.emptySet()));
    final Set<String> readOnlyGroups = request.getReadOnlyGroups().or(defaultReadOnlyGroups);

    checkForAuthorization(user, readWriteGroups, readOnlyGroups, scope, Optional.of(request.getId()));
  }

  public void checkForAuthorization(SingularityUser user, Set<String> readWriteGroups, Set<String> readOnlyGroups, SingularityAuthorizationScope scope, Optional<String> requestId) {
    final Set<String> userGroups = user.getGroups();

    final boolean userIsAdmin = !adminGroups.isEmpty() && groupsIntersect(userGroups, adminGroups);
    final boolean userIsJITA = !jitaGroups.isEmpty() && groupsIntersect(userGroups, jitaGroups);
    final boolean userIsReadWriteUser = readWriteGroups.isEmpty() || groupsIntersect(userGroups, readWriteGroups);
    final boolean userIsReadOnlyUser = groupsIntersect(userGroups, readOnlyGroups) || (!globalReadOnlyGroups.isEmpty() && groupsIntersect(userGroups, globalReadOnlyGroups));
    final boolean userIsPartOfRequiredGroups = requiredGroups.isEmpty() || groupsIntersect(userGroups, requiredGroups);

    if (userIsAdmin) {
      return;  // Admins Rule Everything Around Me
    }

    checkForbidden(userIsPartOfRequiredGroups, "%s must be a member of one or more required groups: %s", user.getId(), JavaUtils.COMMA_JOINER.join(requiredGroups));

    if (scope == SingularityAuthorizationScope.READ) {
      checkForbidden(userIsReadOnlyUser || userIsReadWriteUser || userIsJITA, "%s must be a member of one or more groups to %s %s: %s", user.getId(), scope.name(), requestId, JavaUtils.COMMA_JOINER.join(Iterables.concat(readOnlyGroups, readWriteGroups, jitaGroups)));
    } else if (scope == SingularityAuthorizationScope.WRITE) {
      checkForbidden(userIsReadWriteUser || userIsJITA, "%s must be a member of one or more groups to %s %s: %s", user.getId(), scope.name(), requestId, JavaUtils.COMMA_JOINER.join(Iterables.concat(readWriteGroups, jitaGroups)));
    } else if (scope == SingularityAuthorizationScope.ADMIN) {
      checkForbidden(userIsAdmin, "%s must be a member of one or more groups to %s %s: %s", user.getId(), scope.name(), requestId, JavaUtils.COMMA_JOINER.join(adminGroups));
    }
  }

  public void checkForAuthorizedChanges(SingularityRequest request, SingularityRequest oldRequest, SingularityUser user) {
    if (!authEnabled) {
      return;
    }

    checkForbidden(user.isAuthenticated(), "Not Authenticated!");

    if (!oldRequest.getReadWriteGroups().equals(request.getReadWriteGroups()) || !oldRequest.getGroup().equals(request.getGroup())) {
      // If group or readWriteGroups are changing, a user must be authorized for both the old and new request groups
      checkForAuthorization(oldRequest, user, SingularityAuthorizationScope.WRITE);
      checkForAuthorization(request, user, SingularityAuthorizationScope.WRITE);
    }
  }

  public <T> Iterable<T> filterByAuthorizedRequests(final SingularityUser user, List<T> objects, final Function<T, String> requestIdFunction, final SingularityAuthorizationScope scope) {
    if (hasAdminAuthorization(user)) {
      return objects;
    }

    final Set<String> requestIds = copyOf(Iterables.transform(objects, new Function<T, String>() {
      @Override
      public String apply(@Nonnull T input) {
        return requestIdFunction.apply(input);
      }
    }));

    final Map<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(requestManager.getRequests(requestIds), new Function<SingularityRequestWithState, String>() {
      @Override
      public String apply(@Nonnull SingularityRequestWithState input) {
        return input.getRequest().getId();
      }
    });

    return Iterables.filter(objects, new Predicate<T>() {
      @Override
      public boolean apply(@Nonnull T input) {
        final String requestId = requestIdFunction.apply(input);
        return requestMap.containsKey(requestId) && isAuthorizedForRequest(requestMap.get(requestId).getRequest(), user, scope);
      }
    });
  }

  public Iterable<String> filterAuthorizedRequestIds(final SingularityUser user, List<String> requestIds, final SingularityAuthorizationScope scope, boolean useWebCache) {
    if (hasAdminAuthorization(user)) {
      return requestIds;
    }

    final Map<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(requestManager.getRequests(requestIds, useWebCache), new Function<SingularityRequestWithState, String>() {
      @Override
      public String apply(@Nonnull SingularityRequestWithState input) {
        return input.getRequest().getId();
      }
    });

    return Iterables.filter(requestIds, new Predicate<String>() {
      @Override
      public boolean apply(@Nonnull String input) {
        return requestMap.containsKey(input) && isAuthorizedForRequest(requestMap.get(input).getRequest(), user, scope);
      }
    });
  }
}
