package com.hubspot.singularity.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDisabledAction;
import com.hubspot.singularity.config.SingularityConfiguration;

public class DisabledActionManager extends CuratorManager {
  private static final String DISABLED_ACTIONS_ROOT = "/disabled-actions";

  private static final String MESSAGE_FORMAT = "Cannot %s: %s";
  private static final String DEFAULT_MESSAGE = "Action is currently disabled";

  @Inject
  public DisabledActionManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry) {
    super(curator, configuration, metricRegistry);
  }

  private String getActionPath(SingularityDisabledAction action) {
    return ZKPaths.makePath(DISABLED_ACTIONS_ROOT, action.name());
  }

  public boolean isDisabled(SingularityDisabledAction action) {
    return exists(getActionPath(action));
  }

  public String getDisabledActionMessage(SingularityDisabledAction action) {
    Optional<String> maybeMessage = getStringData(getActionPath(action));
    return String.format(MESSAGE_FORMAT, action, maybeMessage.or(DEFAULT_MESSAGE));
  }

  public SingularityCreateResult disable(SingularityDisabledAction action, Optional<String> maybeMessage) {
    byte[] messageBytes = maybeMessage.or(DEFAULT_MESSAGE).getBytes(StandardCharsets.UTF_8);
    return save(getActionPath(action), Optional.of(messageBytes));
  }

  public SingularityDeleteResult enable(SingularityDisabledAction action) {
    return delete(getActionPath(action));
  }

  public List<SingularityDisabledAction> getDisabledActions() {
    List<String> actions = getChildren(DISABLED_ACTIONS_ROOT);

    List<SingularityDisabledAction> actionEnums = new ArrayList<>();
    for (String action : actions) {
      actionEnums.add(SingularityDisabledAction.valueOf(action));
    }

    return actionEnums;
  }
}
