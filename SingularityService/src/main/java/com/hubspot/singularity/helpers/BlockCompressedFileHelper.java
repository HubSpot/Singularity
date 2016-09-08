package com.hubspot.singularity.helpers;

import java.net.URL;

import htsjdk.samtools.FileTruncatedException;
import htsjdk.samtools.util.BlockCompressedInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.WebExceptions;

public class BlockCompressedFileHelper {
  private static final Logger LOG = LoggerFactory.getLogger(BlockCompressedFileHelper.class);

  public static MesosFileChunkObject getAndDecompressFromUrl(URL url, Optional<Long> offset, int length) throws Exception {
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
}
