import {
  FETCH_CHUNK_STARTED,
  FETCH_CHUNK_ERROR,
  ADD_FILE_CHUNK
} from '../actions';

import { Map } from 'immutable';

const initialState = new Map();

const requestsForIdReducer = (state = initialState, action) => {
  switch (action.type) {
    case FETCH_CHUNK_STARTED:
      return state.set(action.start, {
        apiName: action.apiName,
        startedAt: action.startedAt,
        id: action.id,
        start: action.start,
        end: action.end
      });
    case FETCH_CHUNK_ERROR:
      return state.delete(action.start);
    case ADD_FILE_CHUNK:
      return state.delete(action.requestedStart);
    default:
      return state;
  }
};


const requestsReducer = (state = {}, action) => {
  switch (action.type) {
    case FETCH_CHUNK_STARTED:
    case FETCH_CHUNK_ERROR:
    case ADD_FILE_CHUNK:
      return {
        ...state,
        [action.id]: requestsForIdReducer(
          state[action.id],
          action
        )
      };
    default:
      return state;
  }
};

export default requestsReducer;
