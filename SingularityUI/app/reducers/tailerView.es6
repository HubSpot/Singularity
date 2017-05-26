import {
  SET_TAILER_GROUPS,
  ADD_TAILER_GROUP,
  SET_SEARCH,
  REMOVE_TAILER_GROUP,
  PICK_TAILER_GROUP,
  SET_COLOR,
  TAILER_SET_NOT_FOUND

} from '../actions/tailer';

import Utils from '../utils';

import _ from 'underscore';

const initialState = {
  tailerGroups: [],
  requestIds: [],
  taskIds: [],
  paths: [],
  viewMode: 'split',
  ready: false,
  search: null,
  color: 'default',
  notFound: {}
}

const splice = (array, index, length=1) => [...array.slice(0, index), ...array.slice(index+length)];

export const buildTailerId = (index, taskId, path) => `${index}-${taskId}/${path}`;

const generateTailerState = (tailerGroups) => {
  const tailerGroupsWithTailerId = tailerGroups.map((tailerGroup, i) =>
    tailerGroup.map((tailer) => ({
      ...tailer,
      tailerId: buildTailerId(i, tailer.taskId, tailer.path)
    })));

  const flattenedTasks = [].concat.apply([], tailerGroups);
  const sortedUniqueTaskIds = _.uniq(flattenedTasks.map((task) => task.taskId).sort(), true);
  const sortedUniquePaths = _.uniq(flattenedTasks.map(({path}) => path).sort(), true);

  return {
    tailerGroups: tailerGroupsWithTailerId,
    requestIds: _.uniq(sortedUniqueTaskIds.map(Utils.getRequestIdFromTaskId), true),
    taskIds: sortedUniqueTaskIds,
    paths: sortedUniquePaths,
  }
}

export default (state = initialState, action) => {
  switch (action.type) {
    case SET_SEARCH:
      return {
        ...state,
        search: action.search
      };
    case SET_TAILER_GROUPS:
      return {
        ...state,
        ...generateTailerState(action.tailerGroups),
        ready: true
      };
    case ADD_TAILER_GROUP:
      return {
        ...state,
        ...generateTailerState([...state.tailerGroups, action.tailerGroup])
      }
    case REMOVE_TAILER_GROUP:
      return {
        ...state,
        ...generateTailerState(splice(state.tailerGroups, action.tailerGroupIndex)),
      };
    case PICK_TAILER_GROUP:
      return {
        ...state,
        ...generateTailerState([state.tailerGroups[action.tailerGroupIndex]]),
      }
    case SET_COLOR:
      return {
        ...state,
        color: action.color
      };
    case TAILER_SET_NOT_FOUND:
      return {
        ...state,
        notFound: action.notFound
      };
    default:
      return state;
  }
};