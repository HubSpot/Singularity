export function fetchTasksForRequest(requestId, state = 'active') {
  return $.ajax({url: `${ config.apiRoot }/history/request/${ requestId }/tasks/${ state }?${ $.param({property: 'taskId'}) }`});
}

export function updateActiveTasks(requestId) {
  return dispatch => fetchTasksForRequest(requestId).done(tasks => dispatch({tasks, type: 'REQUEST_ACTIVE_TASKS'}));
}
