{ combineReducers } = require 'redux'

{ getInstanceNumberFromTaskId } = require '../utils'

moment = require 'moment'

buildTaskGroup = (taskIds, search) ->
  {
    taskIds
    search
    logLines: []
    taskBuffer: {}
    prependedLineCount: 0
    linesRemovedFromTop: 0
    updatedAt: +new Date()
    top: false
    bottom: false
    tailing: false
    ready: false
    pendingRequests: false
    detectedTimestamp: false
  }

resetTaskGroup = (tailing=false) -> {
  logLines: []
  taskBuffer: {}
  top: false
  bottom: false
  updatedAt: +new Date()
  tailing
}

updateTaskGroup = (state, taskGroupId, update) ->
  newState = Object.assign([], state)
  newState[taskGroupId] = Object.assign({}, state[taskGroupId], update)
  return newState

filterLogLines = (lines, search) ->
  _.filter lines, ({data}) -> new RegExp(search).test(data)

TIMESTAMP_REGEX = [
  [/^(\d{2}:\d{2}:\d{2}\.\d{3})/, 'HH:mm:ss.SSS']
  [/^[A-Z \[]+(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})/, 'YYYY-MM-DD HH:mm:ss,SSS']
  [/^\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})/, 'YYYY-MM-DD HH:mm:ss,SSS']
]

parseLineTimestamp = (line) ->
  for group in TIMESTAMP_REGEX
    match = line.match(group[0])
    if match
      return moment(match, group[1]).valueOf()
  return null

buildEmptyBuffer = (taskId, offset) -> { offset, taskId, data: '' }

