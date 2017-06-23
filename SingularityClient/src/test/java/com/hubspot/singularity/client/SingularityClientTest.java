package com.hubspot.singularity.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpResponse;

public class SingularityClientTest {
  @Mock
  private HttpClient httpClient;
  @Mock
  private HttpResponse response;
  @Mock
  private HttpRequest request;
  @Captor
  private ArgumentCaptor<HttpRequest> requestCaptor;

  private SingularityClient singularityClient;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    singularityClient = buildClient();

    when(response.getRequest())
        .thenReturn(request);
    when(request.getUrl())
        .thenReturn(new URI("test-url"));
  }

  @Test
  public void itRetriesRequestsThatErrorDueToDeadHost() {
    when(httpClient.execute(any()))
        .thenReturn(response);
    when(response.getStatusCode())
        .thenReturn(503)
        .thenReturn(200);
    when(response.isServerError())
        .thenReturn(true)
        .thenReturn(false);

    singularityClient.pauseSingularityRequest("requestId", Optional.absent());

    verify(httpClient, times(2))
        .execute(requestCaptor.capture());
    HttpRequest sentRequest = requestCaptor.getValue();
    assertThat(sentRequest.getUrl().toString())
        .matches("http://host(1|2)/singularity/v2/api/requests/request/requestId/pause");
  }

  @Test
  public void itThrowsAnExceptionOnServerErrors() {
    when(httpClient.execute(any()))
        .thenReturn(response);
    when(response.getStatusCode())
        .thenReturn(500);
    when(response.isError())
        .thenReturn(true);

    assertThatExceptionOfType(SingularityClientException.class)
        .isThrownBy(() -> singularityClient.pauseSingularityRequest("requestId", Optional.absent()));
  }

  private SingularityClient buildClient() {
    return new SingularityClient("singularity/v2/api", httpClient, ImmutableList.of("host1", "host2"), Optional.absent());
  }
}
