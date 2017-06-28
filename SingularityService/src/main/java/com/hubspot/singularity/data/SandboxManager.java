package com.hubspot.singularity.data;

import java.io.IOException;
import java.io.Reader;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.io.CharSource;
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
  private static final String REPLACEMENT_CHARACTER = "\ufffd";
  private static final String TWO_REPLACEMENT_CHARACTERS = REPLACEMENT_CHARACTER + REPLACEMENT_CHARACTER;

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
      PerRequestConfig timeoutConfig = new PerRequestConfig();
      timeoutConfig.setRequestTimeoutInMs((int) configuration.getSandboxHttpTimeoutMillis());

      Response response = asyncHttpClient
          .prepareGet(String.format("http://%s:5051/files/browse", slaveHostname))
          .setPerRequestConfig(timeoutConfig)
          .addQueryParameter("path", fullPath)
          .execute()
          .get();



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

      return Optional.of(parseResponseBody(response));
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

  /**
   * This method will first sanitize the input by replacing invalid UTF8 characters with \ufffd (Unicode's "REPLACEMENT CHARACTER")
   * before sending it to Jackson for parsing. We then strip the replacement characters characters from the beginning and end of the string
   * and increment the offset field by how many characters were stripped from the beginning.
   */
  @VisibleForTesting
  MesosFileChunkObject parseResponseBody(Response response) throws IOException {
    // not thread-safe, need to make a new one each time;
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .replaceWith(REPLACEMENT_CHARACTER);

    ByteBuffer responseBuffer = response.getResponseBodyAsByteBuffer();
    Reader sanitizedReader = CharSource.wrap(decoder.decode(responseBuffer)).openStream();
    final MesosFileChunkObject initialChunk = objectMapper.readValue(sanitizedReader, MesosFileChunkObject.class);

    // bail early if no replacement characters
    if (!initialChunk.getData().startsWith(REPLACEMENT_CHARACTER) && !initialChunk.getData().endsWith(REPLACEMENT_CHARACTER)) {
      return initialChunk;
    }

    final String data = initialChunk.getData();

    // if we requested data between two characters, return nothing and advance the offset to the end
    if (data.length() <= 4 && data.replace(REPLACEMENT_CHARACTER, "").length() == 0) {
      return new MesosFileChunkObject("", initialChunk.getOffset() + data.length(), Optional.<Long>absent());
    }

    // trim incomplete character at the beginning of the string
    int startIndex = 0;
    if (data.startsWith(TWO_REPLACEMENT_CHARACTERS)) {
      startIndex = 2;
    } else if (data.startsWith(REPLACEMENT_CHARACTER)) {
      startIndex = 1;
    }

    // trim incomplete character at the end of the string
    int endIndex = data.length();
    if (data.endsWith(TWO_REPLACEMENT_CHARACTERS)) {
      endIndex -= 2;
    } else if (data.endsWith(REPLACEMENT_CHARACTER)) {
      endIndex -= 1;
    }

    return new MesosFileChunkObject(data.substring(startIndex, endIndex), initialChunk.getOffset() + startIndex, Optional.<Long>absent());
  }
}
