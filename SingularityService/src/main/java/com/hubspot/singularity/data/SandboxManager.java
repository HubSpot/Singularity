package com.hubspot.singularity.data;

import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
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


  private boolean isContinuationChar(byte b) {
    return b >= (byte)0b1000_0000 && b <= (byte)0b1011_1111;
  }

  private int numberOfFollowingBytes(byte b) {
    if (b >= (byte)0b1100_0000 && b <= (byte)0b1101_1111) {
      return 1;
    } else if (b >= (byte)0b1110_0000 && b <= (byte)0b1110_1111) {
      return 2;
    } else if (b >= (byte)0b1111_0000 && b <= (byte)0b1111_0111) {
      return 3;
    } else {
      // this could be an ASCII char or a continuation byte
      return 0;
    }
  }

  Optional<MesosFileChunkObject> stripInvalidUTF8(Optional<MesosFileChunkObject> inChunk) {
    if (!inChunk.isPresent()) {
      return inChunk;
    }

    ByteBuffer byteBuffer = inChunk.get().getData();

    // I'm assuming that the position and limit are the same size as this is...
    // this is a fair assumption iff inChunk is generated directly before this
    // once run through this function, the file chunk object can no longer
    // be assumed to be directly mapped to the array within the ByteBuffer
    // If this cannot be assumed, refer to: http://stackoverflow.com/a/33899475
    // """
    // final byte[] b = new byte[myByteBuffer.remaining()];
    // myByteBuffer.duplicate().get(b);
    // """
    byte[] data = byteBuffer.array();

    int firstIndex = 0;
    int limit = data.length;


    // Check to see if there's invalid UTF-8 at the beginning of the chunk
    // a continuation byte (0b10xxxxxx) can never be at the start of a
    // character find every continuation byte in a row at the beginning of the
    // sequence and drop them.
    for (int i = 0; i < 3 && i < data.length; i++) {
      // remove every byte from the sequence that starts like 0b10...
      if (isContinuationChar(data[i])) {
        firstIndex += 1;
      } else {
        break;
      }
    }

    // Check to see if there's invalid UTF-8 at the end of the chunk
    for (int i = data.length - 3; i < data.length; i++) {
      if (i < 0) {
        // We don't want to prematurely end loop if data.length < 3
        // and i < 0 in this loop (but later i >= 0)
        continue;
      }
      // Find number of continuation chars, if it extends the length of the
      // array, move end offset back to before this byte and stop

      int following = numberOfFollowingBytes(data[i]);
      if (i + following >= data.length) {
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

    return Optional.of(new MesosFileChunkObject(
        ByteBuffer.wrap(data, firstIndex, length),
        newOffset,
        Optional.of(nextOffset)
    ));
  }

  @SuppressWarnings("deprecation")
  public Optional<MesosFileChunkObject> read(String slaveHostname, String fullPath, Optional<Long> offset, Optional<Long> length) throws SlaveNotFoundException {
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

      // TODO: make sure objectMapper actually converts the string into the ByteBuffer correctly
      return Optional.of(objectMapper.readValue(response.getResponseBodyAsStream(), MesosFileChunkObject.class));
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
