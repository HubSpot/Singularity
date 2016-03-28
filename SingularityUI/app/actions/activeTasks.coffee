fetchTasksForRequest = (requestId, state='active') ->
  params = {
    property: 'taskId'
  }
  $.ajax
    url: "#{ config.apiRoot }/history/request/#{ requestId }/tasks/#{ state }?#{ $.param(params) }"

updateActiveTasks = (requestId) ->
  (dispatch) ->
    fetchTasksForRequest(requestId).done (tasks) ->
      dispatch({tasks, type: 'REQUEST_ACTIVE_TASKS'})

module.exports = { updateActiveTasks, fetchTasksForRequest }
