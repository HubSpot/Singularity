package com.hubspot.singularity.resources;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AbstractMachineManager.DecomissionResult;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;

public abstract class AbstractMachineResource<T extends SingularityMachineAbstraction> {
  
  private final AbstractMachineManager<T> manager;
  
  public AbstractMachineResource(AbstractMachineManager<T> manager) {
    this.manager = manager;
  }
  
  public void removeDead(String objectId) {
    if (manager.removeDead(objectId) ==  SingularityDeleteResult.DIDNT_EXIST) {
      throw new NotFoundException(String.format("Couldn't find dead %s with id %s", getObjectTypeString(), objectId));
    }
  }
  
  public void removeDecomissioning(String objectId) {
    if (manager.removeDecomissioning(objectId) ==  SingularityDeleteResult.DIDNT_EXIST) {
      throw new NotFoundException(String.format("Couldn't find decomissioning %s with id %s", getObjectTypeString(), objectId));
    }
  }
  
  protected abstract String getObjectTypeString();
  
  public void decomission(String objectId, Optional<String> user) {
    DecomissionResult result = manager.decomission(objectId, user);
  
    if (result == DecomissionResult.FAILURE_NOT_FOUND || result == DecomissionResult.FAILURE_DEAD) {
      throw new NotFoundException(String.format("Couldn't find an active %s with id %s (result: %s)", getObjectTypeString(), objectId, result.name()));
    } else if (result == DecomissionResult.FAILURE_ALREADY_DECOMISSIONING) {
      throw new ConflictException(String.format("%s %s is already in decomissioning state", getObjectTypeString(), objectId));
    }
  }
  
}
