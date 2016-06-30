import * as StarredActions from '../../actions/ui/starred';

const initialState = window.localStorage.hasOwnProperty('starredRequests')
  ? JSON.parse(window.localStorage.getItem('starredRequests'))
  : [];

// really not great of grabbing the starred requests from localStorage
// revisit this with an actual pure function later
// (maybe a subscriber will fix this)
const starredRequests = (state = initialState, action) => {

  if (action.type === StarredActions.CHANGE_REQUEST_STAR) {
    return action.value;
  }

  return state;
};

export default starredRequests;
