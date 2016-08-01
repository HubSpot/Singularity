import {
  SANDBOX_SET_API_ROOT,
  TOGGLE_ANSI_COLORING
} from '../actions';

const initialState = {
  parseAnsi: true
};

const configReducer = (state = initialState, action) => {
  switch (action.type) {
    case SANDBOX_SET_API_ROOT:
      return {
        ...state,
        singularityApiRoot: action.apiRoot
      };
    case TOGGLE_ANSI_COLORING:
      return {
        ...state,
        parseAnsi: !state.parseAnsi
      };
    default:
      return state;
  }
};

export default configReducer;
