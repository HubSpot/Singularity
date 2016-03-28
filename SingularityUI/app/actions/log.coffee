Q = require 'q'
{ fetchTasksForRequest } = require './activeTasks'

fetchData = (taskId, path, offset=undefined, length=0) ->
  $.ajax
    url: "#{ config.apiRoot }/sandbox/#{ taskId }/read?#{$.param({path, length, offset})}"

initializeUsingActiveTasks = (requestId, path, search) ->
  (dispatch) ->
    deferred = Q.defer()
    fetchTasksForRequest(requestId).done (tasks) ->
      taskIds = _.sortBy(_.pluck(tasks, 'taskId'), (taskId) -> taskId.instanceNo).map((taskId) -> taskId.id)
      dispatch(initialize(requestId, path, search, taskIds)).then ->
        deferred.resolve()
    deferred.promise

initialize = (requestId, path, search, taskIds) ->
  (dispatch, getState) ->
    { viewMode } = getState()

    if viewMode is 'unified'
      taskIdGroups = [taskIds]
    else
      taskIdGroups = taskIds.map (taskId) -> [taskId]

    dispatch(init(requestId, taskIdGroups, path, search))

    groupPromises = taskIdGroups.map (taskIds, taskGroupId) ->
      taskPromises = taskIds.map (taskId) ->
        resolvedPath = path.replace('$TASK_ID', taskId)
        fetchData(taskId, resolvedPath).done ({offset}) ->
          dispatch(initTask(taskGroupId, taskId, offset, resolvedPath))

      Promise.all(taskPromises).then ->
        fetchPromises = dispatch(taskGroupFetchPrevious(taskGroupId))
        Promise.all(fetchPromises).then ->
          dispatch(taskGroupReady(taskGroupId))

    Promise.all(groupPromises)

init = (requestId, taskIdGroups, path, search) ->
  {
    requestId
    taskIdGroups
    path
    search
    type: 'LOG_INIT'
  }

addTaskGroup = (path, search, taskIds) ->
  {
    path
    taskIds
    search
    type: 'LOG_ADD_TASK_GROUP'
  }

initTask = (taskGroupId, taskId, offset, path) ->
  {
    taskId
    taskGroupId
    offset
    path
    type: 'LOG_TASK_INIT'
  }

taskGroupReady = (taskGroupId) ->
  {
    taskGroupId
    type: 'LOG_TASK_GROUP_READY'
  }

updateFilesizes = ->
  (dispatch, getState) ->
    for taskId, {path} of getState().tasks
      fetchData(taskId, path).done ({offset}) ->
        dispatch(taskFilesize(taskId, offset))

updateGroups = ->
  (dispatch, getState) ->
    getState().taskGroups.map (taskGroup, taskGroupId) ->
      unless taskGroup.pendingRequests
        if taskGroup.top
          dispatch(taskGroupFetchPrevious(taskGroupId))
        if taskGroup.bottom
          dispatch(taskGroupFetchNext(taskGroupId))

taskGroupFetchNext = (taskGroupId) ->
  (dispatch, getState) ->
    {tasks, taskGroups, logRequestLength} = getState()
    dispatch({taskGroupId, type: 'LOG_TASK_GROUP_REQUEST_START'})
    promises = taskGroups[taskGroupId].taskIds.map (taskId) ->
      {maxOffset, path, initialDataLoaded} = tasks[taskId]
      if initialDataLoaded
        xhr = fetchData(taskId, path, maxOffset, maxOffset + logRequestLength)
        xhr.done ({data, offset, nextOffset}) ->
          if data.length > 0
            nextOffset = nextOffset || offset + data.length
            dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, true))
      else
        Promise.resolve() # reject("initialDataLoaded is false for task #{taskId}")
    Promise.all(promises).then ->
      dispatch({taskGroupId, type: 'LOG_TASK_GROUP_REQUEST_END'})

taskGroupFetchPrevious = (taskGroupId) ->
  (dispatch, getState) ->
    {tasks, taskGroups, logRequestLength} = getState()
    taskGroups[taskGroupId].taskIds.map (taskId) ->
      {minOffset, path, initialDataLoaded} = tasks[taskId]
      if minOffset > 0 and initialDataLoaded
        xhr = fetchData(taskId, path, Math.max(minOffset - logRequestLength, 0), Math.min(logRequestLength, minOffset - logRequestLength))
        xhr.done ({data, offset, nextOffset}) ->
          if data.length > 0
            nextOffset = nextOffset || offset + data.length
            dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, false))
      else
        Promise.resolve() # reject("initialDataLoaded is false for task #{taskId}")

taskData = (taskGroupId, taskId, data, offset, nextOffset, append) ->
  {
    taskGroupId
    taskId
    data
    offset
    nextOffset
    append
    type: 'LOG_TASK_DATA'
  }

taskFilesize = (taskId, filesize) ->
  {
    taskId
    filesize
    type: 'LOG_TASK_FILESIZE'
  }

taskGroupTop = (taskGroupId, visible) ->
  {
    taskGroupId
    visible
    type: 'LOG_TASK_GROUP_TOP'
  }

taskGroupBottom = (taskGroupId, visible) ->
  {
    taskGroupId
    visible
    type: 'LOG_TASK_GROUP_BOTTOM'
  }

clickPermalink = (offset) ->
  {
    offset
    type: 'LOG_CLICK_OFFSET_LINK'
  }

selectLogColor = (color) ->
  {
    color
    type: 'LOG_SELECT_COLOR'
  }

switchViewMode = (newViewMode) ->
  (dispatch, getState) ->
    { taskGroups, path, requestId, search, viewMode } = getState()

    if newViewMode in ['custom', viewMode]
      return

    taskIds = _.flatten(_.pluck(taskGroups, 'taskIds'))

    dispatch({viewMode: newViewMode, type: 'LOG_SWITCH_VIEW_MODE'})

    initialize(requestId, path, search, newViewMode, taskIds)(dispatch)

setCurrentSearch = (newSearch) ->
  (dispatch, getState) ->
    {requestId, path, taskGroups, currentSearch} = getState()
    if newSearch != currentSearch
      initialize(requestId, path, newSearch, _.pluck(taskGroups, 'taskIds'))(dispatch)

toggleTaskLog = (taskId) ->
  (dispatch, getState) ->
    {path, taskGroups, tasks, viewMode} = getState()
    if tasks[taskId]
      dispatch({taskId, type: 'LOG_REMOVE_TASK'})
    else
      if viewMode is 'unified'
        taskGroupId = 0
      else
        taskGroupId = dispatch(addTaskGroup(path, '', [taskId]))
      resolvedPath = path.replace('$TASK_ID', taskId)
      fetchData(taskId, resolvedPath).done ({offset}) ->
        dispatch(initTask(taskGroupId, taskId, offset, resolvedPath))
        dispatch(taskGroupFetchPrevious(taskGroupId)).then ->
          dispatch(taskGroupReady(taskGroupId))

module.exports = {
  initialize
  initializeUsingActiveTasks
  taskGroupFetchNext
  taskGroupFetchPrevious
  clickPermalink
  updateGroups
  updateFilesizes
  taskGroupTop
  taskGroupBottom
  selectLogColor
  switchViewMode
  setCurrentSearch
  toggleTaskLog
}
