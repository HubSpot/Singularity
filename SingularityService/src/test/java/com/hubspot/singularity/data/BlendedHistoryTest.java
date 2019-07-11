package com.hubspot.singularity.data;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;
import com.hubspot.singularity.scheduler.SingularitySchedulerTestBase;

public class BlendedHistoryTest extends SingularitySchedulerTestBase {

  @Inject
  private RequestManager requestManager;

  public BlendedHistoryTest() {
    super(false);
  }

  private void mockRequestHistory(HistoryManager hm, List<SingularityRequestHistory> returnValue) {
    when(hm.getRequestHistory(ArgumentMatchers.anyString(), ArgumentMatchers.<Optional<OrderDirection>>any(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt())).thenReturn(returnValue);
  }

  private SingularityRequest request;

  private void saveHistory(long createdAt, RequestHistoryType type) {
    requestManager.saveHistory(makeHistory(createdAt, type));
  }

  private SingularityRequestHistory makeHistory(long createdAt, RequestHistoryType type) {
    return new SingularityRequestHistory(createdAt, Optional.<String> absent(), type, request, Optional.<String> absent());
  }

  // DESCENDING
  @Test
  public void testBlendedRequestHistory() {
    HistoryManager hm = mock(HistoryManager.class);
    String rid = "rid";
    request = new SingularityRequestBuilder(rid, RequestType.WORKER).build();
    RequestHistoryHelper rhh = new RequestHistoryHelper(requestManager, hm, new SingularityConfiguration());

    mockRequestHistory(hm, Collections.<SingularityRequestHistory> emptyList());

    Assertions.assertTrue(rhh.getBlendedHistory(rid, 0, 100).isEmpty());
    Assertions.assertTrue(!rhh.getFirstHistory(rid).isPresent());
    Assertions.assertTrue(!rhh.getLastHistory(rid).isPresent());

    mockRequestHistory(hm, Arrays.asList(makeHistory(52, RequestHistoryType.EXITED_COOLDOWN), makeHistory(51, RequestHistoryType.ENTERED_COOLDOWN), makeHistory(50, RequestHistoryType.CREATED)));

    List<SingularityRequestHistory> history = rhh.getBlendedHistory(rid, 0, 5);

    Assertions.assertTrue(history.size() == 3);

    saveHistory(100, RequestHistoryType.DELETED);
    saveHistory(120, RequestHistoryType.CREATED);

    history = rhh.getBlendedHistory(rid, 0, 5);

    Assertions.assertTrue(history.size() == 5);
    Assertions.assertTrue(history.get(0).getCreatedAt() == 120);
    Assertions.assertTrue(history.get(4).getCreatedAt() == 50);

    history = rhh.getBlendedHistory(rid, 1, 5);

    Assertions.assertTrue(history.size() == 4);
    Assertions.assertTrue(history.get(0).getCreatedAt() == 100);
    Assertions.assertTrue(history.get(3).getCreatedAt() == 50);

    history = rhh.getBlendedHistory(rid, 2, 5);

    Assertions.assertTrue(history.size() == 3);
    Assertions.assertTrue(history.get(0).getCreatedAt() == 52);
    Assertions.assertTrue(history.get(2).getCreatedAt() == 50);

    mockRequestHistory(hm, Collections.<SingularityRequestHistory> emptyList());

    history = rhh.getBlendedHistory(rid, 3, 5);
    Assertions.assertTrue(history.isEmpty());

    history = rhh.getBlendedHistory(rid, 1, 5);
    Assertions.assertTrue(history.size() == 1);
    Assertions.assertTrue(history.get(0).getCreatedAt() == 100);

    Assertions.assertTrue(rhh.getFirstHistory(rid).get().getCreatedAt() == 100);
    Assertions.assertTrue(rhh.getLastHistory(rid).get().getCreatedAt() == 120);

    mockRequestHistory(hm, Arrays.asList(makeHistory(1, RequestHistoryType.EXITED_COOLDOWN)));

    Assertions.assertTrue(rhh.getFirstHistory(rid).get().getCreatedAt() == 1);
    Assertions.assertTrue(rhh.getLastHistory(rid).get().getCreatedAt() == 120);
  }
}
