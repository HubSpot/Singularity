package com.hubspot.singularity.auth;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.SingularityValidator;

@Singleton
public class SingularityAuthorizationHelper {
  private final SingularityValidator validator;
  private final RequestManager requestManager;

  @Inject
  public SingularityAuthorizationHelper(SingularityValidator validator, RequestManager requestManager) {
    this.validator = validator;
    this.requestManager = requestManager;
  }

  public <T> List<T> filterByAuthorizedRequests(final Optional<SingularityUser> user, List<T> objects, final Function<T, String> requestIdFunction) {
    if (validator.hasAdminAuthorization(user)) {
      return objects;
    }

    final Set<String> requestIds = ImmutableSet.copyOf(Iterables.transform(objects, new Function<T, String>() {
      @Nullable
      @Override
      public String apply(@Nullable T input) {
        return requestIdFunction.apply(input);
      }
    }));

    final Map<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(requestManager.getRequests(requestIds), new Function<SingularityRequestWithState, String>() {
      @Nullable
      @Override
      public String apply(@Nullable SingularityRequestWithState input) {
        return input.getRequest().getId();
      }
    });

    return ImmutableList.copyOf(Iterables.filter(objects, new Predicate<T>() {
      @Override
      public boolean apply(@Nullable T input) {
        final String requestId = requestIdFunction.apply(input);
        return requestMap.containsKey(requestId) && validator.isAuthorizedForRequest(requestMap.get(requestId).getRequest(), user);
      }
    }));
  }

  public List<String> filterAuthorizedRequestIds(final Optional<SingularityUser> user, List<String> requestIds) {
    if (validator.hasAdminAuthorization(user)) {
      return requestIds;
    }

    final Map<String, SingularityRequestWithState> requestMap = Maps.uniqueIndex(requestManager.getRequests(requestIds), new Function<SingularityRequestWithState, String>() {
      @Nullable
      @Override
      public String apply(@Nullable SingularityRequestWithState input) {
        return input.getRequest().getId();
      }
    });

    return ImmutableList.copyOf(Iterables.filter(requestIds, new Predicate<String>() {
      @Override
      public boolean apply(@Nullable String input) {
        return requestMap.containsKey(input) && validator.isAuthorizedForRequest(requestMap.get(input).getRequest(), user);
      }
    }));
  }
}
