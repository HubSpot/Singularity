import { actions as tailerActions } from 'singularityui-tailer';

export const SET_TAILER_GROUPS = "TAILER_SET_TAILER_GROUPS";
export const SET_SEARCH = "TAILER_SET_SEARCH";

const setTailerGroups = (tailerGroups) => ({
  type: SET_TAILER_GROUPS,
  tailerGroups
});

const setSearch = (search) => ({
  type: SET_SEARCH,
  search
});

const jumpToBottom = (id, taskId, path) => (dispatch, getState) => {
  const state = getState();

  dispatch(tailerActions.unloadFile(id));
  dispatch(tailerActions.sandboxFetchTail(id, taskId, path, state.tailer.config));
}

const jumpAllToBottom = () => (dispatch, getState) => {
  const state = getState();

  state.tailerView.tailerGroups.map((tailerGroup) => tailerGroup.map((tailer) => {
    dispatch(jumpToBottom(tailer.tailerId, tailer.taskId, tailer.path, state.tailer.config));
  }));
}

const jumpToTop = (id, taskId, path) => (dispatch, getState) => {
  const state = getState();

  dispatch(tailerActions.unloadFile(id));
  dispatch(tailerActions.sandboxFetchLength(id, taskId, path)).then(() =>
    dispatch(tailerActions.sandboxFetchChunk(id, taskId, path, 0, tailerActions.SANDBOX_MAX_BYTES, state.tailer.config)));
}

const jumpAllToTop = () => (dispatch, getState) => {
  const state = getState();

  state.tailerView.tailerGroups.map((tailerGroup) => tailerGroup.map((tailer) => {
    dispatch(jumpToTop(tailer.tailerId, tailer.taskId, tailer.path, state.tailer.config));
  }));
}

export { setTailerGroups, setSearch, jumpToBottom, jumpToTop, jumpAllToTop, jumpAllToBottom };