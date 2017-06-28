package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;

import java.util.List;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizationHelper;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

public abstract class AbstractMachineResource<T extends SingularityMachineAbstraction<T>> {

  protected final AbstractMachineManager<T> manager;
  protected final Optional<SingularityUser> user;

  protected final SingularityAuthorizationHelper authorizationHelper;
  private final SingularityValidator validator;

  public AbstractMachineResource(AbstractMachineManager<T> manager, SingularityAuthorizationHelper authorizationHelper, Optional<SingularityUser> user, SingularityValidator validator) {
    this.manager = manager;
    this.authorizationHelper = authorizationHelper;
    this.user = user;
    this.validator = validator;
  }

  protected void remove(String objectId) {
    authorizationHelper.checkAdminAuthorization(user);
    checkNotFound(manager.deleteObject(objectId) == SingularityDeleteResult.DELETED, "Couldn't find dead %s with id %s", getObjectTypeString(), objectId);
  }

  protected void cancelExpiring(String objectId) {
    authorizationHelper.checkAdminAuthorization(user);
    manager.deleteExpiringObject(objectId);
  }

  protected List<SingularityExpiringMachineState> getExpiringStateChanges() {
    authorizationHelper.checkAdminAuthorization(user);
    return manager.getExpiringObjects();
  }

  protected abstract String getObjectTypeString();

  private void changeState(String objectId, MachineState newState, Optional<SingularityMachineChangeRequest> changeRequest, Optional<String> user) {
    Optional<String> message = Optional.absent();

    if (changeRequest.isPresent()) {
      message = changeRequest.get().getMessage();
    }

    StateChangeResult result = manager.changeState(objectId, newState, message, user);

    switch (result) {
      case FAILURE_NOT_FOUND:
        throw new WebApplicationException(String.format("Couldn't find an active %s with id %s (result: %s)", getObjectTypeString(), objectId, result.name()), Status.NOT_FOUND);
      case FAILURE_ALREADY_AT_STATE:
      case FAILURE_ILLEGAL_TRANSITION:
        throw new WebApplicationException(String.format("%s - %s %s is in %s state", result.name(), getObjectTypeString(), objectId, newState), Status.CONFLICT);
      default:
        break;
    }
  }

  protected void decommission(String objectId, Optional<SingularityMachineChangeRequest> decommissionRequest, Optional<String> queryUser, SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(action);
    validator.validateExpiringMachineStateChange(decommissionRequest, MachineState.STARTING_DECOMMISSION, manager.getExpiringObject(objectId));
    validator.validateDecommissioningCount();
    changeState(objectId, MachineState.STARTING_DECOMMISSION, decommissionRequest, queryUser);
    saveExpiring(decommissionRequest, queryUser, objectId);
  }

  protected void freeze(String objectId, Optional<SingularityMachineChangeRequest> freezeRequest, Optional<String> queryUser, SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(action);
    validator.validateExpiringMachineStateChange(freezeRequest, MachineState.FROZEN, manager.getExpiringObject(objectId));
    changeState(objectId, MachineState.FROZEN, freezeRequest, queryUser);
    saveExpiring(freezeRequest, queryUser, objectId);
  }

  protected void activate(String objectId, Optional<SingularityMachineChangeRequest> activateRequest, Optional<String> queryUser, SingularityAction action) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(action);
    validator.validateExpiringMachineStateChange(activateRequest, MachineState.ACTIVE, manager.getExpiringObject(objectId));
    changeState(objectId, MachineState.ACTIVE, activateRequest, queryUser);
    saveExpiring(activateRequest, queryUser, objectId);
  }

  private void saveExpiring(Optional<SingularityMachineChangeRequest> changeRequest, Optional<String> queryUser, String objectId) {
    if (changeRequest.isPresent() && changeRequest.get().getDurationMillis().isPresent()) {
      manager.saveExpiringObject(
        new SingularityExpiringMachineState(
          queryUser,
          System.currentTimeMillis(),
          changeRequest.get().getActionId().or(UUID.randomUUID().toString()),
          changeRequest.get(),
          objectId,
          changeRequest.get().getRevertToState().get(),
          Optional.of(changeRequest.get().isKillTasksOnDecommissionTimeout())
          ),
        objectId);
    }
  }
}
