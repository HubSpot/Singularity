import {
  SET_TAILER_GROUPS,
  SET_SEARCH
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
  search: null
}

export default (state = initialState, action) => {
  switch (action.type) {
    case SET_SEARCH:
      return {
        ...state,
        search: action.search
      };
    case SET_TAILER_GROUPS:
      const tailerGroupsWithKey = action.tailerGroups.map((tailerGroup, i) =>
        tailerGroup.map((tailer) => ({
          ...tailer,
          tailerId: `${i}-${tailer.taskId}/${tailer.path}`
        })));
      const flattenedTasks = [].concat.apply([], action.tailerGroups);
      const sortedUniqueTaskIds = _.uniq(flattenedTasks.map(({taskId}) => taskId).sort());
      const sortedUniquePaths = _.uniq(flattenedTasks.map(({path}) => path).sort());

      return {
        ...state,
        tailerGroups: tailerGroupsWithKey,
        requestIds: _.uniq(sortedUniqueTaskIds.map(Utils.getRequestIdFromTaskId), true),
        taskIds: sortedUniqueTaskIds,
        paths: sortedUniquePaths,
        ready: true
      };
    default:
      return state;
  }
}