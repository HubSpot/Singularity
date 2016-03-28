{ combineReducers } = require 'redux'

updateTask = (state, taskId, update) ->
  newState = Object.assign({}, state)
  newState[taskId] = Object.assign({}, state[taskId], update)
  return newState

updateTaskGroup = (state, taskGroupId, update) ->
  newState = Object.assign([], state)
  newState[taskGroupId] = Object.assign({}, state[taskGroupId], update)
  return newState

filterLogLines = (lines, search) ->
  _.filter lines, ({data}) ->
    new RegExp(search).test(data)

tasks = (state={}, action) ->
  if action.type is 'LOG_INIT'
    newState = {}
    for taskId in _.flatten(action.taskIdGroups)
      newState[taskId] = {
        minOffset: 0
        maxOffset: 0
        filesize: 0
        initialDataLoaded: false
      }
    return newState
  else if action.type is 'LOG_ADD_TASK_GROUP'
    newState = Object.assign({}, state)
    for taskId in action.taskIds
      newState[taskId] = {
        path: action.path.replace('$TASK_ID', taskId)
        minOffset: 0
        maxOffset: 0
        filesize: 0
        initialDataLoaded: false
      }
    return newState
  else if action.type is 'LOG_REMOVE_TASK'
    newState = Object.assign({}, state)
    delete newState[action.taskId]
    return newState
  else if action.type is 'LOG_TASK_INIT'
    return updateTask(state, action.taskId, {
      path: action.path
      minOffset: action.offset
      maxOffset: action.offset
      filesize: action.offset
      initialDataLoaded: true
      })
  else if action.type is 'LOG_TASK_DATA'
    return updateTask(state, action.taskId, {
      minOffset: Math.min(state[action.taskId].minOffset, action.offset)
      maxOffset: Math.max(state[action.taskId].maxOffset, action.nextOffset)
      filesize: Math.max(state[action.taskId].filesize, action.nextOffset)
    })
  else if action.type is 'LOG_TASK_FILESIZE'
    return updateTask(state, action.taskId, {filesize: action.filesize})
  else if action.type is 'LOG_SCROLL_TO_TOP'
    newState = {}
    for taskId of state
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: 0, maxOffset: 0})
    return newState
  else if action.type is 'LOG_SCROLL_TO_BOTTOM'
    newState = {}
    for taskId of state
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: state[taskId].filesize, maxOffset: state[taskId].filesize})
    return newState
  return state

taskGroups = (state=[], action) ->
  if action.type is 'LOG_INIT'
    return action.taskIdGroups.map (taskIds) -> {
      taskIds,
      logLines: [],
      top: false,
      bottom: false,
      search: action.search,
      ready: false
      pendingRequests: false
    }
  else if action.type is 'LOG_ADD_TASK_GROUP'
    newState = Object.assign([], state)
    newState.push({
      taskIds: action.taskIds,
      logLines: [],
      top: false,
      bottom: false,
      search: action.search,
      ready: false
      pendingRequests: false
    })
    return newState
  else if action.type is 'LOG_REMOVE_TASK'
    newState = []
    for taskGroup in state
      if action.taskId in taskGroup.taskIds
        if taskGroup.taskIds.length is 1
          continue
        else
          newTaskGroup = Object.assign({}, taskGroup)
          newTaskGroup.taskIds = taskGroup.taskIds.filter (taskId) -> taskId isnt action.taskId
          newTaskGroup.logLines = taskGroup.logLines.filter (logLine) -> logLine.taskId isnt action.taskId
          newState.push(newTaskGroup)
      else
        newState.push(taskGroup)
    return newState
  else if action.type is 'LOG_TASK_GROUP_REQUEST_START'
    return updateTaskGroup(state, action.taskGroupId, {pendingRequests: true})
  else if action.type is 'LOG_TASK_GROUP_REQUEST_END'
    return updateTaskGroup(state, action.taskGroupId, {pendingRequests: false})
  else if action.type is 'LOG_TASK_GROUP_TOP'
    return updateTaskGroup(state, action.taskGroupId, {top: action.visible})
  else if action.type is 'LOG_TASK_GROUP_BOTTOM'
    return updateTaskGroup(state, action.taskGroupId, {bottom: action.visible})
  else if action.type is 'LOG_TASK_GROUP_READY'
    return updateTaskGroup(state, action.taskGroupId, {ready: true})
  else if action.type in ['LOG_SCROLL_TO_TOP', 'LOG_SCROLL_TO_BOTTOM']
    return updateTaskGroup(state, action.taskGroupId, {logLines: []})
  else if action.type is 'LOG_TASK_DATA'
    taskGroup = state[action.taskGroupId]

    offset = action.offset
    lines = _.initial(action.data.match /[^\n]*(\n|$)/g).map (data) ->
      offset += data.length
      {data, offset: offset - data.length, taskId: action.taskId}

    if taskGroup.search
      lines = filterLogLines(lines, taskGroup.search)

    if action.append
      logLines = state[action.taskGroupId].logLines.concat(lines)
      if logLines.length > action.maxLines
        logLines = logLines.slice(logLines.length - action.maxLines)
    else
      logLines = lines.concat(state[action.taskGroupId].logLines)
      if logLines.length > action.maxLines
        logLines = logLines.slice(0, action.maxLines)

    return updateTaskGroup(state, action.taskGroupId, {logLines})

    return newState
  return state

path = (state='', action) ->
  if action.type is 'LOG_INIT'
    return action.path
  return state

activeColor = (state='default', action) ->
  if action.type is 'LOG_INIT'
    return window.localStorage.logColor || 'default'
  else if action.type is 'LOG_SELECT_COLOR'
    window.localStorage.logColor = action.color
    return action.color
  return state

colors = (state=[]) -> state

viewMode = (state='custom', action) ->
  if action.type is 'LOG_SWITCH_VIEW_MODE'
    return action.viewMode
  return state

search = (state='', action) ->
  if action.type is 'LOG_INIT'
    return action.search
  return state

logRequestLength = (state=30000, action) ->
  return state

maxLines = (state=1000, action) ->
  return state

activeRequest = (state={}, action) ->
  if action.type is 'LOG_INIT'
    return Object.assign({}, state, {requestId: action.requestId})
  if action.type is 'REQUEST_ACTIVE_TASKS'
    return Object.assign({}, state, {activeTasks: action.tasks})
  return state

module.exports = combineReducers({tasks, taskGroups, activeRequest, path, activeColor, colors, viewMode, search, logRequestLength, maxLines})