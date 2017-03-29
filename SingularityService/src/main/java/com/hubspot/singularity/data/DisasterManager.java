package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.SingularityDisaster;
import com.hubspot.singularity.SingularityDisasterDataPoints;
import com.hubspot.singularity.SingularityDisasterType;
import com.hubspot.singularity.SingularityDisastersData;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;

public class DisasterManager extends CuratorAsyncManager {
  private static final Logger LOG = LoggerFactory.getLogger(DisasterManager.class);

  private static final String DISASTERS_ROOT = "/disasters";
  private static final String DISABLED_ACTIONS_PATH = DISASTERS_ROOT + "/disabled-actions";
  private static final String ACTIVE_DISASTERS_PATH = DISASTERS_ROOT + "/active";
  private static final String DISASTER_STATS_PATH = DISASTERS_ROOT + "/statistics";
  private static final String DISABLE_AUTOMATED_PATH = DISASTERS_ROOT + "/disabled";
  private static final String TASK_CREDITS_PATH = DISASTERS_ROOT + "/task-credits";
  private static final String TASK_CREDITS_ENABLED_PATH = TASK_CREDITS_PATH + "/enabled";
  private static final String TASK_CREDITS_UPDATES_PATH = TASK_CREDITS_PATH + "/updates";
  private static final String TASK_CREDIT_COUNT_PATH = TASK_CREDITS_PATH + "/count";

  private static final String MESSAGE_FORMAT = "Cannot perform action %s: %s";
  private static final String DEFAULT_MESSAGE = "Action is currently disabled";

  private final Transcoder<SingularityDisabledAction> disabledActionTranscoder;
  private final Transcoder<SingularityDisasterDataPoints> disasterStatsTranscoder;

