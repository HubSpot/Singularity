updateTask = (state, taskId, updates) ->
  newState = Object.assign({}, state)
  newState[taskId] = Object.assign({}, state[taskId], updates)
  return newState

buildTask = (taskId, offset=0) ->
  {
    taskId
    minOffset: offset
    maxOffset: offset
    filesize: offset
    initialDataLoaded: false
    logDataLoaded: false
    terminated: false
    exists: false
  }

getLastTaskUpdate = (taskUpdates) ->
  if taskUpdates.length > 0
    _.last(_.sortBy(taskUpdates, (taskUpdate) -> taskUpdate.timestamp)).taskState
  else
    return null

isTerminalTaskState = (taskState) ->
  taskState in ['TASK_FINISHED', 'TASK_KILLED', 'TASK_FAILED', 'TASK_LOST', 'TASK_ERROR']

ACTIONS = {
  LOG_INIT: (state, {taskIdGroups}) ->
    newState = {}
    for taskIdGroup in taskIdGroups
      for taskId in taskIdGroup
        newState[taskId] = buildTask(taskId)
    return newState
  LOG_ADD_TASK_GROUP: (state, {taskIds}) ->
    newState = Object.assign({}, state)
    for taskId in taskIds
      newState[taskId] = buildTask(taskId)
    return newState
  LOG_REMOVE_TASK: (state, {taskId}) ->
    newState = Object.assign({}, state)
    delete newState[taskId]
    return newState
  LOG_TASK_INIT: (state, {taskId, path, offset, exists}) ->
    updateTask(state, taskId, {
      path
      exists
      minOffset: offset
      maxOffset: offset
      filesize: offset
      initialDataLoaded: true
    })
  LOG_TASK_FILE_DOES_NOT_EXIST: (state, {taskId}) ->
    updateTask(state, taskId, {exists: false, initialDataLoaded: true})
  LOG_SCROLL_TO_TOP: (state, {taskIds}) ->
    newState = Object.assign({}, state)
    for taskId in taskIds
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: 0, maxOffset: 0, logDataLoaded: false})
    return newState
  LOG_SCROLL_ALL_TO_TOP: (state) ->
    newState = {}
    for taskId of state
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: 0, maxOffset: 0, logDataLoaded: false})
    return newState
  LOG_SCROLL_TO_BOTTOM: (state, {taskIds}) ->
    newState = Object.assign({}, state)
    for taskId in taskIds
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: state[taskId].filesize, maxOffset: state[taskId].filesize, logDataLoaded: false})
    return newState
  LOG_SCROLL_ALL_TO_BOTTOM: (state) ->
    newState = {}
    for taskId of state
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: state[taskId].filesize, maxOffset: state[taskId].filesize, logDataLoaded: false})
    return newState
  LOG_TASK_FILESIZE: (state, {taskId, filesize}) ->
    updateTask(state, taskId, {filesize})
  LOG_TASK_DATA: (state, {taskId, offset, nextOffset}) ->
    {minOffset, maxOffset, filesize} = state[taskId]
    updateTask(state, taskId, {logDataLoaded: true, minOffset: Math.min(minOffset, offset), maxOffset: Math.max(maxOffset, nextOffset), filesize: Math.max(nextOffset, filesize)})
  LOG_TASK_HISTORY: (state, {taskId, taskHistory}) ->
    lastTaskStatus = getLastTaskUpdate(taskHistory.taskUpdates)
    updateTask(state, taskId, {lastTaskStatus, terminated: isTerminalTaskState(lastTaskStatus)})
  LOG_REMOVE_TASK_GROUP: (state, {taskIds}) ->
    newState = Object.assign({}, state)
    for taskId in taskIds
      delete newState[taskId]
    return newState
  LOG_EXPAND_TASK_GROUP: (state, {taskIds}) ->
    newState = {}
    for taskId in taskIds
      newState[taskId] = state[taskId]
    return newState
}

module.exports = (state={}, action) ->
  if action.type of ACTIONS
    return ACTIONS[action.type](state, action)
  else
    return state