package com.hubspot.singularity.event;

import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;

public interface SingularityEventListener {
  void requestHistoryEvent(SingularityRequestHistory singularityRequestHistory);

  void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate singularityTaskHistoryUpdate);

  void deployHistoryEvent(SingularityDeployUpdate singularityDeployUpdate);
}
