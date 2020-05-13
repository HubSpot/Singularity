package com.hubspot.singularity.auth;

import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.RequestManager;

@Singleton
public class SingularityGroupsScopesAuthorizer extends SingularityAuthorizer {

  public SingularityGroupsScopesAuthorizer(
    RequestManager requestManager,
    boolean authEnabled
  ) {
    super(requestManager, authEnabled);
  }

  @Override
  public boolean hasAdminAuthorization(SingularityUser user) {
    return false;
  }

  @Override
  public void checkAdminAuthorization(SingularityUser user) {}

  @Override
  public void checkReadAuthorization(SingularityUser user) {}

  @Override
  public void checkUserInRequiredGroups(SingularityUser user) {}

  @Override
  public void checkForAuthorization(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {}

  @Override
  public boolean isAuthorizedForRequest(
    SingularityRequest request,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {
    return false;
  }

  @Override
  public void checkForAuthorizationByTaskId(
    String taskId,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {}

  @Override
  public void checkForAuthorizationByRequestId(
    String requestId,
    SingularityUser user,
    SingularityAuthorizationScope scope
  ) {}

  @Override
  public void checkForAuthorizedChanges(
    SingularityRequest request,
    SingularityRequest oldRequest,
    SingularityUser user
  ) {}
}
