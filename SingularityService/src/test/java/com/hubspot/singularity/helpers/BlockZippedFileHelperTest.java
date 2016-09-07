package com.hubspot.singularity.helpers;

import java.io.File;
import java.net.URL;
import java.util.Random;

import htsjdk.samtools.util.BlockCompressedInputStream;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockZippedFileHelperTest {
  private static final Logger LOG = LoggerFactory.getLogger(BlockZippedFileHelperTest.class);

  private static final int CHUNK_SIZE_BYTES = 10000;
  private static final int TOTAL_FILE_SIZE = 591442;
  private static final String TEST_URL = "test";
  private static final String TEST_URL_INVALID = "testinvalid";


  @Test
  public void testDecompressFileInChunksAfterSeek() throws Exception {
    long initialOffset = CHUNK_SIZE_BYTES * new Random().nextInt(4);
    long offset = initialOffset;
    boolean available = true;
    byte[] bytes = new byte[CHUNK_SIZE_BYTES];
    int read = 0;
    while (available) {
      BlockCompressedInputStream stream = new BlockCompressedInputStream(new URL(TEST_URL));
      stream.seek(offset);
      bytes = new byte[CHUNK_SIZE_BYTES];
      read += stream.read(bytes, 0, CHUNK_SIZE_BYTES);
      LOG.debug("read: {}", read);
      offset = stream.getPosition();
      LOG.debug("offset: {}", offset);
      available = stream.available() > 0;
    }
    Assert.assertEquals(TOTAL_FILE_SIZE - initialOffset, read);
  }

  @Test( expected = RuntimeException.class)
  public void testTryInvalidFile() throws Exception {
    long offset = CHUNK_SIZE_BYTES * new Random().nextInt(10);
    boolean available = true;
    while (available) {
      BlockCompressedInputStream stream = new BlockCompressedInputStream(new URL(TEST_URL_INVALID));
      BlockCompressedInputStream.isValidFile(stream);
      stream.seek(offset);
      byte[] bytes = new byte[CHUNK_SIZE_BYTES];
      stream.read(bytes, 0, CHUNK_SIZE_BYTES);
      offset = stream.getPosition();
      available = stream.available() > 0;
    }
  }
}