  @Inject
  public DisasterManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry,
                         Transcoder<SingularityDisabledAction> disabledActionTranscoder, Transcoder<SingularityDisasterDataPoints> disasterStatsTranscoder) {
    super(curator, configuration, metricRegistry);
    this.disabledActionTranscoder = disabledActionTranscoder;
    this.disasterStatsTranscoder = disasterStatsTranscoder;
  }

  private String getActionPath(SingularityAction action) {
    return ZKPaths.makePath(DISABLED_ACTIONS_PATH, action.name());
  }

  public boolean isDisabled(SingularityAction action) {
    return exists(getActionPath(action));
  }

  public SingularityDisabledAction getDisabledAction(SingularityAction action) {
    Optional<SingularityDisabledAction> maybeDisabledAction = getData(getActionPath(action), disabledActionTranscoder);
    return maybeDisabledAction.or(new SingularityDisabledAction(action, String.format(MESSAGE_FORMAT, action, DEFAULT_MESSAGE), Optional.<String>absent(), false, Optional.<Long>absent()));
  }

  public SingularityCreateResult disable(SingularityAction action, Optional<String> maybeMessage, Optional<SingularityUser> user, boolean systemGenerated, Optional<Long> expiresAt) {
    if (!action.isCanDisable()) {
      throw new IllegalArgumentException(String.format("Action %s cannot be disabled", action));
    }
    SingularityDisabledAction disabledAction = new SingularityDisabledAction(
      action,
      String.format(MESSAGE_FORMAT, action, maybeMessage.or(DEFAULT_MESSAGE)),
      user.isPresent() ? Optional.of(user.get().getId()) : Optional.<String>absent(),
      systemGenerated,
      expiresAt);

    return save(getActionPath(action), disabledAction, disabledActionTranscoder);
  }

  public SingularityDeleteResult enable(SingularityAction action) {
    return delete(getActionPath(action));
  }

  public List<SingularityDisabledAction> getDisabledActions() {
    List<String> paths = new ArrayList<>();
    for (String path : getChildren(DISABLED_ACTIONS_PATH)) {
      paths.add(ZKPaths.makePath(DISABLED_ACTIONS_PATH, path));
    }

    return getAsync("getDisabledActions", paths, disabledActionTranscoder);
  }

  public void addDisaster(SingularityDisasterType disaster) {
    create(ZKPaths.makePath(ACTIVE_DISASTERS_PATH, disaster.name()));
  }

  public void removeDisaster(SingularityDisasterType disaster) {
    delete(ZKPaths.makePath(ACTIVE_DISASTERS_PATH, disaster.name()));
    if (getActiveDisasters().isEmpty()) {
      clearSystemGeneratedDisabledActions();
    }
  }

  public boolean isDisasterActive(SingularityDisasterType disaster) {
    return exists(ZKPaths.makePath(ACTIVE_DISASTERS_PATH, disaster.name()));
  }

  public List<SingularityDisasterType> getActiveDisasters() {
    List<String> disasterNames = getChildren(ACTIVE_DISASTERS_PATH);
    List<SingularityDisasterType> disasters = new ArrayList<>();
    for (String name : disasterNames) {
      disasters.add(SingularityDisasterType.valueOf(name));
    }
    return disasters;
  }

  public List<SingularityDisaster> getAllDisasterStates() {
    return getAllDisasterStates(getActiveDisasters());
  }

  public List<SingularityDisaster> getAllDisasterStates(List<SingularityDisasterType> activeDisasters) {
    List<SingularityDisaster> disasters = new ArrayList<>();
    for (SingularityDisasterType type : SingularityDisasterType.values()) {
      disasters.add(new SingularityDisaster(type, activeDisasters.contains(type)));
    }
    return disasters;
  }

  public void saveDisasterStats(SingularityDisasterDataPoints stats) {
    save(DISASTER_STATS_PATH, stats, disasterStatsTranscoder);
  }

  public SingularityDisasterDataPoints getDisasterStats() {
    SingularityDisasterDataPoints stats = getData(DISASTER_STATS_PATH, disasterStatsTranscoder).or(SingularityDisasterDataPoints.empty());
    Collections.sort(stats.getDataPoints());
    return stats;
  }

  public SingularityDisastersData getDisastersData() {
    return new SingularityDisastersData(getDisasterStats().getDataPoints(), getAllDisasterStates(), isAutomatedDisabledActionsDisabled());
  }

  public void updateActiveDisasters(List<SingularityDisasterType> previouslyActiveDisasters, List<SingularityDisasterType> newActiveDisasters) {
    for (SingularityDisasterType disaster : previouslyActiveDisasters) {
      if (!newActiveDisasters.contains(disaster)) {
        removeDisaster(disaster);
      }
    }

    for (SingularityDisasterType disaster : newActiveDisasters) {
      if (!isDisasterActive(disaster)) {
        addDisaster(disaster);
      }
    }


  }

  public void addDisabledActionsForDisasters(List<SingularityDisasterType> newActiveDisasters) {
    boolean automaticallyClearable = true;
    for (SingularityDisasterType disasterType : newActiveDisasters) {
      if (!disasterType.isAutomaticallyClearable()) {
        automaticallyClearable = false;
        break;
      }
    }

    String message = String.format("Active disasters detected: (%s)%s", newActiveDisasters, automaticallyClearable ? "" : ", action must be re-enabled by an admin user");
    Optional<Long> expiresAt = Optional.absent();
    if (automaticallyClearable) {
      expiresAt = Optional.of(System.currentTimeMillis() + configuration.getDisasterDetection().getDefaultDisabledActionExpiration());
    }

    for (SingularityAction action : configuration.getDisasterDetection().getDisableActionsOnDisaster()) {
      disable(action, Optional.of(message), Optional.<SingularityUser>absent(), automaticallyClearable, expiresAt);
    }
  }

  public void clearSystemGeneratedDisabledActions() {
    for (SingularityDisabledAction disabledAction : getDisabledActions()) {
      if (disabledAction.isAutomaticallyClearable()) {
        enable(disabledAction.getType());
      }
    }
  }

  public void disableAutomatedDisabledActions() {
    create(DISABLE_AUTOMATED_PATH);
  }

  public void enableAutomatedDisabledActions() {
    delete(DISABLE_AUTOMATED_PATH);
  }

  public boolean isAutomatedDisabledActionsDisabled() {
    return exists(DISABLE_AUTOMATED_PATH);
  }

  public boolean isTaskCreditEnabled() {
    return exists(TASK_CREDITS_ENABLED_PATH);
  }

  public void enableTaskCredits() {
    create(TASK_CREDITS_ENABLED_PATH);
  }

  public void disableTaskCredits() {
    delete(TASK_CREDITS_ENABLED_PATH);
  }

  public void enqueueCreditsChange(int credits) {
    save(ZKPaths.makePath(TASK_CREDITS_UPDATES_PATH, UUID.randomUUID().toString()), Optional.of(Integer.toString(credits).getBytes(Charsets.UTF_8)));
  }

  public int getTaskCredits() {
    Optional<String> data = getData(TASK_CREDIT_COUNT_PATH, StringTranscoder.INSTANCE);
    try {
      if (data.isPresent()) {
        return Integer.parseInt(data.get());
      } else {
        return 0;
      }
    } catch (NumberFormatException nfe) {
      LOG.error("Could not read integer from path {}", TASK_CREDIT_COUNT_PATH, nfe);
      return 0;
    }
  }

  public void saveTaskCreditCount(int remaining) {
    save(TASK_CREDIT_COUNT_PATH, Optional.of(Integer.toString(remaining).getBytes(Charsets.UTF_8)));
  }

  public int getUpdatedCreditCount() {
    int currentCredits = getTaskCredits();
    List<String> updates = getChildren(TASK_CREDITS_UPDATES_PATH);
    if (updates.isEmpty()) {
      return currentCredits;
    }
    for (String update : updates) {
      String path = ZKPaths.makePath(TASK_CREDITS_UPDATES_PATH, update);
      try {
        Optional<String> data = getData(path, StringTranscoder.INSTANCE);
        if (data.isPresent()) {
          currentCredits = currentCredits + Integer.parseInt(data.get());
        }
      } catch (NumberFormatException nfe) {
        LOG.error("Could not read integer from path {}", path, nfe);
      } finally {
        delete(path);
      }
    }

    currentCredits = Math.max(currentCredits, 0);
    saveTaskCreditCount(currentCredits);
    return currentCredits;
  }
}
