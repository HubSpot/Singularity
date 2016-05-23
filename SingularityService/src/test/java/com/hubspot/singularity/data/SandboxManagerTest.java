package com.hubspot.singularity.data;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.SingularityTestBaseNoDb;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class SandboxManagerTest extends SingularityTestBaseNoDb {

  @Inject
  private SandboxManager sm;

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

  private Optional<MesosFileChunkObject> makeMFCO(byte[] firstArray, byte[] secondArray) {
    final byte[] data = ArrayUtils.addAll(firstArray, secondArray);

    return Optional.of(new MesosFileChunkObject(
        ByteBuffer.wrap(data, 0, data.length),
        0,
        Optional.of((long)data.length)
    ));
  }

  private byte[] getBytesFromMFCO(Optional<MesosFileChunkObject> mfco) {
    byte[] resultBytes = new byte[mfco.get().getData().remaining()];
    mfco.get().getData().duplicate().get(resultBytes);

    return resultBytes;
  }

  @Test
  public void stripInvalidBeginningBytes() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(invalidBeginningCent, validString))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(invalidBeginningCheck, validString))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(invalidBeginningPillow, validString))),
        validString
    );
  }


  @Test
  public void stripInvalidEndBytes() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, invalidEndCent1))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, invalidEndCheck1))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, invalidEndPillow1))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, invalidEndCheck2))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, invalidEndPillow2))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, invalidEndPillow3))),
        validString
    );
  }

  @Test
  public void keepValidStrings() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, new byte[0]))),
        validString
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validUTF8, new byte[0]))),
        validUTF8
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validUTF8, validString))),
        ArrayUtils.addAll(validUTF8, validString)
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, validUTF8))),
        ArrayUtils.addAll(validString, validUTF8)
    );
  }

  @Test
  public void stripTinyBufferUTF8BeginningBytes() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(invalidBeginningCent, new byte[0]))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(invalidBeginningCheck, new byte[0]))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(invalidBeginningPillow, new byte[0]))),
        new byte[0]
    );
  }

  @Test
  public void stripTinyBufferUTF8EndBytes() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(new byte[0], invalidEndCent1))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(new byte[0], invalidEndCheck1))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(new byte[0], invalidEndPillow1))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(new byte[0], invalidEndCheck2))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(new byte[0], invalidEndPillow2))),
        new byte[0]
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(new byte[0], invalidEndPillow3))),
        new byte[0]
    );
  }

  @Test
  public void keepValidUTF8BeginningBytes() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validCent, validString))),
        ArrayUtils.addAll(validCent, validString)
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validCheck, validString))),
        ArrayUtils.addAll(validCheck, validString)
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validPillow, validString))),
        ArrayUtils.addAll(validPillow, validString)
    );
  }

  @Test
  public void keepValidUTF8EndBytes() {
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, validCent))),
        ArrayUtils.addAll(validString, validCent)
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, validCheck))),
        ArrayUtils.addAll(validString, validCheck)
    );
    assertArrayEquals(
        getBytesFromMFCO(sm.stripInvalidUTF8(makeMFCO(validString, validPillow))),
        ArrayUtils.addAll(validString, validPillow)
    );
  }

  @Test
  public void validBeginningOffsetChange() {
    assertEquals(
        sm.stripInvalidUTF8(makeMFCO(invalidBeginningCent, validString)).get().getOffset(),
        makeMFCO(invalidBeginningCent, validString).get().getOffset() + 1
    );
    assertEquals(
        sm.stripInvalidUTF8(makeMFCO(invalidBeginningCheck, validString)).get().getOffset(),
        makeMFCO(invalidBeginningCheck, validString).get().getOffset() + 2
    );
    assertEquals(
        sm.stripInvalidUTF8(makeMFCO(invalidBeginningPillow, validString)).get().getOffset(),
        makeMFCO(invalidBeginningPillow, validString).get().getOffset() + 3
    );
  }

  @Test
  public void validEndNextOffsetChange() {
    assertEquals(
        (long) sm.stripInvalidUTF8(makeMFCO(validString, invalidEndCent1)).get().getNextOffset().get(),
        makeMFCO(validString, invalidEndCent1).get().getNextOffset().get() - 1
    );
    assertEquals(
        (long) sm.stripInvalidUTF8(makeMFCO(validString, invalidEndCheck1)).get().getNextOffset().get(),
        makeMFCO(validString, invalidEndCheck1).get().getNextOffset().get() - 2
    );
    assertEquals(
        (long) sm.stripInvalidUTF8(makeMFCO(validString, invalidEndPillow1)).get().getNextOffset().get(),
        makeMFCO(validString, invalidEndPillow1).get().getNextOffset().get() - 3
    );

    assertEquals(
        (long) sm.stripInvalidUTF8(makeMFCO(validString, invalidEndCheck2)).get().getNextOffset().get(),
        makeMFCO(validString, invalidEndCheck2).get().getNextOffset().get() - 1
    );
    assertEquals(
        (long) sm.stripInvalidUTF8(makeMFCO(validString, invalidEndPillow2)).get().getNextOffset().get(),
        makeMFCO(validString, invalidEndPillow2).get().getNextOffset().get() - 2
    );

    assertEquals(
        (long) sm.stripInvalidUTF8(makeMFCO(validString, invalidEndPillow3)).get().getNextOffset().get(),
        makeMFCO(validString, invalidEndPillow3).get().getNextOffset().get() - 1
    );
  }

}
