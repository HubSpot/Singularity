package com.hubspot.singularity.event;

import com.hubspot.singularity.api.deploy.SingularityDeployUpdate;
import com.hubspot.singularity.api.request.SingularityRequestHistory;
import com.hubspot.singularity.api.task.SingularityTaskHistoryUpdate;

public interface SingularityEventListener {
  void requestHistoryEvent(SingularityRequestHistory singularityRequestHistory);

  void taskHistoryUpdateEvent(SingularityTaskHistoryUpdate singularityTaskHistoryUpdate);

  void deployHistoryEvent(SingularityDeployUpdate singularityDeployUpdate);
}
