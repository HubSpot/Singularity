Q = require 'q'

{ fetchTasksForRequest } = require './activeTasks'

fetchData = (taskId, path, offset=undefined, length=0) ->
  length = Math.max(length, 0)  # API breaks if you request a negative length
  $.ajax
    url: "#{ config.apiRoot }/sandbox/#{ taskId }/read?#{$.param({path, length, offset})}"

fetchTaskHistory = (taskId) ->
  $.ajax
    url: "#{ config.apiRoot }/history/task/#{ taskId }"

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
      taskInitPromises = taskIds.map (taskId) ->
        taskInitDeferred = Q.defer()
        resolvedPath = path.replace('$TASK_ID', taskId)
        fetchData(taskId, resolvedPath).done ({offset}) ->
          dispatch(initTask(taskId, offset, resolvedPath, true))
          taskInitDeferred.resolve()
        .error ({status}) ->
          if status is 404
            app.caughtError()
            dispatch(taskFileDoesNotExist(taskGroupId, taskId))
            taskInitDeferred.resolve()
          else
            taskInitDeferred.reject()
        return taskInitDeferred.promise

      taskStatusPromises = taskIds.map (taskId) ->
        dispatch(updateTaskStatus(taskGroupId, taskId))

      Promise.all(taskInitPromises, taskStatusPromises).then ->
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

addTaskGroup = (taskIds, search) ->
  {
    taskIds
    search
    type: 'LOG_ADD_TASK_GROUP'
  }

initTask = (taskId, offset, path, exists) ->
  {
    taskId
    offset
    path
    exists
    type: 'LOG_TASK_INIT'
  }

taskFileDoesNotExist = (taskGroupId, taskId) ->
  {
    taskId
    taskGroupId
    type: 'LOG_TASK_FILE_DOES_NOT_EXIST'
  }

taskGroupReady = (taskGroupId) ->
  {
    taskGroupId
    type: 'LOG_TASK_GROUP_READY'
  }

taskHistory = (taskGroupId, taskId, taskHistory) ->
  {
    taskGroupId
    taskId
    taskHistory
    type: 'LOG_TASK_HISTORY'
  }

getTasks = (taskGroup, tasks) ->
  taskGroup.taskIds.map (taskId) -> tasks[taskId]

updateFilesizes = ->
  (dispatch, getState) ->
    { tasks } = getState()
    for taskId of tasks
      fetchData(taskId, tasks[taskId.path]).done ({offset}) ->
        dispatch(taskFilesize(taskId, offset))

updateGroups = ->
  (dispatch, getState) ->
    getState().taskGroups.map (taskGroup, taskGroupId) ->
      unless taskGroup.pendingRequests
        if taskGroup.top
          dispatch(taskGroupFetchPrevious(taskGroupId))
        if taskGroup.bottom or taskGroup.tailing
          dispatch(taskGroupFetchNext(taskGroupId))

updateTaskStatuses = ->
  (dispatch, getState) ->
    {tasks, taskGroups} = getState()
    taskGroups.map (taskGroup, taskGroupId) ->
      getTasks(taskGroup, tasks).map ({taskId, terminated}) ->
        if terminated
          Promise.resolve()
        else
          dispatch(updateTaskStatus(taskGroupId, taskId))

updateTaskStatus = (taskGroupId, taskId) ->
  (dispatch, getState) ->
    fetchTaskHistory(taskId, ['taskUpdates']).done (data) ->
      dispatch(taskHistory(taskGroupId, taskId, data))

taskGroupFetchNext = (taskGroupId) ->
  (dispatch, getState) ->
    {tasks, taskGroups, logRequestLength, maxLines} = getState()

    taskGroup = taskGroups[taskGroupId]
    tasks = getTasks(taskGroup, tasks)

    # bail early if there's already a pending request
    if taskGroup.pendingRequests
      return Promise.resolve()

    dispatch({taskGroupId, type: 'LOG_REQUEST_START'})
    promises = tasks.map ({taskId, maxOffset, path, initialDataLoaded}) ->
      if initialDataLoaded
        xhr = fetchData(taskId, path, maxOffset, logRequestLength)
        xhr.done ({data, offset, nextOffset}) ->
          if data.length > 0
            nextOffset = offset + data.length
            dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, true, maxLines))
      else
        Promise.resolve() # reject("initialDataLoaded is false for task #{taskId}")

    Promise.all(promises).then -> dispatch({taskGroupId, type: 'LOG_REQUEST_END'})

