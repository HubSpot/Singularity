package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AbstractMachineManager.StateChangeResult;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

public abstract class AbstractMachineResource<T extends SingularityMachineAbstraction<T>> {

  private final AbstractMachineManager<T> manager;

  public AbstractMachineResource(AbstractMachineManager<T> manager) {
    this.manager = manager;
  }

  public void remove(String objectId) {
    if (manager.deleteObject(objectId) ==  SingularityDeleteResult.DIDNT_EXIST) {
      throw new NotFoundException(String.format("Couldn't find dead %s with id %s", getObjectTypeString(), objectId));
    }
  }

  protected abstract String getObjectTypeString();

  public void decomission(String objectId, Optional<String> user) {
    StateChangeResult result = manager.changeState(objectId, MachineState.STARTING_DECOMISSION, user);

    switch (result) {
      case FAILURE_NOT_FOUND:
        throw new NotFoundException(String.format("Couldn't find an active %s with id %s (result: %s)", getObjectTypeString(), objectId, result.name()));
      case FAILURE_ALREADY_AT_STATE:
      case FAILURE_ILLEGAL_TRANSITION:
        throw new ConflictException(String.format("%s %s is already in decomissioning state", getObjectTypeString(), objectId));
      default:
        break;
    }
  }

}
