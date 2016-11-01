package com.hubspot.singularity.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import com.ning.http.client.Response;

public class SandboxManagerTest extends SingularityTestBaseNoDb {
  private static final byte[] SNOWMAN_BYTES = "☃".getBytes(StandardCharsets.UTF_8);
  private static final byte FIRST_SNOWMAN_BYTE = SNOWMAN_BYTES[0];
  private static final byte SECOND_SNOWMAN_BYTE = SNOWMAN_BYTES[1];

  @Inject
  private SandboxManager sandboxManager;

  @Test
  public void testInvalidUtf8WithOneByteOfThreeByteCharacter() throws IOException {
    // data contains a ☃ character and the first byte of another ☃ character
    byte[] bytes = toBytes("{\"data\":\"", SNOWMAN_BYTES, FIRST_SNOWMAN_BYTE, "\",\"offset\":123}");

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial ☃ should be dropped
    assertThat(chunk.getData()).isEqualTo("☃");
    assertThat(chunk.getOffset()).isEqualTo(123);
  }

  @Test
  public void testInvalidUtf8WithTwoBytesOfThreeByteCharacter() throws IOException {
    // data contains a ☃ character and the first two bytes of another ☃ character
    byte[] bytes = toBytes("{\"data\":\"", SNOWMAN_BYTES, FIRST_SNOWMAN_BYTE, SECOND_SNOWMAN_BYTE, "\",\"offset\":123}");

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial ☃ should be dropped
    assertThat(chunk.getData()).isEqualTo("☃");
    assertThat(chunk.getOffset()).isEqualTo(123);
  }

  @Test
  public void testValidUtf8WithThreeByteCharacters() throws IOException {
    // data contains two ☃ characters
    byte[] bytes = toBytes("{\"data\":\"", SNOWMAN_BYTES, SNOWMAN_BYTES, "\",\"offset\":123}");

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    assertThat(chunk.getData()).isEqualTo("☃☃");
    assertThat(chunk.getOffset()).isEqualTo(123);
  }

  private static byte[] toBytes(Object... objects) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    for (Object o : objects) {
      if (o instanceof String) {
        output.write(((String) o).getBytes(StandardCharsets.UTF_8));
      } else if (o instanceof byte[]) {
        output.write((byte[]) o);
      } else if (o instanceof Byte) {
        output.write((Byte) o);
      }
    }

    return output.toByteArray();
  }

  private static Response response(byte[] bytes) throws IOException {
    Response response = mock(Response.class);
    when(response.getResponseBodyAsByteBuffer()).thenReturn(ByteBuffer.wrap(bytes));
    return response;
  }
}
