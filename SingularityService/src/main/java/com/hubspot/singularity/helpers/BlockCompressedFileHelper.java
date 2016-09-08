package com.hubspot.singularity.helpers;

import java.io.IOException;
import java.net.URL;

import htsjdk.samtools.util.BlockCompressedInputStream;

import com.google.common.base.Optional;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.singularity.WebExceptions;

public class BlockCompressedFileHelper {
  public static MesosFileChunkObject getAndDecompressFromUrl(URL url, Optional<Long> offset, int length) {
    try {
      BlockCompressedInputStream stream = new BlockCompressedInputStream(url);
      BlockCompressedInputStream.isValidFile(stream);
      if (offset.isPresent()) {
        stream.seek(offset.get());
      }
      byte[] bytes = new byte[length];
      int bytesRead = stream.read(bytes, 0, length);
      long newOffset = stream.getPosition();
      return new MesosFileChunkObject(new String(bytes), offset.or(0L), Optional.of(newOffset));
    } catch (IOException ioe) {
      throw WebExceptions.badRequest("Cannot seek to an offset in a file that is not block compressed", ioe);
    }
  }
}
