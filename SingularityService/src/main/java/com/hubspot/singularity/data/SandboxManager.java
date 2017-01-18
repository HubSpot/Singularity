package com.hubspot.singularity.data;

import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.json.MesosBinaryChunkObject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.hubspot.mesos.json.UTF8String;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.PerRequestConfig;
import com.ning.http.client.Response;

@Singleton
public class SandboxManager {

  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  private final SingularityConfiguration configuration;

  private static final TypeReference<Collection<MesosFileObject>> MESOS_FILE_OBJECTS = new TypeReference<Collection<MesosFileObject>>() {};

  @Inject
  public SandboxManager(AsyncHttpClient asyncHttpClient, SingularityConfiguration configuration, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;
    this.configuration = configuration;
  }

  @SuppressWarnings("serial")
  public static class SlaveNotFoundException extends RuntimeException {
    public SlaveNotFoundException(Exception e) {
      super(e);
    }
  }

  public Collection<MesosFileObject> browse(String slaveHostname, String fullPath) throws SlaveNotFoundException {
    try {
      Response response = asyncHttpClient.prepareGet(String.format("http://%s:5051/files/browse", slaveHostname))
          .addQueryParameter("path", fullPath)
          .execute().get();

      if (response.getStatusCode() == 404) {
        return Collections.emptyList();
      }

      if (response.getStatusCode() != 200) {
        throw new RuntimeException(String.format("Got HTTP %s from Mesos slave", response.getStatusCode()));
      }

      return objectMapper.readValue(response.getResponseBodyAsStream(), MESOS_FILE_OBJECTS);
    } catch (ConnectException ce) {
      throw new SlaveNotFoundException(ce);
    } catch (Exception e) {
      if (e.getCause().getClass() == ConnectException.class) {
        throw new SlaveNotFoundException(e);
      } else {
        throw Throwables.propagate(e);
      }
    }
  }


  // Bit-pattern check for UTF-8 continuation byte:
  // 0b10xxxxxx -> true
  private static boolean isContinuationByte(byte b) {
    return b >= (byte)0b10_000000 && b <= (byte)0b10_111111;
  }

  // Bit-pattern check for UTF-8 character extra-byte length:
  // 0b110_xxxxx -> 1
  // 0b1110_xxxx -> 2
  // 0b11110_xxx -> 3
  // else        -> 0
  private static int numberOfFollowingBytes(byte b) {
    if (b >= (byte)0b110_00000 && b <= (byte)0b110_11111) {
      return 1;
    } else if (b >= (byte)0b1110_0000 && b <= (byte)0b1110_1111) {
      return 2;
    } else if (b >= (byte)0b11110_000 && b <= (byte)0b11110_111) {
      return 3;
    } else {
      // this could be an ASCII char or a continuation byte
      return 0;
    }
  }

  static Optional<MesosBinaryChunkObject> stripInvalidUTF8(Optional<MesosBinaryChunkObject> inChunk) {
    if (!inChunk.isPresent()) {
      return inChunk;
    }

    UTF8String utf8String = inChunk.get().getData();

    // I'm assuming that the position and limit are the same size as this is...
    // this is a fair assumption iff inChunk is generated directly before this
    // once run through this function, the file chunk object can no longer
    // be assumed to be directly mapped to the array within the UTF8String
    byte[] data = utf8String.getData();

    int firstIndex = 0;
    int limit = utf8String.getLength();


    // Check to see if there's invalid UTF-8 at the beginning of the chunk
    // a continuation byte (0b10xxxxxx) can never be at the start of a
    // character find every continuation byte in a row at the beginning of the
    // sequence and drop them.
    for (int i = 0; i < 3 && i < utf8String.getLength(); i++) {
      // remove every byte from the sequence that starts like 0b10...
      if (isContinuationByte(utf8String.get(i))) {
        firstIndex += 1;
      } else {
        break;
      }
    }


    // Check to see if there's invalid UTF-8 at the end of the chunk
    for (int i = utf8String.getLength() - 3; i < utf8String.getLength(); i++) {
      if (i < 0) {
        // We don't want to prematurely end loop if string byte length < 3
        // and i < 0 in this loop (but later i >= 0)
        continue;
      }
      // Find number of continuation chars, if it extends the length of the
      // array, move end offset back to before this byte and stop

      int following = numberOfFollowingBytes(utf8String.get(i));
      if (i + following >= utf8String.getLength()) {
        limit = i;
        break;
      }
    }

    // byte offset in chunked file
    long fileOffset = inChunk.get().getOffset();

    // byte length of the data in the truncated buffer
    // I hope we never need > 2GB buffers
    int length = limit - firstIndex;

    long newOffset = fileOffset + firstIndex;

    long nextOffset = newOffset + length;

    UTF8String modifiedBoundsUTF8String = new UTF8String(
        data,
        utf8String.getOffset() + firstIndex,
        length
    );

    return Optional.of(new MesosBinaryChunkObject(
        modifiedBoundsUTF8String,
        newOffset,
        Optional.of(nextOffset)
    ));
  }

  @SuppressWarnings("deprecation")
  public Optional<MesosBinaryChunkObject> read(String slaveHostname, String fullPath, Optional<Long> offset, Optional<Long> length, Optional<Boolean> dropInvalidUTF8) throws SlaveNotFoundException {
    try {
      final AsyncHttpClient.BoundRequestBuilder builder = asyncHttpClient.prepareGet(String.format("http://%s:5051/files/read", slaveHostname))
          .addQueryParameter("path", fullPath);

      PerRequestConfig timeoutConfig = new PerRequestConfig();
      timeoutConfig.setRequestTimeoutInMs((int) configuration.getSandboxHttpTimeoutMillis());
      builder.setPerRequestConfig(timeoutConfig);

      if (offset.isPresent()) {
        builder.addQueryParameter("offset", offset.get().toString());
      }

      if (length.isPresent()) {
        builder.addQueryParameter("length", length.get().toString());
      }

      final Response response = builder.execute().get();

      if (response.getStatusCode() == 404) {
        return Optional.absent();
      }

      if (response.getStatusCode() != 200) {
        throw new RuntimeException(String.format("Got HTTP %s from Mesos slave", response.getStatusCode()));
      }

      Optional<MesosBinaryChunkObject> maybeUncheckedChunk = Optional.of(objectMapper.readValue(response.getResponseBodyAsStream(), MesosBinaryChunkObject.class));

      if (dropInvalidUTF8.or(true)) {
        return stripInvalidUTF8(maybeUncheckedChunk);
      }

      return maybeUncheckedChunk;
    } catch (ConnectException ce) {
      throw new SlaveNotFoundException(ce);
    } catch (Exception e) {
      if ((e.getCause() != null) && (e.getCause().getClass() == ConnectException.class)) {
        throw new SlaveNotFoundException(e);
      } else {
        throw Throwables.propagate(e);
      }
    }
  }
}
