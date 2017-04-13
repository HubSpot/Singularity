import { FetchDeployForRequest, FetchActiveTasksForDeploy, FetchTaskHistory, FetchTaskHistoryForDeploy } from '../api/history'

const fetchTaskHistoryOfActiveTasksForDeploy = () => (dispatch, getState) => {
  const promises = [];

  for (const task of getState().api.activeTasksForDeploy.data) {
    promises.push(dispatch(FetchTaskHistory.trigger(task.taskId.id)));
  }

  return Promise.all(promises);
};

export const initialize = (requestId, deployId) => (dispatch, getStore) => {
  const allPromises = Promise.all([
    dispatch(FetchTaskHistoryForDeploy.clearData()),
    dispatch(FetchTaskHistoryForDeploy.trigger(requestId, deployId, 5, 1))
  ]);

  return allPromises.then(() => dispatch(refresh(requestId, deployId)));
};

export const refresh = (requestId, deployId) => (dispatch, getStore) => {
  const allPromises = Promise.all([
    dispatch(FetchDeployForRequest.trigger(requestId, deployId, true)),
    dispatch(FetchActiveTasksForDeploy.trigger(requestId, deployId)),
  ]);

  return allPromises.then(() => dispatch(fetchTaskHistoryOfActiveTasksForDeploy()));
};
