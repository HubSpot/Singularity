package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.hubspot.singularity.data.SingularityValidator;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

public abstract class AbstractMachineResource<T extends SingularityMachineAbstraction<T>> {

  protected final AbstractMachineManager<T> manager;
  protected final Optional<SingularityUser> user;
  protected final SingularityValidator validator;

  public AbstractMachineResource(AbstractMachineManager<T> manager, SingularityValidator validator, Optional<SingularityUser> user) {
    this.manager = manager;
    this.validator = validator;
    this.user = user;
  }

  protected void remove(String objectId) {
    checkNotFound(manager.deleteObject(objectId) == SingularityDeleteResult.DELETED, "Couldn't find dead %s with id %s", getObjectTypeString(), objectId);
  }

  protected abstract String getObjectTypeString();

  private void changeState(String objectId, MachineState newState, Optional<String> user) {
    StateChangeResult result = manager.changeState(objectId, newState, user);

    switch (result) {
      case FAILURE_NOT_FOUND:
        throw new NotFoundException(String.format("Couldn't find an active %s with id %s (result: %s)", getObjectTypeString(), objectId, result.name()));
      case FAILURE_ALREADY_AT_STATE:
      case FAILURE_ILLEGAL_TRANSITION:
        throw new ConflictException(String.format("%s - %s %s is in %s state", result.name(), getObjectTypeString(), objectId, newState));
      default:
        break;
    }

  }

  protected void decommission(String objectId, Optional<String> user) {
    changeState(objectId, MachineState.STARTING_DECOMMISSION, user);
  }

  protected void activate(String objectId, Optional<String> user) {
    changeState(objectId, MachineState.ACTIVE, user);
  }

}
