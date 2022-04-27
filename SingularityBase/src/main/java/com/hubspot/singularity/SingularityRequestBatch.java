package com.hubspot.singularity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;

@Schema(description = "Batch response for multiple requests")
public class SingularityRequestBatch {

  private final List<SingularityRequestParent> requests;
  private final Set<String> notFound;

  @JsonCreator
  public SingularityRequestBatch(
    @JsonProperty("requests") List<SingularityRequestParent> requests,
    @JsonProperty("notFound") Set<String> notFound
  ) {
    this.requests = requests;
    this.notFound = notFound;
  }

  @Schema(nullable = false, description = "List of found reqeusts from the batch")
  public List<SingularityRequestParent> getRequests() {
    return requests;
  }

  @Schema(
    nullable = false,
    description = "List of request ids from the requested batch that were not found"
  )
  public Set<String> getNotFound() {
    return notFound;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SingularityRequestBatch that = (SingularityRequestBatch) o;

    if (requests != null ? !requests.equals(that.requests) : that.requests != null) {
      return false;
    }
    return notFound != null ? notFound.equals(that.notFound) : that.notFound == null;
  }

  @Override
  public int hashCode() {
    int result = requests != null ? requests.hashCode() : 0;
    result = 31 * result + (notFound != null ? notFound.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return (
      "SingularityRequestBatch{" + "requests=" + requests + ", notFound=" + notFound + '}'
    );
  }
}
