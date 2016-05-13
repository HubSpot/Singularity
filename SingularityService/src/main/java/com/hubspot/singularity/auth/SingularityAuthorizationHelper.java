package com.hubspot.singularity.auth;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkForbidden;
import static com.hubspot.singularity.WebExceptions.checkUnauthorized;

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
  private final boolean authEnabled;

  @Inject
  public SingularityAuthorizationHelper(RequestManager requestManager, SingularityConfiguration configuration) {
    this.requestManager = requestManager;
    this.adminGroups = copyOf(configuration.getAuthConfiguration().getAdminGroups());
    this.requiredGroups = copyOf(configuration.getAuthConfiguration().getRequiredGroups());
    this.jitaGroups = copyOf(configuration.getAuthConfiguration().getJitaGroups());
    this.defaultReadOnlyGroups = copyOf(configuration.getAuthConfiguration().getDefaultReadOnlyGroups());
    this.authEnabled = configuration.getAuthConfiguration().isEnabled();
  }

  public static boolean groupsIntersect(Set<String> a, Set<String> b) {
    return !Sets.intersection(a, b).isEmpty();
  }

  public boolean hasAdminAuthorization(Optional<SingularityUser> user) {
    // disabled auth == no rules!
    if (!authEnabled) {
      return true;
    }

    // not authenticated, or no groups, or no admin groups == can't possibly be admin
    if (!user.isPresent() || user.get().getGroups().isEmpty() || adminGroups.isEmpty()) {
      return false;
    }

    return groupsIntersect(user.get().getGroups(), adminGroups);
  }

  public void checkAdminAuthorization(Optional<SingularityUser> user) {
    if (authEnabled) {
      checkUnauthorized(user.isPresent(), "Please log in to perform this action.");
      if (!adminGroups.isEmpty()) {
        checkForbidden(groupsIntersect(user.get().getGroups(), adminGroups), "%s must be part of one or more admin groups: %s", user.get().getId(), JavaUtils.COMMA_JOINER.join(adminGroups));
      }
    }
  }

  public void checkForAuthorizationByTaskId(String taskId, Optional<SingularityUser> user, SingularityAuthorizationScope scope) {
    if (authEnabled) {
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

  public void checkForAuthorizationByRequestId(String requestId, Optional<SingularityUser> user, SingularityAuthorizationScope scope) {
    if (authEnabled) {
      final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(requestId);

      if (maybeRequest.isPresent()) {
        checkForAuthorization(maybeRequest.get().getRequest(), user, scope);
      }
    }
  }

  public boolean isAuthorizedForRequest(SingularityRequest request, Optional<SingularityUser> user, SingularityAuthorizationScope scope) {
    if (!authEnabled) {
      return true;  // no auth == no rules!
    }

    if (!user.isPresent()) {
      return false;
    }

    final Set<String> userGroups = user.get().getGroups();

    final boolean userIsAdmin = adminGroups.isEmpty() ? false : groupsIntersect(userGroups, adminGroups);
    final boolean userIsJITA = jitaGroups.isEmpty() ? false : groupsIntersect(userGroups, jitaGroups);
    final boolean userIsRequestOwner = request.getGroup().isPresent() ? userGroups.contains(request.getGroup().get()) : true;
    final boolean userIsReadOnlyUser = groupsIntersect(userGroups, request.getReadOnlyGroups().or(defaultReadOnlyGroups));
    final boolean userIsPartOfRequiredGroups = requiredGroups.isEmpty() ? true : groupsIntersect(userGroups, requiredGroups);

    if (userIsAdmin) {
      return true;  // Admins Rule Everything Around Me
    } else if (scope == SingularityAuthorizationScope.READ) {
      return (userIsReadOnlyUser || userIsRequestOwner || userIsJITA) && userIsPartOfRequiredGroups;
    } else if (scope == SingularityAuthorizationScope.WRITE) {
      return (userIsRequestOwner || userIsJITA) && userIsPartOfRequiredGroups;
    } else {
      return false;
    }
  }

  public void checkForAuthorization(SingularityRequest request, Optional<SingularityUser> user, SingularityAuthorizationScope scope) {
    if (!authEnabled) {
      return;
    }

    checkUnauthorized(user.isPresent(), "user must be present");

    final Set<String> userGroups = user.get().getGroups();
    final Set<String> readOnlyGroups = request.getReadOnlyGroups().or(defaultReadOnlyGroups);

    final boolean userIsAdmin = adminGroups.isEmpty() ? false : groupsIntersect(userGroups, adminGroups);
    final boolean userIsJITA = jitaGroups.isEmpty() ? false : groupsIntersect(userGroups, jitaGroups);
    final boolean userIsRequestOwner = request.getGroup().isPresent() ? userGroups.contains(request.getGroup().get()) : true;
    final boolean userIsReadOnlyUser = groupsIntersect(userGroups, readOnlyGroups);
    final boolean userIsPartOfRequiredGroups = requiredGroups.isEmpty() ? true : groupsIntersect(userGroups, requiredGroups);

    if (userIsAdmin) {
      return;  // Admins Rule Everything Around Me
    }

    checkForbidden(userIsPartOfRequiredGroups, "%s must be a member of one or more required groups: %s", user.get().getId(), JavaUtils.COMMA_JOINER.join(requiredGroups));

    if (scope == SingularityAuthorizationScope.READ) {
      checkForbidden(userIsReadOnlyUser || userIsRequestOwner || userIsJITA, "%s must be a member of one or more groups to %s %s: %s", user.get().getId(), scope.name(), request.getId(), JavaUtils.COMMA_JOINER.join(Iterables.concat(readOnlyGroups, request.getGroup().asSet(), jitaGroups)));
    } else if (scope == SingularityAuthorizationScope.WRITE) {
      checkForbidden(userIsRequestOwner || userIsJITA, "%s must be a member of one or more groups to %s %s: %s", user.get().getId(), scope.name(), request.getId(), JavaUtils.COMMA_JOINER.join(Iterables.concat(request.getGroup().asSet(), jitaGroups)));
    } else if (scope == SingularityAuthorizationScope.ADMIN) {
      checkForbidden(userIsAdmin, "%s must be a member of one or more groups to %s %s: %s", user.get().getId(), scope.name(), request.getId(), JavaUtils.COMMA_JOINER.join(adminGroups));
    }
  }

  public <T> Iterable<T> filterByAuthorizedRequests(final Optional<SingularityUser> user, List<T> objects, final Function<T, String> requestIdFunction, final SingularityAuthorizationScope scope) {
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

  public Iterable<String> filterAuthorizedRequestIds(final Optional<SingularityUser> user, List<String> requestIds, final SingularityAuthorizationScope scope) {
    if (hasAdminAuthorization(user)) {
      return requestIds;
    }

    final Map<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(requestManager.getRequests(requestIds), new Function<SingularityRequestWithState, String>() {
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
