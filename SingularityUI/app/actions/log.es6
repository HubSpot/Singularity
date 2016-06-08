import Q from 'q';

import { fetchTasksForRequest } from './activeTasks';

let fetchData = function(taskId, path, offset = undefined, length = 0) {
  length = Math.max(length, 0);  // API breaks if you request a negative length
  return $.ajax(
    {url: `${ config.apiRoot }/sandbox/${ taskId }/read?${$.param({path, length, offset})}`});
};

let fetchTaskHistory = taskId =>
  $.ajax(
    {url: `${ config.apiRoot }/history/task/${ taskId }`})
;

let initializeUsingActiveTasks = (requestId, path, search) =>
  function(dispatch) {
    let deferred = Q.defer();
    fetchTasksForRequest(requestId).done(function(tasks) {
      let taskIds = _.sortBy(_.pluck(tasks, 'taskId'), taskId => taskId.instanceNo).map(taskId => taskId.id);
      return dispatch(initialize(requestId, path, search, taskIds)).then(() => deferred.resolve());
    });
    return deferred.promise;
  }
;

var initialize = (requestId, path, search, taskIds) =>
  function(dispatch, getState) {
    let { viewMode } = getState();

    let taskIdGroups;
    if (viewMode === 'unified') {
      taskIdGroups = [taskIds];
    } else {
      taskIdGroups = taskIds.map(taskId => [taskId]);
    }

    dispatch(init(requestId, taskIdGroups, path, search));

    let groupPromises = taskIdGroups.map(function(taskIds, taskGroupId) {
      let taskInitPromises = taskIds.map(function(taskId) {
        let taskInitDeferred = Q.defer();
        let resolvedPath = path.replace('$TASK_ID', taskId);
        fetchData(taskId, resolvedPath).done(function({offset}) {
          dispatch(initTask(taskId, offset, resolvedPath, true));
          return taskInitDeferred.resolve();
        })
        .error(function({status}) {
          if (status === 404) {
            app.caughtError();
            dispatch(taskFileDoesNotExist(taskGroupId, taskId));
            return taskInitDeferred.resolve();
          } else {
            return taskInitDeferred.reject();
          }
        });
        return taskInitDeferred.promise;
      });

      let taskStatusPromises = taskIds.map(taskId => dispatch(updateTaskStatus(taskGroupId, taskId)));

      return Promise.all(taskInitPromises, taskStatusPromises).then(() =>
        dispatch(taskGroupFetchPrevious(taskGroupId)).then(() => dispatch(taskGroupReady(taskGroupId)))
      );
    });

    return Promise.all(groupPromises);
  }
;

var init = (requestId, taskIdGroups, path, search) =>
  ({
    requestId,
    taskIdGroups,
    path,
    search,
    type: 'LOG_INIT'
  })
;

let addTaskGroup = (taskIds, search) =>
  ({
    taskIds,
    search,
    type: 'LOG_ADD_TASK_GROUP'
  })
;

var initTask = (taskId, offset, path, exists) =>
  ({
    taskId,
    offset,
    path,
    exists,
    type: 'LOG_TASK_INIT'
  })
;

var taskFileDoesNotExist = (taskGroupId, taskId) =>
  ({
    taskId,
    taskGroupId,
    type: 'LOG_TASK_FILE_DOES_NOT_EXIST'
  })
;

var taskGroupReady = taskGroupId =>
  ({
    taskGroupId,
    type: 'LOG_TASK_GROUP_READY'
  })
;

let taskHistory = (taskGroupId, taskId, taskHistory) =>
  ({
    taskGroupId,
    taskId,
    taskHistory,
    type: 'LOG_TASK_HISTORY'
  })
;

let getTasks = (taskGroup, tasks) => taskGroup.taskIds.map(taskId => tasks[taskId]);

let updateFilesizes = () =>
  function(dispatch, getState) {
    let tasks;
    tasks = getState();
    for (let taskId of tasks) {
      fetchData(taskId, tasks[taskId.path]).done(({offset}) => {
        dispatch(taskFilesize(taskId, offset));
      });
    }
  }
;


let updateGroups = () =>
  (dispatch, getState) =>
    getState().taskGroups.map(function(taskGroup, taskGroupId) {
      if (!taskGroup.pendingRequests) {
        if (taskGroup.top) {
          dispatch(taskGroupFetchPrevious(taskGroupId));
        }
        if (taskGroup.bottom || taskGroup.tailing) {
          return dispatch(taskGroupFetchNext(taskGroupId));
        }
      }
    })

;

