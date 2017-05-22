import { actions as tailerActions } from 'singularityui-tailer';
import { push } from 'react-router-redux';
import Utils from '../utils';

export const SET_TAILER_GROUPS = "TAILER_SET_TAILER_GROUPS";
export const ADD_TAILER_GROUP = "TAILER_ADD_TAILER_GROUP";
export const SET_SEARCH = "TAILER_SET_SEARCH";
export const REMOVE_TAILER_GROUP = "TAILER_REMOVE_TAILER_GROUP";
export const PICK_TAILER_GROUP = "TAILER_PICK_TAILER_GROUP";
export const TOGGLE_TAILER_GROUP = "TAILER_TOGGLE_TAILER_GROUP";
export const SET_COLOR = "TAILER_SET_COLOR";
export const TAILER_SET_NOT_FOUND = "TAILER_SET_NOT_FOUND";

export const buildTailerGroupInfo = (taskId, path, offset=-1) => ({taskId, path, offset});

export const setTailerGroups = (tailerGroups) => ({
  type: SET_TAILER_GROUPS,
  tailerGroups
});

export const setSearch = (search) => ({
  type: SET_SEARCH,
  search
});

export const markNotFound = (taskId) => (dispatch, getState) => {
  const { tailerView } = getState();
  const notFound = tailerView.notFound;
  notFound[taskId] = true;
  dispatch({
    type: TAILER_SET_NOT_FOUND,
    notFound: notFound
  });
}

export const jumpToBottom = (id, taskId, path) => (dispatch, getState) => {
  const state = getState();

  dispatch(tailerActions.unloadFile(id));
  dispatch(tailerActions.sandboxFetchTail(id, taskId, path.replace('$TASK_ID', taskId), state.tailer.config))
  dispatch(tailerActions.startTailing(id));
}

export const jumpAllToBottom = () => (dispatch, getState) => {
  const state = getState();

  state.tailerView.tailerGroups.map((tailerGroup) => tailerGroup.map((tailer) => {
    dispatch(jumpToBottom(tailer.tailerId, tailer.taskId, tailer.path));
  }));
}

export const jumpToTop = (id, taskId, path) => (dispatch, getState) => {
  const state = getState();

  dispatch(tailerActions.unloadFile(id));
  dispatch(tailerActions.sandboxFetchLength(id, taskId, path.replace('$TASK_ID', taskId), state.tailer.config));
  dispatch(tailerActions.sandboxFetchChunk(id, taskId, path.replace('$TASK_ID', taskId), 0, tailerActions.SANDBOX_MAX_BYTES, state.tailer.config));
  $.find('log-pane').scrollTop = 0;
  dispatch(tailerActions.stopTailing(id));
}

export const jumpAllToTop = () => (dispatch, getState) => {
  const state = getState();

  state.tailerView.tailerGroups.map((tailerGroup) => tailerGroup.map((tailer) => {
    dispatch(jumpToTop(tailer.tailerId, tailer.taskId, tailer.path));
  }));
}

export const removeTailerGroup = (tailerGroupIndex) => (dispatch) => {
  dispatch({type: REMOVE_TAILER_GROUP, tailerGroupIndex});
  dispatch(updateTailerUrl());
}

export const pickTailerGroup = (tailerGroupIndex) => (dispatch) => {
  dispatch({type: PICK_TAILER_GROUP, tailerGroupIndex});
  dispatch(updateTailerUrl());
}

export const loadColor = () => (dispatch) => {
  if (window.localStorage.hasOwnProperty('logColor')) {
    return dispatch({type: SET_COLOR, color: window.localStorage['logColor']});
  } else {
    return Promise.resolve();
  }
}

export const setColor = (color) => (dispatch) => {
  window.localStorage['logColor'] = color;
  return dispatch({type: SET_COLOR, color})
};

export const addTailerGroup = (tailerGroup) => (dispatch, getState) => {
  dispatch({type: ADD_TAILER_GROUP, tailerGroup});
  dispatch(updateTailerUrl());
}

export const toggleTailerGroup = (taskId, path, visibleTaskIds) => (dispatch, getState) => {
  const { tailerView } = getState();

  const tailerGroupIndex = _.findIndex(tailerView.tailerGroups, (tg) => tg[0].taskId === taskId);

  if (tailerGroupIndex > -1) {
    return dispatch(removeTailerGroup(tailerGroupIndex));
  } else {
    let newPath = path;
    if (newPath.indexOf('$TASK_ID') < 0) {
      for (let i=0; i<visibleTaskIds.length; i++) {
        newPath = newPath.replace(visibleTaskIds[i], '$TASK_ID');
      }
    }
    return dispatch(addTailerGroup([buildTailerGroupInfo(taskId, newPath)]));
  }
}

export const updateTailerUrl = () => (dispatch, getState) => {
  const { tailerView } = getState();

  let path = tailerView.paths[0];
  if (path.indexOf('$TASK_ID') < 0) {
    for (let i=0; i<tailerView.taskIds.length; i++) {
      path = path.replace(tailerView.taskIds[i], '$TASK_ID');
    }
  }

  if (tailerView.taskIds.length === 1) {
    // task tailer
    return dispatch(push(`/task/${tailerView.taskIds[0]}/tail/${path}`));
  } if (tailerView.requestIds.length === 1) {
    // request tailer
    return dispatch(push(`/request/${tailerView.requestIds[0]}/tail/${path}?instance=${tailerView.taskIds.map(Utils.getInstanceNoFromTaskId).join(',')}`));
  } else if (tailerView.paths.length === 1) {
    // custom tailer
    return dispatch(push(`/tail/${path}?taskIds=${tailerView.taskIds.join(',')}`));
  } else {
    // ur fucked
  }
}