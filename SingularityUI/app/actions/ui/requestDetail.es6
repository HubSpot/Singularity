import { FetchRequest } from '../api/requests';
import {
  FetchActiveTasksForRequest,
  FetchTaskHistoryForRequest,
  FetchDeploysForRequest,
  FetchRequestHistory,
} from '../api/history';
import { FetchTaskCleanups, FetchScheduledTasksForRequest } from '../api/tasks';
import { FetchRequestUtilization } from '../api/utilization';

export const refresh = (requestId, location) => (dispatch, getState) => {
  const requiredPromises = Promise.all([
    dispatch(FetchRequest.trigger(requestId)),
    dispatch(FetchRequestHistory.trigger(requestId, 5, 1))
  ])

  dispatch(FetchActiveTasksForRequest.trigger(requestId));
  dispatch(FetchTaskCleanups.trigger());

  const { taskHistoryPage, taskHistoryCount } = location.query;
  if (taskHistoryPage == null || taskHistoryPage == 1) {
  console.log(taskHistoryPage)
    dispatch(FetchTaskHistoryForRequest.trigger(requestId, taskHistoryCount == null ? 10 : taskHistoryCount, taskHistoryPage == null ? 1 : taskHistoryPage));
  }

  dispatch(FetchDeploysForRequest.trigger(requestId, 5, 1));
  dispatch(FetchScheduledTasksForRequest.trigger(requestId));
  dispatch(FetchRequestUtilization.trigger(requestId, [404]))

  return requiredPromises;
}