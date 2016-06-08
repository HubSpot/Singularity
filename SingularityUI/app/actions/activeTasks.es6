let fetchTasksForRequest = function(requestId, state = 'active') {
  let params = {
    property: 'taskId'
  };
  return $.ajax(
    {url: `${ config.apiRoot }/history/request/${ requestId }/tasks/${ state }?${ $.param(params) }`});
};

let updateActiveTasks = requestId =>
  dispatch =>
    fetchTasksForRequest(requestId).done(tasks => dispatch({tasks, type: 'REQUEST_ACTIVE_TASKS'}));

export { updateActiveTasks, fetchTasksForRequest };
