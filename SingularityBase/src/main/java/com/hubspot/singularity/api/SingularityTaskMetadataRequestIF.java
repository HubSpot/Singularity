package com.hubspot.singularity.api;

import org.immutables.value.Value.Immutable;

import com.google.common.base.Optional;
import com.hubspot.immutables.style.SingularityStyle;
import com.hubspot.singularity.MetadataLevel;
import com.wordnik.swagger.annotations.ApiModelProperty;

@Immutable
@SingularityStyle
public interface SingularityTaskMetadataRequestIF {

  @ApiModelProperty(required=true, value="A type to be associated with this metadata")
  String getType();

  @ApiModelProperty(required=true, value="A title to be associated with this metadata")
  String getTitle();

  @ApiModelProperty(required=false, value="An optional message")
  Optional<String> getMessage();

  @ApiModelProperty(required=false, value="Level of metadata, can be INFO, WARN, or ERROR")
  Optional<MetadataLevel> getLevel();
}
