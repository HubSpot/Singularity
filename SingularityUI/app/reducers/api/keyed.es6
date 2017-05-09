import buildApiActionReducer from './base';

export default function buildKeyedApiActionReducer(ActionGroup, ...args) {
  const baseReducer = buildApiActionReducer(ActionGroup, ...args);

  return function reducer(state = {}, action) {
    if (action.type === ActionGroup.CLEAR) {
      return {};
    } else if (_.contains([ActionGroup.ERROR, ActionGroup.SUCCESS, ActionGroup.STARTED], action.type)) {
      const newState = {};
      newState[action.key] = baseReducer(state[action.key], action);
      return _.extend({}, state, newState);
    }
    return state;
  };
}
