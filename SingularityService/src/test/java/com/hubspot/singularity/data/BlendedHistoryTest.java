package com.hubspot.singularity.data;

import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.OrderDirection;
import com.hubspot.singularity.RequestType;
import com.hubspot.singularity.SingularityRequest;
import com.hubspot.singularity.SingularityRequestBuilder;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityRequestHistory.RequestHistoryType;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.RequestHistoryHelper;

public class BlendedHistoryTest extends SingularityTestBaseNoDb {

  @Inject
  private RequestManager requestManager;

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
    RequestHistoryHelper rhh = new RequestHistoryHelper(requestManager, hm);

    mockRequestHistory(hm, Collections.<SingularityRequestHistory> emptyList());

    Assert.assertTrue(rhh.getBlendedHistory(rid, 0, 100).isEmpty());
    Assert.assertTrue(!rhh.getFirstHistory(rid).isPresent());
    Assert.assertTrue(!rhh.getLastHistory(rid).isPresent());

    mockRequestHistory(hm, Arrays.asList(makeHistory(52, RequestHistoryType.EXITED_COOLDOWN), makeHistory(51, RequestHistoryType.ENTERED_COOLDOWN), makeHistory(50, RequestHistoryType.CREATED)));

    List<SingularityRequestHistory> history = rhh.getBlendedHistory(rid, 0, 5);

    Assert.assertTrue(history.size() == 3);

    saveHistory(100, RequestHistoryType.DELETED);
    saveHistory(120, RequestHistoryType.CREATED);

    history = rhh.getBlendedHistory(rid, 0, 5);

    Assert.assertTrue(history.size() == 5);
    Assert.assertTrue(history.get(0).getCreatedAt() == 120);
    Assert.assertTrue(history.get(4).getCreatedAt() == 50);

    history = rhh.getBlendedHistory(rid, 1, 5);

    Assert.assertTrue(history.size() == 4);
    Assert.assertTrue(history.get(0).getCreatedAt() == 100);
    Assert.assertTrue(history.get(3).getCreatedAt() == 50);

    history = rhh.getBlendedHistory(rid, 2, 5);

    Assert.assertTrue(history.size() == 3);
    Assert.assertTrue(history.get(0).getCreatedAt() == 52);
    Assert.assertTrue(history.get(2).getCreatedAt() == 50);

    mockRequestHistory(hm, Collections.<SingularityRequestHistory> emptyList());

    history = rhh.getBlendedHistory(rid, 3, 5);
    Assert.assertTrue(history.isEmpty());

    history = rhh.getBlendedHistory(rid, 1, 5);
    Assert.assertTrue(history.size() == 1);
    Assert.assertTrue(history.get(0).getCreatedAt() == 100);

    Assert.assertTrue(rhh.getFirstHistory(rid).get().getCreatedAt() == 100);
    Assert.assertTrue(rhh.getLastHistory(rid).get().getCreatedAt() == 120);

    mockRequestHistory(hm, Arrays.asList(makeHistory(1, RequestHistoryType.EXITED_COOLDOWN)));

    Assert.assertTrue(rhh.getFirstHistory(rid).get().getCreatedAt() == 1);
    Assert.assertTrue(rhh.getLastHistory(rid).get().getCreatedAt() == 120);
  }
}
