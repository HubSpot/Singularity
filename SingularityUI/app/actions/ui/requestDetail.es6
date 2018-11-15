import { FetchRequest } from '../api/requests';
import {
  FetchActiveTasksForRequest,
  FetchTaskHistoryForRequest,
  FetchDeploysForRequest,
  FetchRequestHistory,
} from '../api/history';
import { FetchTaskCleanups, FetchScheduledTasksForRequest } from '../api/tasks';
import { FetchRequestUtilization } from '../api/utilization';

export const refresh = (requestId, taskHistoryPage = 1, taskHistoryPageSize = 10) => (dispatch, getState) => {
  const requiredPromises = Promise.all([
    dispatch(FetchRequest.trigger(requestId)),
    dispatch(FetchRequestHistory.trigger(requestId, 5, 1))
  ])

  dispatch(FetchActiveTasksForRequest.trigger(requestId));
  dispatch(FetchTaskCleanups.trigger());

  if (taskHistoryPage == 1) {
    dispatch(FetchTaskHistoryForRequest.trigger(requestId, taskHistoryPageSize, taskHistoryPage));
  }

  dispatch(FetchDeploysForRequest.trigger(requestId, 5, 1));
  dispatch(FetchScheduledTasksForRequest.trigger(requestId));
  dispatch(FetchRequestUtilization.trigger(requestId, [404]))

  return requiredPromises;
}