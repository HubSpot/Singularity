package com.hubspot.singularity.helpers;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Arrays;

import htsjdk.samtools.FileTruncatedException;
import htsjdk.samtools.util.BlockCompressedFilePointerUtil;
import htsjdk.samtools.util.BlockCompressedInputStream;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.WebExceptions;

public class BlockCompressedFileHelper {
  private static final Logger LOG = LoggerFactory.getLogger(BlockCompressedFileHelper.class);

  public static MesosFileChunkObject getChunkAtOffset(URL url, Optional<Long> offset, int length) throws Exception {
    BlockCompressedInputStream stream = new BlockCompressedInputStream(url);
    try {
      if (offset.isPresent()) {
        stream.seek(offset.get());
      }
      byte[] bytes = new byte[length];
      int bytesRead = stream.read(bytes, 0, length);
      LOG.trace("Read {} bytes at offset {} for log at {}", bytesRead, offset, url.getPath());
      long newOffset = stream.getPosition();
      return new MesosFileChunkObject(new String(bytes, Charsets.UTF_8), offset.or(0L), Optional.of(newOffset));
    } catch (FileTruncatedException fte) {
      throw WebExceptions.badRequest(String.format("File at %s is not block compressed", url.getPath()), fte);
    } finally {
      stream.close();
    }
  }

  public static MesosFileChunkObject readInReverseFromOffset(URL url, Optional<Long> offset, int length) throws Exception {
    if (!offset.isPresent() || offset.get() == 0) {
      return new MesosFileChunkObject("", 0L, Optional.of(0L));
    }

    BlockCompressedInputStream stream = new BlockCompressedInputStream(url);

    try {
      long nextOffsetStart = offset.get();
      byte[] bytes = new byte[length];
      long bytesRead = 0;

      while(readFromPreviousBlock(stream, nextOffsetStart, bytes, bytesRead, length)) {
        LOG.trace("Read {} bytes of {} max, moved offset back to {}", bytesRead, length, nextOffsetStart);
      }

      // Offset = largest virtualFilePointer read from, nextOffset = smallest virtualFilePointer read from
      return new MesosFileChunkObject(new String(bytes, Charsets.UTF_8), offset.get(), Optional.of(nextOffsetStart));
    } catch (FileTruncatedException fte) {
      throw WebExceptions.badRequest(String.format("File at %s is not block compressed", url.getPath()), fte);
    } finally {
      stream.close();
    }
  }

  private static boolean readFromPreviousBlock(final BlockCompressedInputStream stream, long nextOffsetStart, byte[] bytes, long bytesRead, final long length) throws IOException {
    long remainingInBlock = BlockCompressedFilePointerUtil.getBlockOffset(nextOffsetStart);
    long blockStart = BlockCompressedFilePointerUtil.getBlockAddress(nextOffsetStart);
    long bytesNeeded = length - bytesRead;

    // Seek to beginning of current block
    stream.seek(makeFilePointer(blockStart, 0));

    byte[] tempBytes = new byte[(int) remainingInBlock];
    int read = stream.read(tempBytes, 0, (int) remainingInBlock);
    int readStart = (int) Math.max(0, read - bytesNeeded);
    nextOffsetStart = Math.max(0, nextOffsetStart - read);
    bytesRead += Math.max(0, read - readStart);
    bytes = concatenate(Arrays.copyOfRange(tempBytes, readStart, read), bytes);

    if (bytesRead >= bytesNeeded || nextOffsetStart == 0) {
      // Read length bytes or reached beginning of the file
      return false;
    } else {
      // Move to end of previous block
      long previousBlockPointer = makeFilePointer(blockStart - 1L, 0);
      stream.seek(previousBlockPointer);
      nextOffsetStart = makeFilePointer(blockStart - 1L, stream.available());
      return true;
    }
  }

  public static byte[] concatenate(byte[] a, byte[] b) {
    int aLen = a.length;
    int bLen = b.length;

    byte[] c = (byte[]) Array.newInstance(a.getClass().getComponentType(), aLen+bLen);
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);

    return c;
  }

  public static long makeFilePointer(long blockAddress, int blockOffset) {
    if(blockOffset < 0) {
      throw new IllegalArgumentException("Negative blockOffset " + blockOffset + " not allowed.");
    } else if(blockAddress < 0L) {
      throw new IllegalArgumentException("Negative blockAddress " + blockAddress + " not allowed.");
    } else if(blockOffset > '\uffff') {
      throw new IllegalArgumentException("blockOffset " + blockOffset + " too large.");
    } else if(blockAddress > 281474976710655L) {
      throw new IllegalArgumentException("blockAddress " + blockAddress + " too large.");
    } else {
      return blockAddress << 16 | (long)blockOffset;
    }
  }
}
