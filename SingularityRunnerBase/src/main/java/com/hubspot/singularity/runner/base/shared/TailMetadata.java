package com.hubspot.singularity.runner.base.shared;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.hash.Hashing;

public class TailMetadata {

  private final String filename;
  private final String tag;
  private final Map<String, String> extraFields;
  private final boolean finished;
  private final String filenameKey;

  public static Path getTailMetadataPath(Path logMetadataDirectory, String logMetadataSuffix, TailMetadata tail) {
    return logMetadataDirectory.resolve(Paths.get(tail.getFilenameKey() + logMetadataSuffix));
  }

  @JsonCreator
  public TailMetadata(@JsonProperty("filename") String filename, @JsonProperty("tag") String tag, @JsonProperty("extraFields") Map<String, String> extraFields, @JsonProperty("finished") boolean finished) {
    this.filename = filename;
    this.tag = tag;
    this.extraFields = extraFields;
    this.finished = finished;
    this.filenameKey = generateFilenameKey(filename);
  }

  private String generateFilenameKey(String filename) {
    return Hashing.sha256().hashString(filename, UTF_8).toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((filename == null) ? 0 : filename.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null) {
        return false;
    }
    if (getClass() != obj.getClass()) {
        return false;
    }
    TailMetadata other = (TailMetadata) obj;
    if (filename == null) {
      if (other.filename != null) {
        return false;
    }
    } else if (!filename.equals(other.filename)) {
        return false;
    }
    return true;
  }

  @JsonIgnore
  public String getFilenameKey() {
    return filenameKey;
  }

  public String getFilename() {
    return filename;
  }

  public String getTag() {
    return tag;
  }

  public Map<String, String> getExtraFields() {
    return extraFields;
  }

  public boolean isFinished() {
    return finished;
  }

  @Override
  public String toString() {
    return "TailMetadata [filename=" + filename + ", tag=" + tag + ", extraFields=" + extraFields + ", finished=" + finished + ", filenameKey=" + filenameKey + "]";
  }

}
