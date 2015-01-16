package com.hubspot.singularity.resources;

import static com.hubspot.singularity.WebExceptions.checkNotFound;
import static com.hubspot.singularity.WebExceptions.conflict;
import static com.hubspot.singularity.WebExceptions.notFound;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.data.AbstractMachineManager;
import com.hubspot.singularity.data.AbstractMachineManager.DecomissionResult;

public abstract class AbstractMachineResource<T extends SingularityMachineAbstraction> {

  private final AbstractMachineManager<T> manager;

  public AbstractMachineResource(AbstractMachineManager<T> manager) {
    this.manager = manager;
  }

  public void removeDead(String objectId) {
    checkNotFound(manager.removeDead(objectId) == SingularityDeleteResult.DELETED, "Couldn't find dead %s with id %s", getObjectTypeString(), objectId);
  }

  public void removeDecomissioning(String objectId) {
    checkNotFound(manager.removeDecomissioning(objectId) == SingularityDeleteResult.DELETED, "Couldn't find decomissioning %s with id %s", getObjectTypeString(), objectId);
  }

  protected abstract String getObjectTypeString();

  public void decomission(String objectId, Optional<String> user) {
    DecomissionResult result = manager.decomission(objectId, user);

    if (result == DecomissionResult.FAILURE_NOT_FOUND || result == DecomissionResult.FAILURE_DEAD) {
      throw notFound("Couldn't find an active %s with id %s (result: %s)", getObjectTypeString(), objectId, result.name());
    } else if (result == DecomissionResult.FAILURE_ALREADY_DECOMISSIONING) {
      throw conflict("%s %s is already in decomissioning state", getObjectTypeString(), objectId);
    }
  }
}
