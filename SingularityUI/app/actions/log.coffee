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
        dispatch(taskGroupFetchPrevious(taskGroupId)).then ->
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
    {tasks, taskGroups, logRequestLength, maxLines} = getState()

    promises = taskGroups[taskGroupId].taskIds.map (taskId) ->
      {maxOffset, path, initialDataLoaded} = tasks[taskId]
      if initialDataLoaded
        xhr = fetchData(taskId, path, maxOffset, logRequestLength)
        xhr.done ({data, offset, nextOffset}) ->
          if data.length > 0
            nextOffset = nextOffset || offset + data.length
            dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, true, maxLines))
      else
        Promise.resolve() # reject("initialDataLoaded is false for task #{taskId}")

    Promise.all(promises)

taskGroupFetchPrevious = (taskGroupId) ->
  (dispatch, getState) ->
    {tasks, taskGroups, logRequestLength, maxLines} = getState()

    promises = taskGroups[taskGroupId].taskIds.map (taskId) ->
      {minOffset, path, initialDataLoaded} = tasks[taskId]
      if minOffset > 0 and initialDataLoaded
        xhr = fetchData(taskId, path, Math.max(minOffset - logRequestLength, 0), Math.min(logRequestLength, minOffset))
        xhr.done ({data, offset, nextOffset}) ->
          if data.length > 0
            nextOffset = nextOffset || offset + data.length
            dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, false, maxLines))
      else
        Promise.resolve() # reject("initialDataLoaded is false for task #{taskId}")

    Promise.all(promises)

taskData = (taskGroupId, taskId, data, offset, nextOffset, append, maxLines) ->
  {
    taskGroupId
    taskId
    data
    offset
    nextOffset
    append
    maxLines
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
    { taskGroups, path, activeRequest, search, viewMode } = getState()

    if newViewMode in ['custom', viewMode]
      return

    taskIds = _.flatten(_.pluck(taskGroups, 'taskIds'))

    dispatch({viewMode: newViewMode, type: 'LOG_SWITCH_VIEW_MODE'})
    dispatch(initialize(activeRequest.requestId, path, search, taskIds))

setCurrentSearch = (newSearch) ->
  (dispatch, getState) ->
    {activeRequest, path, taskGroups, currentSearch} = getState()
    if newSearch != currentSearch
      dispatch(initialize(activeRequest.requestId, path, newSearch, _.flatten(_.pluck(taskGroups, 'taskIds'))))

toggleTaskLog = (taskId) ->
  (dispatch, getState) ->
    {search, path, tasks, viewMode} = getState()
    if tasks[taskId]
      if Object.keys(tasks).length > 1
        dispatch({taskId, type: 'LOG_REMOVE_TASK'})
    else
      if viewMode is 'split'
        dispatch(addTaskGroup(path, search, [taskId]))
        taskGroupId = getState().taskGroups.length - 1
      else
        taskGroupId = 0
      resolvedPath = path.replace('$TASK_ID', taskId)
      fetchData(taskId, resolvedPath).done ({offset}) ->
        dispatch(initTask(taskGroupId, taskId, offset, resolvedPath))
        dispatch(taskGroupFetchPrevious(taskGroupId)).then ->
          dispatch(taskGroupReady(taskGroupId))

scrollToTop = () ->
  (dispatch, getState) ->
    getState().taskGroups.map (taskGroup, taskGroupId) ->
      dispatch({taskGroupId, type: 'LOG_SCROLL_TO_TOP'})
      dispatch(taskGroupFetchNext(taskGroupId))

scrollToBottom = () ->
  (dispatch, getState) ->
    getState().taskGroups.map (taskGroup, taskGroupId) ->
      dispatch({taskGroupId, type: 'LOG_SCROLL_TO_BOTTOM'})
      dispatch(taskGroupFetchPrevious(taskGroupId))

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
  scrollToTop
  scrollToBottom
}
