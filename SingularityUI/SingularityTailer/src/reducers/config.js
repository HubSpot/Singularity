import { SANDBOX_SET_API_ROOT } from '../actions';

const initialState = {};

const configReducer = (state = initialState, action) => {
  switch (action.type) {
    case SANDBOX_SET_API_ROOT:
      return {
        ...state,
        singularityApiRoot: action.apiRoot
      };
    default:
      return state;
  }
};

export default configReducer;