let updateTaskStatuses = () =>
  function(dispatch, getState) {
    let {tasks, taskGroups} = getState();
    return taskGroups.map((taskGroup, taskGroupId) =>
      getTasks(taskGroup, tasks).map(function({taskId, terminated}) {
        if (terminated) {
          return Promise.resolve();
        } else {
          return dispatch(updateTaskStatus(taskGroupId, taskId));
        }
      })
    );
  }
;

var updateTaskStatus = (taskGroupId, taskId) =>
  (dispatch, getState) =>
    fetchTaskHistory(taskId, ['taskUpdates']).done(data => dispatch(taskHistory(taskGroupId, taskId, data)))

;

var taskGroupFetchNext = taskGroupId =>
  function(dispatch, getState) {
    let {tasks, taskGroups, logRequestLength, maxLines} = getState();

    let taskGroup = taskGroups[taskGroupId];
    tasks = getTasks(taskGroup, tasks);

    // bail early if there's already a pending request
    if (taskGroup.pendingRequests) {
      return Promise.resolve();
    }

    dispatch({taskGroupId, type: 'LOG_REQUEST_START'});
    let promises = tasks.map(function({taskId, exists, maxOffset, path, initialDataLoaded}) {
      if (initialDataLoaded && exists !== false) {
        let xhr = fetchData(taskId, path, maxOffset, logRequestLength);
        return xhr.done(function({data, offset, nextOffset}) {
          if (data.length > 0) {
            nextOffset = offset + data.length;
            return dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, true, maxLines));
          }
        });
      } else {
        return Promise.resolve(); // reject("initialDataLoaded is false for task #{taskId}")
      }
    });

    return Promise.all(promises).then(() => dispatch({taskGroupId, type: 'LOG_REQUEST_END'}));
  }
;

var taskGroupFetchPrevious = taskGroupId =>
  function(dispatch, getState) {
    let {tasks, taskGroups, logRequestLength, maxLines} = getState();

    let taskGroup = taskGroups[taskGroupId];
    tasks = getTasks(taskGroup, tasks);

    // bail early if all tasks are at the top
    if (_.all(tasks.map(({minOffset}) => minOffset === 0))) {
      return Promise.resolve();
    }

    // bail early if there's already a pending request
    if (taskGroup.pendingRequests) {
      return Promise.resolve();
    }

    dispatch({taskGroupId, type: 'LOG_REQUEST_START'});
    let promises = tasks.map(function({taskId, exists, minOffset, path, initialDataLoaded}) {
      if (minOffset > 0 && initialDataLoaded && exists !== false) {
        let xhr = fetchData(taskId, path, Math.max(minOffset - logRequestLength, 0), Math.min(logRequestLength, minOffset));
        return xhr.done(function({data, offset, nextOffset}) {
          if (data.length > 0) {
            nextOffset = offset + data.length;
            return dispatch(taskData(taskGroupId, taskId, data, offset, nextOffset, false, maxLines));
          }
        });
      } else {
        return Promise.resolve(); // reject("initialDataLoaded is false for task #{taskId}")
      }
    });

    return Promise.all(promises).then(() => dispatch({taskGroupId, type: 'LOG_REQUEST_END'}));
  }
;

var taskData = (taskGroupId, taskId, data, offset, nextOffset, append, maxLines) =>
  ({
    taskGroupId,
    taskId,
    data,
    offset,
    nextOffset,
    append,
    maxLines,
    type: 'LOG_TASK_DATA'
  })
;

let taskFilesize = (taskId, filesize) =>
  ({
    taskId,
    filesize,
    type: 'LOG_TASK_FILESIZE'
  })
;

let taskGroupTop = (taskGroupId, visible) =>
  function(dispatch, getState) {
    if (getState().taskGroups[taskGroupId].top !== visible) {
      dispatch({taskGroupId, visible, type: 'LOG_TASK_GROUP_TOP'});
      if (visible) {
        return dispatch(taskGroupFetchPrevious(taskGroupId));
      }
    }
  }
;

let taskGroupBottom = (taskGroupId, visible, tailing = false) =>
  function(dispatch, getState) {
    let { taskGroups, tasks } = getState();
    let taskGroup = taskGroups[taskGroupId];
    if (taskGroup.tailing !== tailing) {
      if (tailing === false || _.all(getTasks(taskGroup, tasks).map(({maxOffset, filesize}) => maxOffset >= filesize))) {
        dispatch({taskGroupId, tailing, type: 'LOG_TASK_GROUP_TAILING'});
      }
    }
    if (taskGroup.bottom !== visible) {
      dispatch({taskGroupId, visible, type: 'LOG_TASK_GROUP_BOTTOM'});
      if (visible) {
        return dispatch(taskGroupFetchNext(taskGroupId));
      }
    }
  }