taskGroupFetchPrevious = (taskGroupId) ->
  (dispatch, getState) ->
    {tasks, taskGroups, logRequestLength, maxLines} = getState()

    taskGroup = taskGroups[taskGroupId]
    tasks = getTasks(taskGroup, tasks)

    # bail early if all tasks are at the top
    if _.all(tasks.map ({minOffset}) -> minOffset is 0)
      return Promise.resolve()

    # bail early if there's already a pending request
    if taskGroup.pendingRequests
      return Promise.resolve()

    dispatch({taskGroupId, type: 'LOG_REQUEST_START'})
    promises = tasks.map ({taskId, minOffset, path, initialDataLoaded}) ->
      if minOffset > 0 and initialDataLoaded
        xhr = fetchData(taskId, path, Math.max(minOffset - logRequestLength, 0), Math.min(logRequestLength, minOffset))
        xhr.done ({data, offset, nextOffset}) ->
          if data.length > 0
            nextOffset = offset + data.length
            dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, false, maxLines))
      else
        Promise.resolve() # reject("initialDataLoaded is false for task #{taskId}")

    Promise.all(promises).then -> dispatch({taskGroupId, type: 'LOG_REQUEST_END'})

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
  (dispatch, getState) ->
    if getState().taskGroups[taskGroupId].top != visible
      dispatch({taskGroupId, visible, type: 'LOG_TASK_GROUP_TOP'})
      if visible
        dispatch(taskGroupFetchPrevious(taskGroupId))

taskGroupBottom = (taskGroupId, visible, tailing=false) ->
  (dispatch, getState) ->
    { taskGroups, tasks } = getState()
    taskGroup = taskGroups[taskGroupId]
    if taskGroup.tailing != tailing
      if tailing is false or _.all(getTasks(taskGroup, tasks).map(({maxOffset, filesize}) -> maxOffset >= filesize))
        dispatch({taskGroupId, tailing, type: 'LOG_TASK_GROUP_TAILING'})
    if taskGroup.bottom != visible
      dispatch({taskGroupId, visible, type: 'LOG_TASK_GROUP_BOTTOM'})
      if visible
        dispatch(taskGroupFetchNext(taskGroupId))

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

setCurrentSearch = (newSearch) ->  # TODO: can we do something less heavyweight?
  (dispatch, getState) ->
    {activeRequest, path, taskGroups, currentSearch} = getState()
    if newSearch != currentSearch
      dispatch(initialize(activeRequest.requestId, path, newSearch, _.flatten(_.pluck(taskGroups, 'taskIds'))))

toggleTaskLog = (taskId) ->
  (dispatch, getState) ->
    {search, path, tasks, viewMode} = getState()
    if taskId of tasks
      # only remove task if it's not the last one
      if Object.keys(tasks).length > 1
        dispatch({taskId, type: 'LOG_REMOVE_TASK'})
      else
        return
    else
      if viewMode is 'split'
        dispatch(addTaskGroup([taskId], search))

      resolvedPath = path.replace('$TASK_ID', taskId)

      fetchData(taskId, resolvedPath).done ({offset}) ->
        dispatch(initTask(taskId, offset, resolvedPath, true))

        getState().taskGroups.map (taskGroup, taskGroupId) ->
          if taskId in taskGroup.taskIds
            dispatch(updateTaskStatus(taskGroupId, taskId))
            dispatch(taskGroupFetchPrevious(taskGroupId)).then ->
              dispatch(taskGroupReady(taskGroupId))

removeTaskGroup = (taskGroupId) ->
  (dispatch, getState) ->
    { taskIds } = getState().taskGroups[taskGroupId]
    dispatch({taskGroupId, taskIds, type: 'LOG_REMOVE_TASK_GROUP'})

expandTaskGroup = (taskGroupId) ->
  (dispatch, getState) ->
    { taskIds } = getState().taskGroups[taskGroupId]
    dispatch({taskGroupId, taskIds, type: 'LOG_EXPAND_TASK_GROUP'})

scrollToTop = (taskGroupId) ->
  (dispatch, getState) ->
    { taskIds } = getState().taskGroups[taskGroupId]
    dispatch({taskGroupId, taskIds, type: 'LOG_SCROLL_TO_TOP'})
    dispatch(taskGroupFetchNext(taskGroupId))

scrollAllToTop = () ->
  (dispatch, getState) ->
    dispatch({type: 'LOG_SCROLL_ALL_TO_TOP'})
    getState().taskGroups.map (taskGroup, taskGroupId) ->
      dispatch(taskGroupFetchNext(taskGroupId))

scrollToBottom = (taskGroupId) ->
  (dispatch, getState) ->
    { taskIds } = getState().taskGroups[taskGroupId]
    dispatch({taskGroupId, taskIds, type: 'LOG_SCROLL_TO_BOTTOM'})
    dispatch(taskGroupFetchPrevious(taskGroupId))

scrollAllToBottom = () ->
  (dispatch, getState) ->
    dispatch({type: 'LOG_SCROLL_ALL_TO_BOTTOM'})
    getState().taskGroups.map (taskGroup, taskGroupId) ->
      dispatch(taskGroupFetchPrevious(taskGroupId))

module.exports = {
  initialize
  initializeUsingActiveTasks
  taskGroupFetchNext
  taskGroupFetchPrevious
  clickPermalink
  updateGroups
  updateTaskStatuses
  updateFilesizes
  taskGroupTop
  taskGroupBottom
  selectLogColor
  switchViewMode
  setCurrentSearch
  toggleTaskLog
  scrollToTop
  scrollAllToTop
  scrollToBottom
  scrollAllToBottom
  removeTaskGroup
  expandTaskGroup
}
