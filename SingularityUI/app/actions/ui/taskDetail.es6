import { FetchTaskHistory, FetchDeployForRequest } from '../api/history';
import { FetchTaskStatistics, FetchTaskCleanups } from '../api/tasks';
import { FetchPendingDeploys } from '../api/deploys';
import { FetchTaskS3Logs } from '../api/logs';
import { FetchTaskFiles } from '../../actions/api/sandbox';

export const refresh = (taskId, splat) => (dispatch, getState) => {
  const promises = [];

  const path = _.isUndefined(splat) ? undefined : splat.substring(1);

  promises.push(dispatch(FetchTaskFiles.trigger(taskId, path, [400, 404])));

  const taskPromise = dispatch(FetchTaskHistory.trigger(taskId, true));

  taskPromise.then((apiData) => {
    if (apiData.statusCode === 404) return;
    const task = apiData.data;

    promises.push(dispatch(FetchDeployForRequest.trigger(task.task.taskId.requestId, task.task.taskId.deployId)))

    if (task.isStillRunning) {
      promises.push(dispatch(FetchTaskStatistics.trigger(taskId, [404])));
    }
  });

  promises.push(taskPromise);
  promises.push(dispatch(FetchTaskCleanups.trigger()));
  promises.push(dispatch(FetchPendingDeploys.trigger()));

  return Promise.all(promises);
};

export const onLoad = (taskId) => (dispatch) => {
  return dispatch(FetchTaskS3Logs.trigger(taskId, [404, 500]));
};
