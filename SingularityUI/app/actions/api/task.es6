import fetch from 'isomorphic-fetch';

export const FETCH_TASK = 'FETCH_TASK';
export const FETCH_TASK_STARTED = 'FETCH_TASK_STARTED';
export const FETCH_TASK_ERROR = 'FETCH_TASK_ERROR';
export const FETCH_TASK_SUCCESS = 'FETCH_TASK_SUCCESS';

export function fetchTask(taskId) {
  return function (dispatch) {
    dispatch(fetchTaskStarted());

    return fetch(`${ config.apiRoot }/history/task/${taskId}`, {
      credentials: 'include'
    })
      .then(response => response.json())
      .then(json => {
        dispatch(fetchTaskSuccess(json, taskId));
      })
      .catch(ex => {
        dispatch(fetchTasksError(ex));
      });
  };
}

export function fetchTaskStarted(taskId) {
  return { type: FETCH_TASK_STARTED, taskId: taskId };
}

export function fetchTasksError(error, taskId) {
  return { type: FETCH_TASK_ERROR, error: error, taskId: taskId };
}

export function fetchTaskSuccess(data, taskId) {
  return { type: FETCH_TASK_SUCCESS, data: data, taskId: taskId };
}
