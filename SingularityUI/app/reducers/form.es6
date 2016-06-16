const ACTIONS = {
    MODIFY_FORM_FIELD(state, {formId, fieldId, newValue}) {
        let newState = Object.assign({}, state);
        if (!newState[formId]) {
            newState[formId] = {};
        }
        newState[formId][fieldId] = newValue;
        if (state[formId].modifications) {
            newState[formId].modifications = state[formId].modifications + 1;
        } else {
            newState[formId].modifications = 1;
        }
        return newState;
    },

    CLEAR_FORM_FIELD(state, {formId, fieldId}) {
        return MODIFY_FORM_FIELD(state, {formId, fieldId, undefined});
    },

    CLEAR_FORM(state, {formId}) {
        let newState = Object.assign({}, state);
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
