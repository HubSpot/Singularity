{ combineReducers } = require 'redux'

{ getInstanceNumberFromTaskId } = require '../utils'

moment = require 'moment'

buildTask = (taskId, offset=0) ->
  {
    taskId
    minOffset: offset
    maxOffset: offset
    filesize: offset
    initialDataLoaded: false
  }

buildTaskGroup = (taskIds, search) ->
  taskGroup = {
    search
    logLines: []
    prependedLineCount: 0
    linesRemovedFromTop: 0
    updatedAt: +new Date()
    top: false
    bottom: false
    ready: false
    pendingRequests: false
    tasks: taskIds.map(buildTask)
    taskIdLookup: buildTaskLookup(taskIds)
  }

buildTaskLookup = (tasks) ->
  lookup = {}
  tasks.map (taskId, i) -> lookup[taskId] = i
  return lookup

updateTask = (state, taskId, update) ->
  newTasks = Object.assign([], state.tasks)
  index = state.taskIdLookup[taskId]
  newTasks[index] = Object.assign({}, state.tasks[index], update)
  newState = Object.assign({}, state)
  newState.tasks = newTasks
  return newState

updateTaskGroup = (state, taskGroupId, update) ->
  newState = Object.assign([], state)
  newState[taskGroupId] = Object.assign({}, state[taskGroupId], update)
  return newState

filterLogLines = (lines, search) ->
  _.filter lines, ({data}) -> new RegExp(search).test(data)


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
      if taskGroup.tasks[taskId]
        if taskGroup.taskIdsLookup.length is 1
          continue

        # remove task
        newTasks = Object.assign({}, taskGroup.tasks)
        delete newTasks[taskId]

        # update lookup map
        newTaskLookup = buildTaskLookup(newTasks)

        # remove task loglines
        newLogLines = taskGroup.logLines.filter (logLine) -> logLine.taskId isnt taskId

        newTaskGroup = Object.assign({}, taskGroup)
        newTaskGroup.tasks = newTasks
        newTaskGroup.taskIdLookup = newTaskLookup
        newTaskGroup.logLines = newLogLines
        
        newState.push(newTaskGroup)
      else
        newState.push(taskGroup)
    return newState

  # A task has been initialized
  LOG_TASK_INIT: (state, {taskGroupId, taskId, path, offset}) ->
    newTaskGroup = Object.assign({}, state[taskGroupId])
    newTaskGroup = updateTask(newTaskGroup, taskId, {
      path
      minOffset: offset
      maxOffset: offset
      filesize: offset
      initialDataLoaded: true
    })
    newState = Object.assign([], state)
    newState[taskGroupId] = newTaskGroup
    return newState

  # The logger has either entered or exited the top
  LOG_TASK_GROUP_TOP: (state, {taskGroupId, visible}) ->
    return updateTaskGroup(state, taskGroupId, {top: visible})

  # The logger has either entered or exited the bottom
  LOG_TASK_GROUP_BOTTOM: (state, {taskGroupId, visible}) ->
    updateTaskGroup(state, taskGroupId, {bottom: visible})

  # An entire task group is ready
  LOG_TASK_GROUP_READY: (state, {taskGroupId}) ->
    return updateTaskGroup(state, taskGroupId, {ready: true})

  # The logger has been asked to scroll to the top
  LOG_SCROLL_TO_TOP: (state, {}) ->
    return state.map (taskGroup) ->
      Object.assign({}, taskGroup, {
        tasks: taskGroup.tasks.map (task) -> Object.assign({}, task, {minOffset: 0, maxOffset: 0, prependedLineCount: 0})
        logLines: []
        top: false
        bottom: false
      })

  # The logger has been asked to scroll to the bottom
  LOG_SCROLL_TO_BOTTOM: (state, {}) ->
    return state.map (taskGroup) ->
      Object.assign({}, taskGroup, {
        tasks: taskGroup.tasks.map (task) -> Object.assign({}, task, {minOffset: task.filesize, maxOffset: task.filesize, prependedLineCount: 0})
        logLines: []
        top: false
        bottom: false
      })

  # We've received new filesize information for a task
  LOG_TASK_FILESIZE: (state, {taskGroupId, taskId, filesize}) ->
    newTaskGroup = Object.assign({}, state[taskGroupId])
    newTaskGroup.tasks = updateTask(state[taskGroupId], taskId, {filesize})
    return updateTaskGroup(state, taskGroupId, newTaskGroup)

  # We've received logging data for a task
  LOG_TASK_DATA: (state, {taskGroupId, taskId, offset, nextOffset, maxLines, data, append}) ->
    # bail early if no data
    if data.length is 0
      return state

    taskGroup = state[taskGroupId]
    task = taskGroup.tasks[taskGroup.taskIdLookup[taskId]]

    # split task data into separate lines
    currentOffset = offset
    lines = _.initial(data.match /[^\n]*(\n|$)/g).map (data) ->
      currentOffset += data.length
      parsedTimestamp = moment(data)
      unless parsedTimestamp.isValid()
        parsedTimestamp = moment(data, 'HH:mm:ss.SSS')
      if parsedTimestamp.isValid()
        timestamp = parsedTimestamp.valueOf()
      else
        timestamp = null
      {timestamp, data, offset: currentOffset - data.length, taskId}

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

    newLogLines = Object.assign([], taskGroup.logLines)

    prependedLineCount = 0
    updatedAt = +new Date()

    # merge in tail
    if offset > 0 and offset is task.maxOffset and not _.last(taskGroup.logLines).data.endsWith('\n')
      newLastLine = Object.assign({}, _.last(taskGroup.logLines))

      newLastLine.data = newLastLine.data + lines[0].data

      newLogLines = _.initial(newLogLines).concat(newLastLine)
      lines = _.rest(lines)

    # merge in head
    if offset + data.length is task.minOffset
      newFirstLine = Object.assign({}, taskGroup.logLines[0])
      lastLine = _.last(lines)
      unless lastLine.data.endsWith('\n')
        newFirstLine.data = lastLine.data + newFirstLine.data
        newFirstLine.offset = newFirstLine.offset - lastLine.data.length
        lines = _.initial(lines)

    # TODO: find a better location
    if taskGroup.search
      lines = filterLogLines(lines, taskGroup.search)

    # merge lines
    if append
      newLogLines = newLogLines.concat(lines)
      if newLogLines.length > maxLines
        newLogLines = newLogLines.slice(newLogLines.length - maxLines)
    else
      newLogLines = lines.concat(newLogLines)
      prependedLineCount = lines.length
      if newLogLines.length > maxLines
        newLogLines = newLogLines.slice(0, maxLines)

    newLogLines = _.sortBy(newLogLines, ({timestamp, offset}) -> [timestamp, offset])

    # update state
    newState = Object.assign([], state)
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], {logLines: newLogLines, prependedLineCount, updatedAt})
    newState[taskGroupId] = updateTask(newState[taskGroupId], taskId, {
      minOffset: _.min(newLogLines.map (line) -> line.offset),
      maxOffset: _.max(newLogLines.map (line) -> line.offset + line.data.length),
      filesize: Math.max(task.filesize, nextOffset)
    })

    return newState
}

module.exports = (state=[], action) ->
  if action.type of ACTIONS
    return ACTIONS[action.type](state, action)
  else
    return state