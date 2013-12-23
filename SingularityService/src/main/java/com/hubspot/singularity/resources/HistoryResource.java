package com.hubspot.singularity.resources;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
import com.hubspot.singularity.data.history.HistoryManager.RequestHistoryOrderBy;
import com.hubspot.singularity.data.history.HistoryManager.TaskHistoryOrderBy;
import com.sun.jersey.api.NotFoundException;

@Path("/history")
@Produces({ MediaType.APPLICATION_JSON })
public class HistoryResource {
  
  private final HistoryManager historyManager;
  
  @Inject
  public HistoryResource(HistoryManager historyManager) {
    this.historyManager = historyManager;
  }
  
  @GET
  @Path("/task/{taskId}")
  public SingularityTaskHistory getHistoryForTask(@PathParam("taskId") String taskId) {
    Optional<SingularityTaskHistory> history = historyManager.getTaskHistory(taskId, true);
  
    if (!history.isPresent()) {
      throw new NotFoundException(String.format("No history for task %s", taskId));
    }
    
    return history.get();
  }
  
  private Integer getLimitCount(Integer countParam) {
    if (countParam == null) {
      return 100;
    }
    
    if (countParam < 1) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }
    
    if (countParam > 1000) {
      return 1000;
    }
    
    return countParam;
  }
  
  private Integer getLimitStart(Integer limitCount, Integer pageParam) {
    if (pageParam == null) {
      return 0;
    }
    
    if (pageParam < 1) {
      throw new WebApplicationException(Status.BAD_REQUEST);
    }
    
    return limitCount * (pageParam - 1);
  }
  
  private Optional<OrderDirection> getOrderDirection(String orderDirection) {
    if (orderDirection == null) {
      return Optional.absent();
    }
    
    checkExists(orderDirection, OrderDirection.values());
    
    return Optional.of(OrderDirection.valueOf(orderDirection));
  }
  
  private void checkExists(String name, Enum<?>[] choices) {
    boolean found = false;
    for (Enum<?> choice : choices) {
      if (name.equals(choice.name())) {
        found = true;
        break;
      }
    }
    
    if (!found) {
      throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity(String.format("%s was not found in choices:%s", name, Arrays.toString(choices))).type("text/plain").build());
    }
  }
  
  private Optional<TaskHistoryOrderBy> getTaskHistoryOrderBy(String orderBy) {
    if (orderBy == null) {
      return Optional.absent();
    }
    
    checkExists(orderBy, TaskHistoryOrderBy.values());
    
    return Optional.of(TaskHistoryOrderBy.valueOf(orderBy));
  }
  
  private Optional<RequestHistoryOrderBy> getRequestHistoryOrderBy(String orderBy) {
    if (orderBy == null) {
      return Optional.absent();
    }
    
    checkExists(orderBy, RequestHistoryOrderBy.values());
    
    return Optional.of(RequestHistoryOrderBy.valueOf(orderBy));
  }
  
  @GET
  @Path("/request/{requestId}/tasks/active")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@PathParam("requestId") String requestId) {    
    return historyManager.getActiveTaskHistoryForRequest(requestId);
  }
  
  @GET
  @Path("/request/{requestId}/tasks")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@PathParam("requestId") String requestId, @QueryParam("orderBy") String orderBy, @QueryParam("orderDirection") String orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
    Optional<TaskHistoryOrderBy> taskOrderBy = getTaskHistoryOrderBy(orderBy);
    Optional<OrderDirection> maybeOrderDirection = getOrderDirection(orderDirection);
    
    return historyManager.getTaskHistoryForRequest(requestId, taskOrderBy, maybeOrderDirection, limitStart, limitCount);
  }
  
  @GET
  @Path("/tasks/search")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike, @QueryParam("orderBy") String orderBy, @QueryParam("orderDirection") String orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
    Optional<TaskHistoryOrderBy> taskOrderBy = getTaskHistoryOrderBy(orderBy);
    Optional<OrderDirection> maybeOrderDirection = getOrderDirection(orderDirection);
    
    return historyManager.getTaskHistoryForRequestLike(requestIdLike, taskOrderBy, maybeOrderDirection, limitStart, limitCount);
  }
  
  @GET
  @Path("/request/{requestId}/requests")
  public List<SingularityRequestHistory> getRequestHistoryForRequest(@PathParam("requestId") String requestId, @QueryParam("orderBy") String orderBy, @QueryParam("orderDirection") String orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
    Optional<RequestHistoryOrderBy> requestOrderBy = getRequestHistoryOrderBy(orderBy);
    Optional<OrderDirection> maybeOrderDirection = getOrderDirection(orderDirection);
    
    return historyManager.getRequestHistory(requestId, requestOrderBy, maybeOrderDirection, limitStart, limitCount);
  }
  
  @GET
  @Path("/requests/search")
  public List<SingularityRequestHistory> getRequestHistoryForRequestLike(@QueryParam("requestIdLike") String requestIdLike, @QueryParam("orderBy") String orderBy, @QueryParam("orderDirection") String orderDirection, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    Integer limitCount = getLimitCount(count);
    Integer limitStart = getLimitStart(limitCount, page);
    Optional<RequestHistoryOrderBy> requestOrderBy = getRequestHistoryOrderBy(orderBy);
    Optional<OrderDirection> maybeOrderDirection = getOrderDirection(orderDirection);
    
    return historyManager.getRequestHistoryLike(requestIdLike, requestOrderBy, maybeOrderDirection, limitStart, limitCount);
  }
  
}
