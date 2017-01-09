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
  private static final int DEFAULT_OFFSET = 123;

  private static final String JSON_START = "{\"data\":\"";
  private static final String JSON_END = "\",\"offset\":" + DEFAULT_OFFSET + "}";

  private static final String SNOWMAN = "☃";
  private static final byte[] SNOWMAN_UTF8_BYTES = SNOWMAN.getBytes(StandardCharsets.UTF_8);
  private static final byte FIRST_SNOWMAN_BYTE = SNOWMAN_UTF8_BYTES[0];
  private static final byte SECOND_SNOWMAN_BYTE = SNOWMAN_UTF8_BYTES[1];

  private static final String BALLOON = "\uD83C\uDF88";
  private static final byte[] BALLOON_BYTES = BALLOON.getBytes(StandardCharsets.UTF_8);
  private static final byte SECOND_BALLOON_BYTE = BALLOON_BYTES[1];
  private static final byte THIRD_BALLOON_BYTE = BALLOON_BYTES[2];

  @Inject
  private SandboxManager sandboxManager;

  @Test
  public void testInvalidUtf8WithOneByteOfThreeByteCharacter() throws IOException {
    // data contains a ☃ character and the first byte of another ☃ character
    byte[] bytes = toBytes(JSON_START, SNOWMAN_UTF8_BYTES, FIRST_SNOWMAN_BYTE, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial ☃ should be dropped
    assertThat(chunk.getData()).isEqualTo(SNOWMAN);
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET);
  }

  @Test
  public void testInvalidUtf8WithTwoBytesOfThreeByteCharacter() throws IOException {
    // data contains a ☃ character and the first two bytes of another ☃ character
    byte[] bytes = toBytes(JSON_START, SNOWMAN_UTF8_BYTES, FIRST_SNOWMAN_BYTE, SECOND_SNOWMAN_BYTE, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial ☃ should be dropped
    assertThat(chunk.getData()).isEqualTo(SNOWMAN);
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET);
  }

  @Test
  public void testValidUtf8WithThreeByteCharacters() throws IOException {
    // data contains two ☃ characters
    byte[] bytes = toBytes(JSON_START, SNOWMAN_UTF8_BYTES, SNOWMAN_UTF8_BYTES, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // nothing should be dropped
    assertThat(chunk.getData()).isEqualTo(SNOWMAN + SNOWMAN);
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET);
  }

  @Test
  public void testInvalidUtf8WithLastByte() throws IOException {
    // data contains last byte of a fire character and a ☃ character
    byte[] bytes = toBytes(JSON_START, THIRD_BALLOON_BYTE, SNOWMAN_UTF8_BYTES, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial fire should be dropped and the offset should be advanced by one byte
    assertThat(chunk.getData()).isEqualTo(SNOWMAN);
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET + 1);
  }

  @Test
  public void testInvalidUtf8WithLastTwoBytes() throws IOException {
    // data contains last two bytes of a fire character and a ☃ character
    byte[] bytes = toBytes(JSON_START, SECOND_BALLOON_BYTE, THIRD_BALLOON_BYTE, SNOWMAN_UTF8_BYTES, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial fire should be dropped and the offset should be advanced by two bytes
    assertThat(chunk.getData()).isEqualTo(SNOWMAN);
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET + 2);
  }

  @Test
  public void testInvalidUtf8WithOneByte() throws IOException {
    // data contains the last middle byte of a fire character
    byte[] bytes = toBytes(JSON_START, SECOND_BALLOON_BYTE, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial fire should be dropped and the offset should be advanced by one byte
    assertThat(chunk.getData()).isEqualTo("");
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET + 1);
  }

  @Test
  public void testInvalidUtf8WithTwoBytes() throws IOException {
    // data contains the last two bytes of a fire character
    byte[] bytes = toBytes(JSON_START, SECOND_BALLOON_BYTE, THIRD_BALLOON_BYTE, JSON_END);

    MesosFileChunkObject chunk = sandboxManager.parseResponseBody(response(bytes));
    // the partial fire should be dropped and the offset should be advanced by two bytes
    assertThat(chunk.getData()).isEqualTo("");
    assertThat(chunk.getOffset()).isEqualTo(DEFAULT_OFFSET + 2);
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
