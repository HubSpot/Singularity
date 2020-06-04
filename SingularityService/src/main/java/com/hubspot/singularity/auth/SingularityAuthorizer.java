package com.hubspot.singularity.auth;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.hubspot.singularity.WebExceptions.badRequest;
import static com.hubspot.singularity.WebExceptions.checkForbidden;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.RequestManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class SingularityAuthorizer {
  protected final RequestManager requestManager;
  protected final boolean authEnabled;

  public SingularityAuthorizer(RequestManager requestManager, boolean authEnabled) {
    this.requestManager = requestManager;
    this.authEnabled = authEnabled;
  }

  static boolean groupsIntersect(Set<String> a, Set<String> b) {
    return !Sets.intersection(a, b).isEmpty();
  }

  public abstract boolean hasAdminAuthorization(SingularityUser user);

  public abstract void checkAdminAuthorization(SingularityUser user);

  public abstract void checkReadAuthorization(SingularityUser user);

  public abstract void checkForAuthorization(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  );

  public abstract boolean isAuthorizedForRequest(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  );

  public abstract void checkForAuthorizedChanges(
    SingularityRequest request,
    SingularityRequest oldRequest,
    SingularityUser user
  );

  public void checkForAuthorizationByTaskId(
    String taskId,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    if (authEnabled) {
      checkForbidden(user.isAuthenticated(), "Not Authenticated!");
      try {
        final SingularityTaskId taskIdObj = SingularityTaskId.valueOf(taskId);

        final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(
          taskIdObj.getRequestId()
        );

        maybeRequest.ifPresent(
          singularityRequestWithState ->
            checkForAuthorization(singularityRequestWithState.getRequest(), user, scope)
        );
      } catch (InvalidSingularityTaskIdException e) {
        badRequest(e.getMessage());
      }
    }
  }

  public void checkForAuthorizationByRequestId(
    String requestId,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    if (authEnabled) {
      final Optional<SingularityRequestWithState> maybeRequest = requestManager.getRequest(
        requestId
      );

      maybeRequest.ifPresent(
        singularityRequestWithState ->
          checkForAuthorization(singularityRequestWithState.getRequest(), user, scope)
      );
    }
  }

  public <T> List<T> filterByAuthorizedRequests(
    final SingularityUser user,
    List<T> objects,
    final Function<T, String> requestIdFunction,
    final SingularityAuthorizationScope scope
  ) {
    if (hasAdminAuthorization(user)) {
      return objects;
    }

    final Set<String> requestIds = copyOf(
      objects.stream().map(requestIdFunction).collect(Collectors.toList())
    );

    final ImmutableMap<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(
      requestManager.getRequests(requestIds),
      input -> input.getRequest().getId()
    );

    return objects
      .stream()
      .filter(
        input -> {
          final String requestId = requestIdFunction.apply(input);
          return (
            requestMap.containsKey(requestId) &&
            isAuthorizedForRequest(requestMap.get(requestId).getRequest(), user, scope)
          );
        }
      )
      .collect(Collectors.toList());
  }

  public List<String> filterAuthorizedRequestIds(
    final SingularityUser user,
    List<String> requestIds,
    final SingularityAuthorizationScope scope,
    boolean useWebCache
  ) {
    if (hasAdminAuthorization(user)) {
      return requestIds;
    }

    final Map<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(
      requestManager.getRequests(requestIds, useWebCache),
      input -> input.getRequest().getId()
    );

    return requestIds
      .stream()
      .filter(
        input ->
          requestMap.containsKey(input) &&
          isAuthorizedForRequest(requestMap.get(input).getRequest(), user, scope)
      )
      .collect(Collectors.toList());
  }
}
