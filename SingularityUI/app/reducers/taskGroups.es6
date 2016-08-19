import Utils from '../utils';

import moment from 'moment';

let buildTaskGroup = (taskIds, search) =>
  ({
    taskIds,
    search,
    logLines: [],
    taskBuffer: {},
    prependedLineCount: 0,
    linesRemovedFromTop: 0,
    updatedAt: +new Date(),
    top: false,
    bottom: false,
    tailing: false,
    ready: false,
    pendingRequests: false,
    detectedTimestamp: false
  })
;

let resetTaskGroup = (tailing = false) => ({
  logLines: [],
  taskBuffer: {},
  top: true,
  bottom: true,
  updatedAt: +new Date(),
  tailing
});

let updateTaskGroup = function(state, taskGroupId, update) {
  let newState = Object.assign([], state);
  newState[taskGroupId] = Object.assign({}, state[taskGroupId], update);
  return newState;
};

let filterLogLines = (lines, search) => _.filter(lines, ({data}) => new RegExp(search).test(data));

let TIMESTAMP_REGEX = [
  [/^(\d{2}:\d{2}:\d{2}\.\d{3})/, 'HH:mm:ss.SSS'],
  [/^[A-Z \[]+(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})/, 'YYYY-MM-DD HH:mm:ss,SSS'],
  [/^\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})/, 'YYYY-MM-DD HH:mm:ss,SSS']
];

let parseLineTimestamp = function(line) {
  for (let i = 0; i < TIMESTAMP_REGEX.length; i++) {
    let group = TIMESTAMP_REGEX[i];
    let match = line.match(group[0]);
    if (match) {
      return moment(match, group[1]).valueOf();
    }
  }
  return null;
};

let buildEmptyBuffer = (taskId, offset) => ({ offset, taskId, data: '' });

