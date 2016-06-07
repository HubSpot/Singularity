package com.hubspot.singularity.data;

import java.net.ConnectException;
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
