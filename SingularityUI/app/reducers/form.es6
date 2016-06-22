import Utils from '../utils'

const ACTIONS = {
  MODIFY_FORM_FIELD(state, {formId, fieldId, newValue}) {
    let newState = Utils.deepClone(state);
    if (!newState[formId]) {
      newState[formId] = {};
    }
    newState[formId][fieldId] = newValue;
    return newState;
  },

  CLEAR_FORM_FIELD(state, {formId, fieldId}) {
    return MODIFY_FORM_FIELD(state, {formId, fieldId, newValue: undefined});
  },

  CLEAR_FORM(state, {formId}) {
    let newState = Utils.deepClone({}, state);
    newState[formId] = {};
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
