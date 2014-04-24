package com.hubspot.singularity.logwatcher;

import java.util.Map;

public class TailMetadata {

  private String filename;
  private String tag;
  private Map<String, String> extraFields;
  
  public TailMetadata(String filename, String tag, Map<String, String> extraFields) {
    this.filename = filename;
    this.tag = tag;
    this.extraFields = extraFields;
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
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TailMetadata other = (TailMetadata) obj;
    if (filename == null) {
      if (other.filename != null)
        return false;
    } else if (!filename.equals(other.filename))
      return false;
    return true;
  }
  
  public String getFilename() {
    return filename;
  }
  
  public void setFilename(String filename) {
    this.filename = filename;
  }
  
  public String getTag() {
    return tag;
  }
  
  public void setTag(String tag) {
    this.tag = tag;
  }
  
  public Map<String, String> getExtraFields() {
    return extraFields;
  }
  
  public void setExtraFields(Map<String, String> extraFields) {
    this.extraFields = extraFields;
  }
  
  @Override
  public String toString() {
    return "TailMetadata [filename=" + filename + ", tag=" + tag + ", extraFields=" + extraFields + "]";
  }
  
}
