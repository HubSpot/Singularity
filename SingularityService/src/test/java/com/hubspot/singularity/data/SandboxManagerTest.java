package com.hubspot.singularity.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.ning.http.client.Response;
import com.ning.http.util.Base64;

public class SandboxManagerTest extends SingularityTestBaseNoDb {

  @Inject
  private SandboxManager sandboxManager;

  @Test
  public void testInvalidUtf8WithOneByteOfThreeByteCharacter() throws IOException {
    // data contains a ☃ character and the first byte of another ☃ character
    String base64 = "eyJkYXRhIjoi4piD4iIsIm9mZnNldCI6MjkwfQ==";

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(base64));
    // the partial ☃ should be dropped
    assertThat(chunk.getData()).isEqualTo("☃");
    assertThat(chunk.getOffset()).isEqualTo(290);
  }

  @Test
  public void testInvalidUtf8WithTwoBytesOfThreeByteCharacter() throws IOException {
    // data contains a ☃ character and the first two bytes of another ☃ character
    String base64 = "eyJkYXRhIjoi4piD4pgiLCJvZmZzZXQiOjI5MH0=";

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(base64));
    // the partial ☃ should be dropped
    assertThat(chunk.getData()).isEqualTo("☃");
    assertThat(chunk.getOffset()).isEqualTo(290);
  }

  @Test
  public void testValidUtf8WithThreeByteCharacters() throws IOException {
    // data contains two ☃ characters
    String base64 = "eyJkYXRhIjoi4piD4piDIiwib2Zmc2V0IjoyOTB9";

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(base64));
    assertThat(chunk.getData()).isEqualTo("☃☃");
    assertThat(chunk.getOffset()).isEqualTo(290);
  }

  private static Response response(String base64) throws IOException {
    Response response = mock(Response.class);
    byte[] bytes = Base64.decode(base64);
    when(response.getResponseBodyAsByteBuffer()).thenReturn(ByteBuffer.wrap(bytes));
    return response;
  }
}
