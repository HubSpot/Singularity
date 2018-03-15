package com.hubspot.mesos.json;

import java.util.Optional;

import org.immutables.value.Value.Immutable;

import com.hubspot.singularity.annotations.SingularityStyle;

import io.swagger.v3.oas.annotations.media.Schema;

@Immutable
@SingularityStyle
@Schema(description = "A portion of a file from a task sandbox")
public interface MesosFileChunkObjectIF {
  @Schema(description = "Content of this portion of the file")
  String getData();

  @Schema(description = "Offset in bytes of this content")
  long getOffset();

  @Schema(description = "The next offset to fetch to continue from the end of the content in this object", nullable = true)
  Optional<Long> getNextOffset();
}
