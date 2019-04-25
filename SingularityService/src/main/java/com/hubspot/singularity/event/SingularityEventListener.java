package com.hubspot.singularity.event;

import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskWebhook;

public interface SingularityEventListener {
  void requestHistoryEvent(SingularityRequestHistory singularityRequestHistory);

  void taskHistoryUpdateEvent(SingularityTaskWebhook singularityTaskWebhook);

  void deployHistoryEvent(SingularityDeployUpdate singularityDeployUpdate);
}
