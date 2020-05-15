package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.AuthConfiguration;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.ning.http.client.AsyncHttpClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

public abstract class AbstractMachineResource<T extends SingularityMachineAbstraction<T>>
  extends AbstractLeaderAwareResource {
  protected final AbstractMachineManager<T> manager;

  protected final SingularityAuthorizer authorizationHelper;
  private final SingularityValidator validator;
  private final AuthConfiguration authConfiguration;

  public AbstractMachineResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    ObjectMapper objectMapper,
    AbstractMachineManager<T> manager,
    SingularityAuthorizer authorizationHelper,
    SingularityValidator validator,
    AuthConfiguration authConfiguration
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.manager = manager;
    this.authorizationHelper = authorizationHelper;
    this.validator = validator;
    this.authConfiguration = authConfiguration;
  }

  protected void remove(String objectId, SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    checkNotFound(
      manager.deleteObject(objectId) == SingularityDeleteResult.DELETED,
      "Couldn't find dead %s with id %s",
      getObjectTypeString(),
      objectId
    );
  }

  protected void cancelExpiring(String objectId, SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    manager.deleteExpiringObject(objectId);
  }

  protected List<SingularityExpiringMachineState> getExpiringStateChanges(
    SingularityUser user
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getExpiringObjects();
  }

  protected abstract String getObjectTypeString();

  private void changeState(
    String objectId,
    MachineState newState,
    Optional<SingularityMachineChangeRequest> changeRequest,
    Optional<String> user
  ) {
    Optional<String> message = Optional.empty();

    if (changeRequest.isPresent()) {
      message = changeRequest.get().getMessage();
    }

    StateChangeResult result = manager.changeState(objectId, newState, message, user);

    switch (result) {
      case FAILURE_NOT_FOUND:
        throw new WebApplicationException(
          String.format(
            "Couldn't find an active %s with id %s (result: %s)",
            getObjectTypeString(),
            objectId,
            result.name()
          ),
          Status.NOT_FOUND
        );
      case FAILURE_ALREADY_AT_STATE:
      case FAILURE_ILLEGAL_TRANSITION:
        throw new WebApplicationException(
          String.format(
            "%s - %s %s is in %s state",
            result.name(),
            getObjectTypeString(),
            objectId,
            newState
          ),
          Status.CONFLICT
        );
      default:
        break;
    }
  }

  protected void decommission(
    String objectId,
    Optional<SingularityMachineChangeRequest> decommissionRequest,
    SingularityUser user,
    SingularityAction action
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(action);
    validator.validateExpiringMachineStateChange(
      decommissionRequest,
      MachineState.STARTING_DECOMMISSION,
      manager.getExpiringObject(objectId)
    );
    validator.validateDecommissioningCount();
    changeState(
      objectId,
      MachineState.STARTING_DECOMMISSION,
      decommissionRequest,
      user.getEmailOrDefault(authConfiguration.getDefaultEmailDomain())
    );
    saveExpiring(decommissionRequest, user, objectId);
  }

  protected void freeze(
    String objectId,
    Optional<SingularityMachineChangeRequest> freezeRequest,
    SingularityUser user,
    SingularityAction action
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(action);
    validator.validateExpiringMachineStateChange(
      freezeRequest,
      MachineState.FROZEN,
      manager.getExpiringObject(objectId)
    );
    changeState(
      objectId,
      MachineState.FROZEN,
      freezeRequest,
      user.getEmailOrDefault(authConfiguration.getDefaultEmailDomain())
    );
    saveExpiring(freezeRequest, user, objectId);
  }

  protected void activate(
    String objectId,
    Optional<SingularityMachineChangeRequest> activateRequest,
    SingularityUser user,
    SingularityAction action
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(action);
    validator.validateExpiringMachineStateChange(
      activateRequest,
      MachineState.ACTIVE,
      manager.getExpiringObject(objectId)
    );
    changeState(
      objectId,
      MachineState.ACTIVE,
      activateRequest,
      user.getEmailOrDefault(authConfiguration.getDefaultEmailDomain())
    );
    saveExpiring(activateRequest, user, objectId);
  }

  private void saveExpiring(
    Optional<SingularityMachineChangeRequest> changeRequest,
    SingularityUser user,
    String objectId
  ) {
    if (
      changeRequest.isPresent() && changeRequest.get().getDurationMillis().isPresent()
    ) {
      manager.saveExpiringObject(
        new SingularityExpiringMachineState(
          user.getEmailOrDefault(authConfiguration.getDefaultEmailDomain()),
          System.currentTimeMillis(),
          changeRequest.get().getActionId().orElse(UUID.randomUUID().toString()),
          changeRequest.get(),
          objectId,
          changeRequest.get().getRevertToState().get(),
          Optional.of(changeRequest.get().isKillTasksOnDecommissionTimeout())
        ),
        objectId
      );
    }
  }
}
