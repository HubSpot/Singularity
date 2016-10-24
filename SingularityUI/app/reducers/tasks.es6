let updateTask = function(state, taskId, updates) {
  let newState = Object.assign({}, state);
  newState[taskId] = Object.assign({}, state[taskId], updates);
  return newState;
};

let buildTask = (taskId, offset=0) =>
  ({
    taskId,
    minOffset: offset,
    maxOffset: offset,
    filesize: offset,
    initialDataLoaded: false,
    logDataLoaded: false,
    terminated: false,
    exists: false
  })
;

let getLastTaskUpdate = function(taskUpdates) {
  if (taskUpdates.length > 0) {
    return _.last(_.sortBy(taskUpdates, taskUpdate => taskUpdate.timestamp)).taskState;
  } else {
    return null;
  }
};

let isTerminalTaskState = taskState => __in__(taskState, ['TASK_FINISHED', 'TASK_KILLED', 'TASK_FAILED', 'TASK_LOST', 'TASK_ERROR']);

const ACTIONS = {
  LOG_INIT(state, {taskIdGroups}) {
    let newState = {};
    for (let i = 0; i < taskIdGroups.length; i++) {
      let taskIdGroup = taskIdGroups[i];
      for (let j = 0; j < taskIdGroup.length; j++) {
        let taskId = taskIdGroup[j];
        newState[taskId] = buildTask(taskId);
      }
    }
    return newState;
  },
  LOG_ADD_TASK_GROUP(state, {taskIds}) {
    let newState = Object.assign({}, state);
    for (let i = 0; i < taskIds.length; i++) {
      let taskId = taskIds[i];
      newState[taskId] = buildTask(taskId);
    }
    return newState;
  },
  LOG_REMOVE_TASK(state, {taskId}) {
    let newState = Object.assign({}, state);
    delete newState[taskId];
    return newState;
  },
  LOG_FINISHED_LOG_EXISTS(state, {taskId}) {
    const newState = Object.assign({}, state);
    newState[taskId].taskFinishedLogExists = true;
    return newState;
  },
  LOG_TASK_INIT(state, {taskId, path, offset, exists}) {
    return updateTask(state, taskId, {
      path,
      exists,
      minOffset: offset,
      maxOffset: offset,
      filesize: offset,
      initialDataLoaded: true
    });
  },
  LOG_TASK_FILE_DOES_NOT_EXIST(state, {taskId}) {
    return updateTask(state, taskId, {exists: false, initialDataLoaded: true});
  },
  LOG_SCROLL_TO_TOP(state, {taskIds}) {
    let newState = Object.assign({}, state);
    for (let i = 0; i < taskIds.length; i++) {
      let taskId = taskIds[i];
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: 0, maxOffset: 0, logDataLoaded: false});
    }
    return newState;
  },
  LOG_SCROLL_ALL_TO_TOP(state) {
    let newState = {};
    for (let taskId in state) {
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: 0, maxOffset: 0, logDataLoaded: false});
    }
    return newState;
  },
  LOG_SCROLL_TO_BOTTOM(state, {taskIds}) {
    let newState = Object.assign({}, state);
    for (let i = 0; i < taskIds.length; i++) {
      let taskId = taskIds[i];
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: state[taskId].filesize, maxOffset: state[taskId].filesize, logDataLoaded: false});
    }
    return newState;
  },
  LOG_SCROLL_ALL_TO_BOTTOM(state) {
    let newState = {};
    for (let taskId in state) {
      newState[taskId] = Object.assign({}, state[taskId], {minOffset: state[taskId].filesize, maxOffset: state[taskId].filesize, logDataLoaded: false});
    }
    return newState;
  },
  LOG_TASK_FILESIZE(state, {taskId, filesize}) {
    return updateTask(state, taskId, {filesize});
  },
  LOG_TASK_DATA(state, {taskId, offset, nextOffset}) {
    let {minOffset, maxOffset, filesize} = state[taskId];
    return updateTask(state, taskId, {logDataLoaded: true, minOffset: Math.min(minOffset, offset), maxOffset: Math.max(maxOffset, nextOffset), filesize: Math.max(nextOffset, filesize)});
  },
  LOG_FILE_EMPTY(state, {taskId}) {
    return updateTask(state, taskId, {logDataLoaded: true, minOffset: 0, maxOffset: 0, filesize: 0})
  },
  LOG_TASK_HISTORY(state, {taskId, taskHistory}) {
    let lastTaskStatus = getLastTaskUpdate(taskHistory.taskUpdates);
    return updateTask(state, taskId, {lastTaskStatus, terminated: isTerminalTaskState(lastTaskStatus)});
  },
  LOG_REMOVE_TASK_GROUP(state, {taskIds}) {
    let newState = Object.assign({}, state);
    for (let i = 0; i < taskIds.length; i++) {
      let taskId = taskIds[i];
      delete newState[taskId];
    }
    return newState;
  },
  LOG_EXPAND_TASK_GROUP(state, {taskIds}) {
    let newState = {};
    for (let i = 0; i < taskIds.length; i++) {
      let taskId = taskIds[i];
      newState[taskId] = state[taskId];
    }
    return newState;
  }
};

export default function(state={}, action) {
  if (action.type in ACTIONS) {
    return ACTIONS[action.type](state, action);
  } else {
    return state;
  }
}
function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}
