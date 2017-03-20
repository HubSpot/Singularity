import { FetchRequest } from '../api/requests';
import {
  FetchActiveTasksForRequest,
  FetchTaskHistoryForRequest,
  FetchDeploysForRequest,
  FetchRequestHistory,
} from '../api/history';
import { FetchTaskCleanups, FetchScheduledTasksForRequest } from '../api/tasks';

export const refresh = (requestId) => (dispatch, getState) => {
  const requiredPromises = Promise.all([
    dispatch(FetchRequest.trigger(requestId)),
    dispatch(FetchRequestHistory.trigger(requestId, 5, 1))
  ])

  dispatch(FetchActiveTasksForRequest.trigger(requestId, 5, 1));
  dispatch(FetchTaskCleanups.trigger());
  dispatch(FetchTaskHistoryForRequest.trigger(requestId, 5, 1));
  dispatch(FetchDeploysForRequest.trigger(requestId, 5, 1));
  dispatch(FetchScheduledTasksForRequest.trigger(requestId));

  return requiredPromises;
}