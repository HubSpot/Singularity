import * as StarredActions from '../../actions/ui/starred';

// really not great of grabbing the starred requests from localStorage
// revisit this with an actual pure function later
// (maybe a subscriber will fix this)
const starredRequests = (state = [], action) => {
  if (action.type === StarredActions.GET_STARRED_REQUESTS) {
    if (window.localStorage.hasOwnProperty('starredRequests')) {
      return JSON.parse(window.localStorage.starredRequests);
    }
  } else if (action.type === StarredActions.CHANGE_REQUEST_STAR) {
    let newState;
    if (state.indexOf(action.value) > -1) {
      // remove
      newState = state.filter((requestId) => requestId !== action.value);
    } else {
      newState = [...state, action.value];
    }
    // This part is the bad part
    // persist the starredRequests
    window.localStorage.starredRequests = JSON.stringify(newState);
    // end bad part
    return newState;
  }

  return state;
};

export default starredRequests;
