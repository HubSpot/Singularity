import { ADD_CHUNK } from '../actions';

export default function reducer(state = {}, action) {
  const { value } = action;
  switch (action.type) {
    case ADD_CHUNK:
      return {
        ...state,
      };
    default:
      return state;
  }
}
