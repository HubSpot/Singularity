package com.hubspot.singularity.data.history;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityDeployHistory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.SingularityMappers.SingularityRequestIdCount;

public class NoopHistoryManager implements HistoryManager {

  @Inject
  public NoopHistoryManager() {
  }

  @Override
  public void saveRequestHistoryUpdate(SingularityRequestHistory requestHistory) {
    throw new UnsupportedOperationException("NoopHistoryManager can not save");
  }

  @Override
  public void saveTaskHistory(SingularityTaskHistory taskHistory) {
    throw new UnsupportedOperationException("NoopHistoryManager can not save");
  }

  @Override
  public void saveDeployHistory(SingularityDeployHistory deployHistory) {
    throw new UnsupportedOperationException("NoopHistoryManager can not save");
  }

  @Override
  public Optional<SingularityDeployHistory> getDeployHistory(String requestId, String deployId) {
    return Optional.absent();
  }

  @Override
  public List<SingularityDeployHistory> getDeployHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return Collections.emptyList();
  }

  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(String requestId, Integer limitStart, Integer limitCount) {
    return Collections.emptyList();
  }

  @Override
  public List<SingularityTaskIdHistory> getTaskHistoryForDeploy(String requestId, String deployId, Integer limitStart, Integer limitCount) {
    return Collections.emptyList();
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistory(String taskId) {
    return Optional.absent();
  }

  @Override
  public Optional<SingularityTaskHistory> getTaskHistoryByRunId(String runId) {
    return Optional.absent();
  }

  @Override
  public List<SingularityRequestHistory> getRequestHistory(String requestId, Optional<OrderDirection> orderDirection, Integer limitStart, Integer limitCount) {
    return Collections.emptyList();
  }

  @Override
  public List<String> getRequestHistoryLike(String requestIdLike, Integer limitStart, Integer limitCount) {
    return Collections.emptyList();
  }

  @Override
  public List<SingularityRequestIdCount> getRequestIdCounts(Date before) {
    return Collections.emptyList();
  }

  @Override
  public void purgeTaskHistory(String requestId, int count, Optional<Integer> limit, Optional<Date> purgeBefore, boolean deleteRowInsteadOfUpdate) {
    throw new UnsupportedOperationException("NoopHistoryManager can not update/delete");
  }

}
