package com.hubspot.singularity.mesos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.squareup.tape2.ObjectQueue;
import com.squareup.tape2.ObjectQueue.Converter;
import com.squareup.tape2.QueueFile;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import org.apache.mesos.v1.Protos.TaskStatus;

@Singleton
public class StatusUpdateQueue {
  private final SingularityConfiguration configuration;
  private final File queueFile;
  private final Queue<TaskStatus> inMemoryQueue;
  private final ObjectQueue<TaskStatus> onDiskQueue;

  @Inject
  public StatusUpdateQueue(
    SingularityConfiguration configuration,
    @Singularity ObjectMapper objectMapper
  )
    throws IOException {
    this.configuration = configuration;
    this.inMemoryQueue =
      new ArrayBlockingQueue<>(
        configuration.getMesosConfiguration().getMaxStatusUpdateQueueSize()
      );
    File parent = Files.createTempDirectory("queues").toFile();
    this.queueFile = new File(parent, "queue-file");
    this.onDiskQueue =
      ObjectQueue.create(
        new QueueFile.Builder(queueFile).build(),
        new Converter<TaskStatus>() {

          @Override
          public TaskStatus from(byte[] source) throws IOException {
            return objectMapper.readValue(source, TaskStatus.class);
          }

          @Override
          public void toStream(TaskStatus value, OutputStream sink) throws IOException {
            objectMapper.writeValue(sink, value);
          }
        }
      );
  }

  public int onDiskSize() {
    return onDiskQueue.size();
  }

  public int inMemorySize() {
    return inMemoryQueue.size();
  }

  public int size() {
    return onDiskQueue.size() + inMemoryQueue.size();
  }

  public synchronized void add(TaskStatus update) throws IOException {
    if (
      inMemoryQueue.size() <
      configuration.getMesosConfiguration().getMaxStatusUpdateQueueSize()
    ) {
      inMemoryQueue.add(update);
    } else {
      onDiskQueue.add(update);
    }
  }

  public Iterator<TaskStatus> diskQueueIterator() {
    return onDiskQueue.iterator();
  }

  public TaskStatus nextInMemory() {
    return inMemoryQueue.poll();
  }
}
