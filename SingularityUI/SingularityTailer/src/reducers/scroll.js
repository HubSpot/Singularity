import {
  RENDERED_LINES
} from '../actions';

const scrollReducer = (state = {}, action) => {
  switch (action.type) {
    case RENDERED_LINES:
      return {
        ...state,
        [action.id]: {
          startIndex: action.startIndex,
          stopIndex: action.stopIndex,
          overscanStartIndex: action.overscanStartIndex,
          overscanStopIndex: action.overscanStopIndex
        }
      };
    default:
      return state;
  }
};

export default scrollReducer;