ACTIONS = {
  # The logger is being initialized
  LOG_INIT: (state, {taskIdGroups, search}) ->
    return taskIdGroups.map (taskIds) -> buildTaskGroup(taskIds, search)

  # Add a group of tasks to the logger
  LOG_ADD_TASK_GROUP: (state, {taskIds}) ->
    newState = state.concat(buildTaskGroup(taskIds, state.search))
    return _.sortBy(newState, (taskGroup) -> getInstanceNumberFromTaskId(taskGroup.taskIds[0]))

  # Remove a task from the logger
  LOG_REMOVE_TASK: (state, {taskId}) ->
    newState = []
    for taskGroup in state
      if taskId in taskGroup.taskIds
        if taskGroup.taskIds.length is 1
          continue

        # remove task
        newTaskIds = _.without(taskGroup.taskIds, taskId)

        # remove task loglines
        newLogLines = taskGroup.logLines.filter (logLine) -> logLine.taskId isnt taskId

        newState.push(Object.assign({}, taskGroup, {tasksIds: newTasksIds, logLines: newLogLines}))
      else
        newState.push(taskGroup)
    return newState

  # The logger has either entered or exited the top
  LOG_TASK_GROUP_TOP: (state, {taskGroupId, visible}) ->
    return updateTaskGroup(state, taskGroupId, {top: visible, tailing: false})

  # The logger has either entered or exited the bottom
  LOG_TASK_GROUP_BOTTOM: (state, {taskGroupId, visible}) ->
    return updateTaskGroup(state, taskGroupId, {bottom: visible})

  # An entire task group is ready
  LOG_TASK_GROUP_READY: (state, {taskGroupId}) ->
    return updateTaskGroup(state, taskGroupId, {ready: true, updatedAt: +new Date(), tailing: true})

  LOG_TASK_GROUP_TAILING: (state, {taskGroupId, tailing}) ->
    return updateTaskGroup(state, taskGroupId, {tailing})

  LOG_REMOVE_TASK_GROUP: (state, {taskGroupId}) ->
    newState = []
    for i in [0..state.length-1]
      unless i is taskGroupId
        newState.push(state[i])
    return newState

  LOG_EXPAND_TASK_GROUP: (state, {taskGroupId}) ->
    return [state[taskGroupId]]

  # The logger has been asked to scroll to the top
  LOG_SCROLL_ALL_GROUPS_TO_TOP: (state) ->
    return state.map (taskGroup) ->
      Object.assign({}, taskGroup, resetTaskGroup())

  LOG_SCROLL_TO_TOP: (state, {taskGroupId}) ->
    newState = Object.assign([], state)
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], resetTaskGroup())
    return newState

  LOG_SCROLL_ALL_TO_TOP: (state) ->
    state.map (taskGroup) -> Object.assign({}, taskGroup, resetTaskGroup())

  LOG_SCROLL_TO_BOTTOM: (state, {taskGroupId}) ->
    newState = Object.assign([], state)
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], resetTaskGroup(true))
    return newState

  LOG_SCROLL_ALL_TO_BOTTOM: (state) ->
    state.map (taskGroup) -> Object.assign({}, taskGroup, resetTaskGroup(true))

  LOG_REQUEST_START: (state, {taskGroupId}) ->
    newState = Object.assign([], state)
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], {pendingRequests: true})
    return newState

  LOG_REQUEST_END: (state, {taskGroupId}) ->
    newState = Object.assign([], state)
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], {pendingRequests: false})
    return newState

  # We've received logging data for a task
  LOG_TASK_DATA: (state, {taskGroupId, taskId, offset, nextOffset, maxLines, data, append}) ->
    taskGroup = state[taskGroupId]

    # bail early if no data
    if data.length is 0 and task.loadedData
      return state

    # split task data into separate lines, attempt to parse timestamp
    currentOffset = offset
    lines = _.initial(data.match /[^\n]*(\n|$)/g).map (data) ->
      currentOffset += data.length

      timestamp = parseLineTimestamp(data)

      if timestamp
        detectedTimestamp = true

      {timestamp, data, offset: currentOffset - data.length, taskId}

    # task buffers
    taskBuffer = taskGroup.taskBuffer[taskId] || buildEmptyBuffer(taskId, 0)

    if append
      if taskBuffer.offset + taskBuffer.data.length is offset
        firstLine = _.first(lines)
        lines = _.rest(lines)
        taskBuffer = {offset: taskBuffer.offset, data: taskBuffer.data + firstLine.data, taskId}
        if taskBuffer.data.endsWith('\n')
          taskBuffer.timestamp = parseLineTimestamp(taskBuffer.data)
          lines.unshift(taskBuffer)
          taskBuffer = buildEmptyBuffer(taskId, nextOffset)
      if lines.length > 0
        lastLine = _.last(lines)
        if not lastLine.data.endsWith('\n')
          taskBuffer = lastLine
          lines = _.initial(lines)
    else
      if nextOffset is taskBuffer.offset
        lastLine = _.last(lines)
        lines = _.initial(lines)
        taskBuffer = {offset: nextOffset - lastLine.data.length, data: lastLine.data + taskBuffer.data, taskId}
        if lines.length > 0
          taskBuffer.timestamp = parseLineTimestamp(taskBuffer.data)
          lines.push(taskBuffer)
          taskBuffer = buildEmptyBuffer(taskId, offset)
      if lines.length > 0
        firstLine = _.first(lines)
        if firstLine.offset > 0
          taskBuffer = firstLine
          lines = _.rest(lines)

    newTaskBuffer = Object.assign({}, taskGroup.taskBuffer)
    newTaskBuffer[taskId] = taskBuffer

    # backfill old timestamps
    if taskGroup.logLines.length > 0
      lastTimestamp = _.last(taskGroup.logLines).timestamp
    else
      lastTimestamp = 0

    lines = lines.map (line) ->
      if line.timestamp
        lastTimestamp = line.timestamp
      else
        line.timestamp = lastTimestamp
      return line

    prependedLineCount = 0
    linesRemovedFromTop = 0
    updatedAt = +new Date()

    # search
    if taskGroup.search
      lines = filterLogLines(lines, taskGroup.search)

    # merge lines
    newLogLines = Object.assign([], taskGroup.logLines)
    if append
      newLogLines = newLogLines.concat(lines)
      if newLogLines.length > maxLines
        linesRemovedFromTop = newLogLines.length - maxLines
        newLogLines = newLogLines.slice(newLogLines.length - maxLines)
    else
      newLogLines = lines.concat(newLogLines)
      prependedLineCount = lines.length
      if newLogLines.length > maxLines
        newLogLines = newLogLines.slice(0, maxLines)

    # sort lines by timestamp if unified view
    if taskGroup.taskIds.length > 1
      newLogLines = _.sortBy(newLogLines, ({timestamp, offset}) -> [timestamp, offset])

    # update state
    newState = Object.assign([], state)
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], {taskBuffer: newTaskBuffer, logLines: newLogLines, prependedLineCount, linesRemovedFromTop, updatedAt})
    return newState
}

module.exports = (state=[], action) ->
  if action.type of ACTIONS
    return ACTIONS[action.type](state, action)
  else
    return state