package com.hubspot.singularity.resources;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.data.history.HistoryManager.OrderDirection;
import com.hubspot.singularity.data.history.HistoryManager.RequestHistoryOrderBy;

@Path("/history")
@Produces({ MediaType.APPLICATION_JSON })
public class HistoryResource extends AbstractHistoryResource {
  
  private final HistoryManager historyManager;
  private final TaskManager taskManager;
  
  @Inject
  public HistoryResource(HistoryManager historyManager, TaskManager taskManager) {
    super(historyManager, taskManager);
    
    this.historyManager = historyManager;
    this.taskManager = taskManager;
  }
 
  @GET
  @Path("/task/{taskId}")
  public SingularityTaskHistory getHistoryForTask(@PathParam("taskId") String taskId) {
    SingularityTaskId taskIdObj = getTaskIdObject(taskId);
    
    return getTaskHistory(taskIdObj);
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
      throw WebExceptions.badRequest("%s was not found in choices:%s", name, Arrays.toString(choices));
    }
  }
  
  private Optional<RequestHistoryOrderBy> getRequestHistoryOrderBy(String orderBy) {
    if (orderBy == null) {
      return Optional.absent();
    }
    
    checkExists(orderBy, RequestHistoryOrderBy.values());
    
    return Optional.of(RequestHistoryOrderBy.valueOf(orderBy));
  }
  
  // TODO should this return id history or full history?
  
  @GET
  @Path("/request/{requestId}/tasks/active")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@PathParam("requestId") String requestId) {    
    List<SingularityTaskId> activeTaskIds = taskManager.getActiveTaskIdsForRequest(requestId);
    
    return getHistoriesFor(activeTaskIds);
  }
  
  private List<SingularityTaskIdHistory> getHistoriesFor(Collection<SingularityTaskId> taskIds) {
    Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> map = taskManager.getTaskHistoryUpdates(taskIds);
    
    List<SingularityTaskIdHistory> histories = Lists.newArrayListWithCapacity(taskIds.size());
    
    for (SingularityTaskId taskId : taskIds) {
      Collection<SingularityTaskHistoryUpdate> historyUpdates = map.get(taskId);
      
      histories.add(SingularityTaskIdHistory.fromTaskIdAndUpdates(taskId, historyUpdates));
    }
    
    return Ordering.natural().sortedCopy(histories);
  }
  
  
  @GET
  @Path("/request/{requestId}/tasks")
  public List<SingularityTaskIdHistory> getTaskHistoryForRequest(@PathParam("requestId") String requestId, @QueryParam("count") Integer count, @QueryParam("page") Integer page) {
    final Integer limitCount = getLimitCount(count);
    final Integer limitStart = getLimitStart(limitCount, page);
  
    final List<SingularityTaskId> inactiveTasksInZk = taskManager.getInActiveTaskIdsForRequest(requestId);
    final List<SingularityTaskIdHistory> idHistories = getHistoriesFor(inactiveTasksInZk);

    final int numFromZk = idHistories.size() - limitStart;
    
    final Integer numFromHistory = limitCount - numFromZk;
    final Integer historyStart = limitStart - idHistories.size();
    
    List<SingularityTaskIdHistory> returned = Lists.newArrayListWithCapacity(limitCount);
    
    if (numFromZk > 0) {
      returned.addAll(idHistories.subList(limitStart, limitStart + numFromZk));
    }
    
    if (numFromHistory > 0) {
      returned.addAll(historyManager.getTaskHistoryForRequest(requestId, historyStart, numFromHistory));
    }
      
    return returned;
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
