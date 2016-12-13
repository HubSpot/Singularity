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
      long newOffset = stream.getPosition();
      LOG.trace("Read {} bytes at offset {} (next offset is {}) for log at {}", bytesRead, offset, newOffset, url.getPath());
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
      byte[] bytes = new byte[0];
      long bytesRead = 0;
      boolean continueReading = true;

      while(continueReading) {
        long remainingInBlock = BlockCompressedFilePointerUtil.getBlockOffset(nextOffsetStart);
        long blockStart = BlockCompressedFilePointerUtil.getBlockAddress(nextOffsetStart);
        long bytesNeeded = length - bytesRead;

        LOG.trace("Block starting at {} has {} bytes remaining, need to read {}", blockStart, remainingInBlock, bytesNeeded);

        // Seek to beginning of current block or back as many bytes as we need to read from the starting offset
        int neededInBlock = (int) Math.min(remainingInBlock, bytesNeeded);
        int inBlockOffset = (int) remainingInBlock - neededInBlock;
        stream.seek(makeFilePointer(blockStart, inBlockOffset));

        // Read the needed number of bytes into tempBytes and update our possible next offset and total bytes
        byte[] tempBytes = new byte[neededInBlock];
        int read = stream.read(tempBytes, 0, neededInBlock);
        int readStart = (int) Math.max(0, read - bytesNeeded);
        nextOffsetStart = Math.max(0, nextOffsetStart - read);
        bytesRead += Math.max(0, read - readStart);
        LOG.trace("Read {} total bytes", bytesRead);

        // Copy the bytes we read into our overall result
        bytes = concatenate(Arrays.copyOfRange(tempBytes, readStart, read), bytes);

        if (bytesRead >= bytesNeeded || nextOffsetStart == 0) {
          // Finished reading length bytes or reached beginning of the file
          continueReading = false;
        } else {
          // Move to end of previous block
          long previousBlockPointer = makeFilePointer(Math.max((blockStart >> 16) - 1L, 0L), 0);
          stream.seek(previousBlockPointer);
          nextOffsetStart = makeFilePointer(previousBlockPointer, stream.available());
          continueReading = true;
        }
        LOG.trace("Read {} bytes of {} max, moved offset back to {}", bytesRead, length, nextOffsetStart);
      }
      LOG.trace("Read {} bytes in reverse from offset {} for log at {}", bytesRead, offset, url.getPath());

      // Offset = largest virtualFilePointer read from, nextOffset = smallest virtualFilePointer read from
      return new MesosFileChunkObject(new String(bytes, Charsets.UTF_8), offset.get(), Optional.of(nextOffsetStart));
    } catch (FileTruncatedException fte) {
      throw WebExceptions.badRequest(String.format("File at %s is not block compressed", url.getPath()), fte);
    } finally {
      stream.close();
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
