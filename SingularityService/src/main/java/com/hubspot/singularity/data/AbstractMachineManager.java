package com.hubspot.singularity.data;

import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityMachineAbstraction;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;

public abstract class AbstractMachineManager<T extends SingularityMachineAbstraction<T>> extends CuratorAsyncManager {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractMachineManager.class);

  private static final String HISTORY_PATH = "history";
  private static final String EXPIRING_PATH = "expiring";

  private final Transcoder<T> transcoder;
  private final Transcoder<SingularityMachineStateHistoryUpdate> historyTranscoder;
  private final Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder;

  public AbstractMachineManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, Transcoder<T> transcoder,
      Transcoder<SingularityMachineStateHistoryUpdate> historyTranscoder, Transcoder<SingularityExpiringMachineState> expiringMachineStateTranscoder) {
    super(curator, configuration, metricRegistry);

    this.transcoder = transcoder;
    this.historyTranscoder = historyTranscoder;
    this.expiringMachineStateTranscoder = expiringMachineStateTranscoder;
  }

  protected abstract String getRoot();

  private String getHistoryPath(String objectId) {
    return ZKPaths.makePath(getObjectPath(objectId), HISTORY_PATH);
  }

  public List<SingularityMachineStateHistoryUpdate> getHistory(String objectId) {
    return getAsyncChildren(getHistoryPath(objectId), historyTranscoder);
  }

  public List<T> getObjects() {
    return getObjects(getRoot());
  }

  public List<String> getObjectIds() {
    return getChildren(getRoot());
  }

  public int getNumObjectsAtState(MachineState state) {
    return getObjectsFiltered(state).size();
  }

  public int getNumActive() {
    return getNumObjectsAtState(MachineState.ACTIVE);
  }

  public Map<String, T> getObjectsByIdForState(MachineState state) {
    List<T> filteredObjects = getObjectsFiltered(state);

    Map<String, T> filteredObjectIds = Maps.newHashMapWithExpectedSize(filteredObjects.size());

    for (T filteredObject : filteredObjects) {
      filteredObjectIds.put(filteredObject.getId(), filteredObject);
    }

    return filteredObjectIds;
  }

  public List<T> getObjectsFiltered(MachineState state) {
    return getObjectsFiltered(Optional.of(state));
  }

  public List<T> getObjectsFiltered(Optional<MachineState> state) {
    List<T> objects = getObjects();

    if (!state.isPresent()) {
      return objects;
    }

    return getObjectsFiltered(objects, state.get());
  }

  private List<T> getObjectsFiltered(List<T> objects, MachineState state) {
    List<T> filtered = Lists.newArrayListWithCapacity(objects.size());

    for (T object : objects) {
      if (object.getCurrentState().getState() == state) {
        filtered.add(object);
      }
    }

    return filtered;
  }

  private String getObjectPath(String objectId) {
    return ZKPaths.makePath(getRoot(), objectId);
  }

  public Optional<T> getObject(String objectId) {
    return getData(getObjectPath(objectId), transcoder);
  }

  protected List<T> getObjects(String root) {
    return getAsyncChildren(root, transcoder);
  }

  public SingularityDeleteResult removed(String objectId) {
    return delete(getObjectPath(objectId));
  }

  public enum StateChangeResult {
    FAILURE_NOT_FOUND, FAILURE_ALREADY_AT_STATE, FAILURE_ILLEGAL_TRANSITION, SUCCESS;
  }

  public StateChangeResult changeState(String objectId, MachineState newState, Optional<String> message, Optional<String> user) {
    Optional<T> maybeObject = getObject(objectId);

    if (!maybeObject.isPresent()) {
      return StateChangeResult.FAILURE_NOT_FOUND;
    }

    final T object = maybeObject.get();

    return changeState(object, newState, message, user);
  }

  public StateChangeResult changeState(T object, MachineState newState, Optional<String> message, Optional<String> user) {
    Optional<StateChangeResult> maybeInvalidStateChange = getInvalidStateChangeResult(object.getCurrentState().getState(), newState, false);

    if (maybeInvalidStateChange.isPresent()) {
      return maybeInvalidStateChange.get();
    }

    clearExpiringStateChangeIfInvalid(object, newState);

    SingularityMachineStateHistoryUpdate newStateUpdate = new SingularityMachineStateHistoryUpdate(object.getId(), newState, System.currentTimeMillis(), user, message);

    LOG.debug("{} changing state from {} to {} by {}", object.getId(), object.getCurrentState().getState(), newState, user);

    saveObject(object.changeState(newStateUpdate));

    return StateChangeResult.SUCCESS;
  }

  private void clearExpiringStateChangeIfInvalid(T object, MachineState newState) {
    Optional<SingularityExpiringMachineState> maybeExpiring = getExpiringObject(object.getId());
    if (!maybeExpiring.isPresent()) {
      return;
    }
    MachineState targetExpiringState = maybeExpiring.get().getRevertToState();

    Optional<StateChangeResult> maybeInvalidStateChange = getInvalidStateChangeResult(newState, targetExpiringState, true);

    if (maybeInvalidStateChange.isPresent()) {
      LOG.info("Cannot complete expiring state transition from {} to {}, removing expiring action for {}", newState, targetExpiringState, object.getId());
      deleteExpiringObject(object.getId());
    }
  }

  private Optional<StateChangeResult> getInvalidStateChangeResult(MachineState currentState, MachineState newState, boolean expiringAction) {
    if (currentState == newState) {
      return Optional.of(StateChangeResult.FAILURE_ALREADY_AT_STATE);
    }

    if (newState == MachineState.STARTING_DECOMMISSION && currentState.isDecommissioning()) {
      return Optional.of(StateChangeResult.FAILURE_ILLEGAL_TRANSITION);
    }

    // can't jump from FROZEN or ACTIVE to DECOMMISSIONING or DECOMMISSIONED
    if (((newState == MachineState.DECOMMISSIONING) || (newState == MachineState.DECOMMISSIONED)) && (currentState == MachineState.FROZEN || currentState == MachineState.ACTIVE)) {
      return Optional.of(StateChangeResult.FAILURE_ILLEGAL_TRANSITION);
    }

    // can't jump from a decommissioning state to FROZEN
    if ((newState == MachineState.FROZEN) && currentState.isDecommissioning()) {
      return Optional.of(StateChangeResult.FAILURE_ILLEGAL_TRANSITION);
    }

    // User/Expiring can't jump from inactive to active state
    if (currentState.isInactive() && expiringAction) {
      return Optional.of(StateChangeResult.FAILURE_ILLEGAL_TRANSITION);
    }

    return Optional.absent();
  }

  private String getHistoryUpdatePath(SingularityMachineStateHistoryUpdate historyUpdate) {
    final String historyChildPath = String.format("%s-%s", historyUpdate.getState().name(), historyUpdate.getTimestamp());

    return ZKPaths.makePath(getHistoryPath(historyUpdate.getObjectId()), historyChildPath);
  }

  private SingularityCreateResult saveHistoryUpdate(SingularityMachineStateHistoryUpdate historyUpdate) {
    return create(getHistoryUpdatePath(historyUpdate), historyUpdate, historyTranscoder);
  }

  public SingularityDeleteResult deleteObject(String objectId) {
    return delete(getObjectPath(objectId));
  }

  public void saveObject(T object) {
    saveHistoryUpdate(object.getCurrentState());

    save(getObjectPath(object.getId()), object, transcoder);
  }

  private String getExpiringPath(String machineId) {
    return ZKPaths.makePath(getRoot(), EXPIRING_PATH, machineId);
  }

  public List<SingularityExpiringMachineState> getExpiringObjects() {
    return getAsyncChildren(ZKPaths.makePath(getRoot(), EXPIRING_PATH), expiringMachineStateTranscoder);
  }

  public Optional<SingularityExpiringMachineState> getExpiringObject(String machineId) {
    return getData(getExpiringPath(machineId), expiringMachineStateTranscoder);
  }

  public SingularityCreateResult saveExpiringObject(SingularityExpiringMachineState expiringMachineState, String machineId) {
    return save(getExpiringPath(machineId), expiringMachineState, expiringMachineStateTranscoder);
  }

  public SingularityDeleteResult deleteExpiringObject(String machineId) {
    return delete(getExpiringPath(machineId));
  }


}
