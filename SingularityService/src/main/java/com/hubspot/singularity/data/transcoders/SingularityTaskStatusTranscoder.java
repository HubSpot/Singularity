package com.hubspot.singularity.data.transcoders;

import org.apache.mesos.Protos.TaskStatus;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityTaskStatusTranscoder extends CompressingTranscoder<TaskStatus> {

  @Inject
  public SingularityTaskStatusTranscoder(SingularityConfiguration configuration) {
    super(configuration);
  }

  @Override
  protected TaskStatus actualTranscode(byte[] data) {
    try {
      return TaskStatus.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  protected byte[] actualToBytes(TaskStatus object) {
    return object.toByteArray();
  }

}
