package com.hubspot.singularity.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosBinaryChunkObject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.UTF8String;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class SandboxManagerTest extends SingularityTestBaseNoDb {

  @Inject
  private SandboxManager sm;
  @Inject
  private ObjectMapper objectMapper;

  // valid ASCII "Hi I'm valid ASCII"
  private byte[] validString = {(byte)0b01001000, (byte)0b01101001, (byte)0b00100000,
      (byte)0b01001001, (byte)0b00100111, (byte)0b01101101, (byte)0b00100000,
      (byte)0b01110110, (byte)0b01100001, (byte)0b01101100, (byte)0b01101001,
      (byte)0b01100100, (byte)0b00100000, (byte)0b01000001, (byte)0b01010011,
      (byte)0b01000011, (byte)0b01001001, (byte)0b01001001};

  // valid UTF-8 "✓Hi I'm ✓valid✓ UTF-8 ✓"
  private byte[] validUTF8 = {(byte)0b11100010, (byte)0b10011100, (byte)0b10010011,
      (byte)0b01001000, (byte)0b01101001, (byte)0b00100000, (byte)0b01001001,
      (byte)0b00100111, (byte)0b01101101, (byte)0b00100000, (byte)0b11100010,
      (byte)0b10011100, (byte)0b10010011, (byte)0b01110110, (byte)0b01100001,
      (byte)0b01101100, (byte)0b01101001, (byte)0b01100100, (byte)0b11100010,
      (byte)0b10011100, (byte)0b10010011, (byte)0b00100000, (byte)0b01010101,
      (byte)0b01010100, (byte)0b01000110, (byte)0b00101101, (byte)0b00111000,
      (byte)0b00100000, (byte)0b11100010, (byte)0b10011100, (byte)0b10010011};

  // invalid cent [2-byte] character - 1 byte(s) cut from beginning
  private byte[] invalidBeginningCent = {(byte)0b10100010};
  // invalid check [3-byte] character - 1 byte(s) cut from beginning
  private byte[] invalidBeginningCheck = {(byte)0b10011100, (byte)0b10010011};
  // invalid pillow [4-byte] character - 1 byte(s) cut from beginning
  private byte[] invalidBeginningPillow = {(byte)0b10100000, (byte)0b10110001, (byte)0b10111000};
  // invalid cent [2-byte] character - 1 byte(s) cut from end
  private byte[] invalidEndCent1 = {(byte)0b11000010};
  // invalid check [3-byte] character - 1 byte(s) cut from end
  private byte[] invalidEndCheck1 = {(byte)0b11100010, (byte)0b10011100};
  // invalid pillow [4-byte] character - 1 byte(s) cut from end
  private byte[] invalidEndPillow1 = {(byte)0b11110000, (byte)0b10100000, (byte)0b10110001};
  // invalid check [3-byte] character - 2 byte(s) cut from end
  private byte[] invalidEndCheck2 = {(byte)0b11100010};
  // invalid pillow [4-byte] character - 2 byte(s) cut from end
  private byte[] invalidEndPillow2 = {(byte)0b11110000, (byte)0b10100000};
  // invalid pillow [4-byte] character - 3 byte(s) cut from end
  private byte[] invalidEndPillow3 = {(byte)0b11110000};

  private byte[] validCent = {(byte)0b11000010, (byte)0b10100010};
  private byte[] validCheck = {(byte)0b11100010, (byte)0b10011100, (byte)0b10010011};
  private byte[] validPillow = {(byte)0b11110000, (byte)0b10100000, (byte)0b10110001, (byte)0b10111000};

  private Optional<MesosBinaryChunkObject> makeMBCO(byte[] firstArray, byte[] secondArray) {
    final byte[] data = ArrayUtils.addAll(firstArray, secondArray);

    return Optional.of(new MesosBinaryChunkObject(
        new UTF8String(data),
        0,
        Optional.of((long)data.length)
    ));
  }

  private byte[] getBytesFromMBCO(Optional<MesosBinaryChunkObject> mbco) {
    UTF8String str = mbco.get().getData();
    byte[] resultBytes = new byte[str.getLength()];
    System.arraycopy(str.getData(), str.getOffset(), resultBytes, 0, str.getLength());

    return resultBytes;
  }

  @Test
  public void stripInvalidBeginningBytes() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningCent, validString))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningCheck, validString))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningPillow, validString))),
        validString
    );
  }


  @Test
  public void stripInvalidEndBytes() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndCent1))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndCheck1))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndPillow1))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndCheck2))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndPillow2))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndPillow3))),
        validString
    );
  }

  @Test
  public void keepValidStrings() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, new byte[0]))),
        validString
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validUTF8, new byte[0]))),
        validUTF8
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validUTF8, validString))),
        ArrayUtils.addAll(validUTF8, validString)
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, validUTF8))),
        ArrayUtils.addAll(validString, validUTF8)
    );
  }

  @Test
  public void stripTinyBufferUTF8BeginningBytes() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningCent, new byte[0]))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningCheck, new byte[0]))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningPillow, new byte[0]))),
        new byte[0]
    );
  }

  @Test
  public void stripTinyBufferUTF8EndBytes() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(new byte[0], invalidEndCent1))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(new byte[0], invalidEndCheck1))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(new byte[0], invalidEndPillow1))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(new byte[0], invalidEndCheck2))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(new byte[0], invalidEndPillow2))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(new byte[0], invalidEndPillow3))),
        new byte[0]
    );
  }

  @Test
  public void keepValidUTF8BeginningBytes() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validCent, validString))),
        ArrayUtils.addAll(validCent, validString)
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validCheck, validString))),
        ArrayUtils.addAll(validCheck, validString)
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validPillow, validString))),
        ArrayUtils.addAll(validPillow, validString)
    );
  }

  @Test
  public void keepValidUTF8EndBytes() {
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, validCent))),
        ArrayUtils.addAll(validString, validCent)
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, validCheck))),
        ArrayUtils.addAll(validString, validCheck)
    );
    assertArrayEquals(
        getBytesFromMBCO(SandboxManager.stripInvalidUTF8(makeMBCO(validString, validPillow))),
        ArrayUtils.addAll(validString, validPillow)
    );
  }

  @Test
  public void validBeginningOffsetChange() {
    assertEquals(
        SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningCent, validString)).get().getOffset(),
        makeMBCO(invalidBeginningCent, validString).get().getOffset() + 1
    );
    assertEquals(
        SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningCheck, validString)).get().getOffset(),
        makeMBCO(invalidBeginningCheck, validString).get().getOffset() + 2
    );
    assertEquals(
        SandboxManager.stripInvalidUTF8(makeMBCO(invalidBeginningPillow, validString)).get().getOffset(),
        makeMBCO(invalidBeginningPillow, validString).get().getOffset() + 3
    );
  }

  @Test
  public void validEndNextOffsetChange() {
    assertEquals(
        (long) SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndCent1)).get().getNextOffset().get(),
        makeMBCO(validString, invalidEndCent1).get().getNextOffset().get() - 1
    );
    assertEquals(
        (long) SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndCheck1)).get().getNextOffset().get(),
        makeMBCO(validString, invalidEndCheck1).get().getNextOffset().get() - 2
    );
    assertEquals(
        (long) SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndPillow1)).get().getNextOffset().get(),
        makeMBCO(validString, invalidEndPillow1).get().getNextOffset().get() - 3
    );

    assertEquals(
        (long) SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndCheck2)).get().getNextOffset().get(),
        makeMBCO(validString, invalidEndCheck2).get().getNextOffset().get() - 1
    );
    assertEquals(
        (long) SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndPillow2)).get().getNextOffset().get(),
        makeMBCO(validString, invalidEndPillow2).get().getNextOffset().get() - 2
    );

    assertEquals(
        (long) SandboxManager.stripInvalidUTF8(makeMBCO(validString, invalidEndPillow3)).get().getNextOffset().get(),
        makeMBCO(validString, invalidEndPillow3).get().getNextOffset().get() - 1
    );
  }


  @Test
  public void testParseMesosResponse() throws Exception {
    final long MESOS_OFFSET = 283275599;
    final String MESOS_DATA = "fetches in 5 ms\\n14:35:08.594 [Executor task launch worker-82] INFO  o.a.s.s.ShuffleBlockFetch";
    final String MESOS_JSON = "{\"data\":\"" + MESOS_DATA + "\",\"offset\":" + MESOS_OFFSET + "}";


    final MesosBinaryChunkObject binaryChunkObject = objectMapper.readValue(MESOS_JSON, MesosBinaryChunkObject.class);

    assertEquals(MESOS_OFFSET, binaryChunkObject.getOffset());

    final String serializedJSON = objectMapper.writeValueAsString(binaryChunkObject);

    final MesosFileChunkObject chunkObject = objectMapper.readValue(serializedJSON, MesosFileChunkObject.class);

    assertEquals(MESOS_DATA, chunkObject.getData());
  }

}
