package com.hubspot.singularity.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.json.MesosFileChunkObject;
import com.hubspot.mesos.json.MesosFileObject;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.util.Collection;
import java.util.Collections;

public class SandboxManager {
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;

  private static final TypeReference<Collection<MesosFileObject>> MESOS_FILE_OBJECTS = new TypeReference<Collection<MesosFileObject>>() {};

  @Inject
  public SandboxManager(AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.objectMapper = objectMapper;
  }

  public Collection<MesosFileObject> browse(String slaveHostname, String fullPath) {
    try {
      Response response = asyncHttpClient.prepareGet(String.format("http://%s:5051/files/browse.json", slaveHostname))
          .addQueryParameter("path", fullPath)
          .execute().get();

      if (response.getStatusCode() == 404) {
        return Collections.emptyList();
      }

      if (response.getStatusCode() != 200) {
        throw new RuntimeException(String.format("Got HTTP %s from Mesos slave", response.getStatusCode()));
      }

      return objectMapper.readValue(response.getResponseBodyAsStream(), MESOS_FILE_OBJECTS);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<MesosFileChunkObject> read(String slaveHostname, String fullPath, Optional<Long> offset, Optional<Long> length) {
    try {
      final AsyncHttpClient.BoundRequestBuilder builder = asyncHttpClient.prepareGet(String.format("http://%s:5051/files/read.json", slaveHostname))
          .addQueryParameter("path", fullPath);

      if (offset.isPresent()) {
        builder.addQueryParameter("offset", offset.get().toString());
      }

      if (length.isPresent()) {
        builder.addQueryParameter("length", length.get().toString());
      }

      final Response response = builder.execute().get();

      if (response.getStatusCode() == 400) {
        return Optional.absent();
      }

      if (response.getStatusCode() != 200) {
        throw new RuntimeException(String.format("Got HTTP %s from Mesos slave", response.getStatusCode()));
      }

      return Optional.of(objectMapper.readValue(response.getResponseBodyAsStream(), MesosFileChunkObject.class));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
