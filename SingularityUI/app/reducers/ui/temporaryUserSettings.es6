const ACTIONS = {
  UPDATE_TEMPORARY_USER_SETTINGS(state, action) {
    return { data: action.newSettings };
  },

  CLEAR_TEMPORARY_USER_SETTINGS() {
    return {};
  }
};

export default function(state = {}, action) {
  if (action.type in ACTIONS) {
    return ACTIONS[action.type](state, action);
  }
  return state;
}
