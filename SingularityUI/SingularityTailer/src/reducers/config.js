import {
  SANDBOX_SET_API_ROOT,
  BLAZAR_SET_API_ROOT,
  TOGGLE_ANSI_COLORING,
  TOGGLE_FETCH_OVERSCAN,
  SET_TAIL_INTERVAL_MS,
  SET_AUTHORIZATION_HEADER
} from '../actions';

const initialState = {
  parseAnsi: true,
  fetchOverscan: true,
  tailIntervalMs: 5000
};

const configReducer = (state = initialState, action) => {
  switch (action.type) {
    case SANDBOX_SET_API_ROOT:
      return {
        ...state,
        singularityApiRoot: action.apiRoot
      };
    case BLAZAR_SET_API_ROOT:
      return {
        ...state,
        blazarApiRoot: action.apiRoot
      };
    case TOGGLE_ANSI_COLORING:
      return {
        ...state,
        parseAnsi: !state.parseAnsi
      };
    case TOGGLE_FETCH_OVERSCAN:
      return {
        ...state,
        fetchOverscan: !state.fetchOverscan
      };
    case SET_TAIL_INTERVAL_MS:
      return {
        ...state,
        tailIntervalMs: action.tailIntervalMs
      }
    case SET_AUTHORIZATION_HEADER:
      return {
        ...state,
        authorizationHeader: action.authorizationHeader
      }
    default:
      return state;
  }
};

export default configReducer;