;

let clickPermalink = offset =>
  ({
    offset,
    type: 'LOG_CLICK_OFFSET_LINK'
  })
;

let selectLogColor = color =>
  ({
    color,
    type: 'LOG_SELECT_COLOR'
  })
;

let switchViewMode = newViewMode =>
  function(dispatch, getState) {
    let { taskGroups, path, activeRequest, search, viewMode } = getState();

    if (__in__(newViewMode, ['custom', viewMode])) {
      return;
    }

    let taskIds = _.flatten(_.pluck(taskGroups, 'taskIds'));

    dispatch({viewMode: newViewMode, type: 'LOG_SWITCH_VIEW_MODE'});
    return dispatch(initialize(activeRequest.requestId, path, search, taskIds));
  }
;

let setCurrentSearch = newSearch =>  // TODO: can we do something less heavyweight?
  function(dispatch, getState) {
    let {activeRequest, path, taskGroups, currentSearch} = getState();
    if (newSearch !== currentSearch) {
      return dispatch(initialize(activeRequest.requestId, path, newSearch, _.flatten(_.pluck(taskGroups, 'taskIds'))));
    }
  }
;

let toggleTaskLog = taskId =>
  function(dispatch, getState) {
    let {search, path, tasks, viewMode} = getState();
    if (taskId in tasks) {
      // only remove task if it's not the last one
      if (Object.keys(tasks).length > 1) {
        return dispatch({taskId, type: 'LOG_REMOVE_TASK'});
      } else {
        return;
      }
    } else {
      if (viewMode === 'split') {
        dispatch(addTaskGroup([taskId], search));
      }

      let resolvedPath = path.replace('$TASK_ID', taskId);

      return fetchData(taskId, resolvedPath).done(function({offset}) {
        dispatch(initTask(taskId, offset, resolvedPath, true));

        return getState().taskGroups.map(function(taskGroup, taskGroupId) {
          if (__in__(taskId, taskGroup.taskIds)) {
            dispatch(updateTaskStatus(taskGroupId, taskId));
            return dispatch(taskGroupFetchPrevious(taskGroupId)).then(() => dispatch(taskGroupReady(taskGroupId)));
          }
        });
      });
    }
  }
;

let removeTaskGroup = taskGroupId =>
  function(dispatch, getState) {
    let { taskIds } = getState().taskGroups[taskGroupId];
    return dispatch({taskGroupId, taskIds, type: 'LOG_REMOVE_TASK_GROUP'});
  }
;

let expandTaskGroup = taskGroupId =>
  function(dispatch, getState) {
    let { taskIds } = getState().taskGroups[taskGroupId];
    return dispatch({taskGroupId, taskIds, type: 'LOG_EXPAND_TASK_GROUP'});
  }
;

let scrollToTop = taskGroupId =>
  function(dispatch, getState) {
    let { taskIds } = getState().taskGroups[taskGroupId];
    dispatch({taskGroupId, taskIds, type: 'LOG_SCROLL_TO_TOP'});
    return dispatch(taskGroupFetchNext(taskGroupId));
  }
;

let scrollAllToTop = () =>
  function(dispatch, getState) {
    dispatch({type: 'LOG_SCROLL_ALL_TO_TOP'});
    return getState().taskGroups.map((taskGroup, taskGroupId) => dispatch(taskGroupFetchNext(taskGroupId)));
  }
;

let scrollToBottom = taskGroupId =>
  function(dispatch, getState) {
    let { taskIds } = getState().taskGroups[taskGroupId];
    dispatch({taskGroupId, taskIds, type: 'LOG_SCROLL_TO_BOTTOM'});
    return dispatch(taskGroupFetchPrevious(taskGroupId));
  }
;

let scrollAllToBottom = () =>
  function(dispatch, getState) {
    dispatch({type: 'LOG_SCROLL_ALL_TO_BOTTOM'});
    return getState().taskGroups.map((taskGroup, taskGroupId) => dispatch(taskGroupFetchPrevious(taskGroupId)));
  }
;

export default { initialize, initializeUsingActiveTasks, taskGroupFetchNext, taskGroupFetchPrevious, clickPermalink, updateGroups, updateTaskStatuses, updateFilesizes, taskGroupTop, taskGroupBottom, selectLogColor, switchViewMode, setCurrentSearch, toggleTaskLog, scrollToTop, scrollAllToTop, scrollToBottom, scrollAllToBottom, removeTaskGroup, expandTaskGroup };

function __in__(needle, haystack) {
  return haystack.indexOf(needle) >= 0;
}
