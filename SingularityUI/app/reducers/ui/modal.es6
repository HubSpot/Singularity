const initialState = {
  open: new Map()
};

const ACTIONS = {
  SHOW_MODAL(state, {modalType, payload}) {
    return {
      ...state,
      open: new Map(state.open).set(modalType, payload)
    };
  },

  HIDE_MODAL(state, {modalType}) {
    return {
      ...state,
      open: new Map(state.open).delete(modalType)
    };
  }
};

export default function(state = initialState, action) {
  if (action.type in ACTIONS) {
    return ACTIONS[action.type](state, action);
  }
  return state;
}