const ACTIONS = {
  // The logger is being initialized
  LOG_INIT(state, {taskIdGroups, search}) {
    return taskIdGroups.map(taskIds => buildTaskGroup(taskIds, search));
  },

  // Add a group of tasks to the logger
  LOG_ADD_TASK_GROUP(state, {taskIds, search}) {
    return _.sortBy(state.concat(buildTaskGroup(taskIds, search)), taskGroup => Utils.getTaskDataFromTaskId(taskGroup.taskIds[0]).instanceNo);
  },

  // Remove a task from the logger
  LOG_REMOVE_TASK(state, {taskId}) {
    const newState = [];
    for (let i = 0; i < state.length; i++) {
      let taskGroup = state[i];
      if (__in__(taskId, taskGroup.taskIds)) {
        // remove task group if it only has one task
        if (taskGroup.taskIds.length === 1) {
          continue;
        }

        // remove task
        const newTaskIds = _.without(taskGroup.taskIds, taskId);

        // remove task loglines
        const newLogLines = taskGroup.logLines.filter(logLine => logLine.taskId !== taskId);

        newState.push(Object.assign({}, taskGroup, {tasksIds: newTaskIds, logLines: newLogLines}));
      } else {
        newState.push(taskGroup);
      }
    }
    return newState;
  },

  // The logger has either entered or exited the top
  LOG_TASK_GROUP_TOP(state, {taskGroupId, visible}) {
    return updateTaskGroup(state, taskGroupId, {top: visible, tailing: false});
  },

  // The logger has either entered or exited the bottom
  LOG_TASK_GROUP_BOTTOM(state, {taskGroupId, visible}) {
    return updateTaskGroup(state, taskGroupId, {bottom: visible});
  },

  // An entire task group is ready
  LOG_TASK_GROUP_READY(state, {taskGroupId}) {
    return updateTaskGroup(state, taskGroupId, {ready: true, updatedAt: +new Date(), top: true, bottom: true, tailing: true});
  },

  LOG_TASK_GROUP_TAILING(state, {taskGroupId, tailing}) {
    return updateTaskGroup(state, taskGroupId, {tailing});
  },

  LOG_REMOVE_TASK_GROUP(state, {taskGroupId}) {
    let newState = [];
    let iterable = __range__(0, state.length - 1, true);
    for (let j = 0; j < iterable.length; j++) {
      let i = iterable[j];
      if (i !== taskGroupId) {
        newState.push(state[i]);
      }
    }
    return newState;
  },

  LOG_EXPAND_TASK_GROUP(state, {taskGroupId}) {
    return [state[taskGroupId]];
  },

  LOG_SCROLL_TO_TOP(state, {taskGroupId}) {
    return updateTaskGroup(state, taskGroupId, resetTaskGroup());
  },

  LOG_SCROLL_ALL_TO_TOP(state) {
    return state.map(taskGroup => Object.assign({}, taskGroup, resetTaskGroup()));
  },

  LOG_SCROLL_TO_BOTTOM(state, {taskGroupId}) {
    return updateTaskGroup(state, taskGroupId, resetTaskGroup(true));
  },

  LOG_SCROLL_ALL_TO_BOTTOM(state) {
    return state.map(taskGroup => Object.assign({}, taskGroup, resetTaskGroup(true)));
  },

  LOG_REQUEST_START(state, {taskGroupId}) {
    return updateTaskGroup(state, taskGroupId, {pendingRequests: true});
  },

  LOG_REQUEST_END(state, {taskGroupId}) {
    return updateTaskGroup(state, taskGroupId, {pendingRequests: false});
  },

  // We've received logging data for a task
  LOG_TASK_DATA(state, {taskGroupId, taskId, offset, nextOffset, maxLines, data, append}) {
    let taskGroup = state[taskGroupId];

    // bail early if no data
    if (data.length === 0 && task.loadedData) {
      return state;
    }

    // split task data into separate lines, attempt to parse timestamp
    let currentOffset = offset;
    let lines = _.initial(data.match(/[^\n]*(\n|$)/g)).map(function(data) {
      currentOffset += data.length;

      data = data.replace('\r', '');  // carriage return screws stuff up

      let timestamp = parseLineTimestamp(data);

      if (timestamp) {
        let detectedTimestamp = true;
      }

      return {timestamp, data, offset: currentOffset - data.length, taskId};
    });

    // task buffers
    let taskBuffer = taskGroup.taskBuffer[taskId] || buildEmptyBuffer(taskId, 0);

    if (append) {
      if (taskBuffer.offset + taskBuffer.data.length === offset) {
        var firstLine = _.first(lines);
        lines = _.rest(lines);
        taskBuffer = {offset: taskBuffer.offset, data: taskBuffer.data + firstLine.data, taskId};
        if (taskBuffer.data.endsWith('\n')) {
          taskBuffer.timestamp = parseLineTimestamp(taskBuffer.data);
          lines.unshift(taskBuffer);
          taskBuffer = buildEmptyBuffer(taskId, nextOffset);
        }
      }
      if (lines.length > 0) {
        var lastLine = _.last(lines);
        if (!lastLine.data.endsWith('\n')) {
          taskBuffer = lastLine;
          lines = _.initial(lines);
        }
      }
    } else {
      if (nextOffset === taskBuffer.offset) {
        var lastLine = _.last(lines);
        lines = _.initial(lines);
        taskBuffer = {offset: nextOffset - lastLine.data.length, data: lastLine.data + taskBuffer.data, taskId};
        if (lines.length > 0) {
          taskBuffer.timestamp = parseLineTimestamp(taskBuffer.data);
          lines.push(taskBuffer);
          taskBuffer = buildEmptyBuffer(taskId, offset);
        }
      }
      if (lines.length > 0) {
        var firstLine = _.first(lines);
        if (firstLine.offset > 0) {
          taskBuffer = firstLine;
          lines = _.rest(lines);
        }
      }
    }

    let newTaskBuffer = Object.assign({}, taskGroup.taskBuffer);
    newTaskBuffer[taskId] = taskBuffer;

    // backfill old timestamps
    if (taskGroup.logLines.length > 0) {
      var lastTimestamp = _.last(taskGroup.logLines).timestamp;
    } else {
      var lastTimestamp = 0;
    }

    lines = lines.map(function(line) {
      if (line.timestamp) {
        var lastTimestamp = line.timestamp;
      } else {
        line.timestamp = lastTimestamp;
      }
      return line;
    });

    let prependedLineCount = 0;
    let linesRemovedFromTop = 0;
    let updatedAt = +new Date();

    // search
    if (taskGroup.search) {
      lines = filterLogLines(lines, taskGroup.search);
    }

    // merge lines
    let newLogLines = Object.assign([], taskGroup.logLines);
    if (append) {
      newLogLines = newLogLines.concat(lines);
      if (newLogLines.length > maxLines) {
        linesRemovedFromTop = newLogLines.length - maxLines;
        newLogLines = newLogLines.slice(newLogLines.length - maxLines);
      }
    } else {
      newLogLines = lines.concat(newLogLines);
      prependedLineCount = lines.length;
      if (newLogLines.length > maxLines) {
        newLogLines = newLogLines.slice(0, maxLines);
      }
    }

    // sort lines by timestamp if unified view
    if (taskGroup.taskIds.length > 1) {
      newLogLines = _.sortBy(newLogLines, ({timestamp, offset}) => [timestamp, offset]);
    }

    // update state
    let newState = Object.assign([], state);
    newState[taskGroupId] = Object.assign({}, state[taskGroupId], {taskBuffer: newTaskBuffer, logLines: newLogLines, prependedLineCount, linesRemovedFromTop, updatedAt});
    return newState;
  }
};

export default function(state = [], action) {
  if (action.type in ACTIONS) {
    return ACTIONS[action.type](state, action);
  } else {
    return state;
  }
}

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}
function __range__(left, right, inclusive) {
  let range = [];
  let ascending = left < right;
  let end = !inclusive ? right : ascending ? right + 1 : right - 1;
  for (let i = left; ascending ? i < end : i > end; ascending ? i++ : i--) {
    range.push(i);
  }
  return range;
}
