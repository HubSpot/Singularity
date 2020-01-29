import { FetchRequest, FetchRequestShuffleOptOut } from '../api/requests';
import {
  FetchActiveTasksForRequest,
  FetchDeploysForRequest,
  FetchRequestHistory,
} from '../api/history';
import { FetchTaskCleanups, FetchScheduledTasksForRequest } from '../api/tasks';
import { FetchRequestUtilization } from '../api/utilization';

export const refresh = (requestId) => (dispatch, getState) => {
  const requiredPromises = Promise.all([
    dispatch(FetchRequest.trigger(requestId)),
    dispatch(FetchRequestHistory.trigger(requestId, 5, 1)),
  ])

  dispatch(FetchActiveTasksForRequest.trigger(requestId));
  dispatch(FetchTaskCleanups.trigger());
  dispatch(FetchDeploysForRequest.trigger(requestId, 5, 1));
  dispatch(FetchScheduledTasksForRequest.trigger(requestId));
  dispatch(FetchRequestUtilization.trigger(requestId, [404]))

  return requiredPromises;
}

export const initialize = (requestId) => (dispatch, getState) => {
  const requiredPromises = Promise.all([
    dispatch(FetchRequest.trigger(requestId)),
    dispatch(FetchRequestHistory.trigger(requestId, 5, 1)),
    dispatch(FetchRequestShuffleOptOut.trigger(requestId)),
  ])

  dispatch(FetchActiveTasksForRequest.trigger(requestId));
  dispatch(FetchTaskCleanups.trigger());
  dispatch(FetchDeploysForRequest.trigger(requestId, 5, 1));
  dispatch(FetchScheduledTasksForRequest.trigger(requestId));
  dispatch(FetchRequestUtilization.trigger(requestId, [404]))

  return requiredPromises;
}
