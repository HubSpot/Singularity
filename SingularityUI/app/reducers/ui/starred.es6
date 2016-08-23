import * as StarredActions from '../../actions/ui/starred';

const initialState = window.localStorage.hasOwnProperty('starredRequests')
  ? JSON.parse(window.localStorage.getItem('starredRequests'))
  : [];

export default (state = initialState, action) => {
  if (action.type === StarredActions.TOGGLE_LOCAL_REQUEST_STAR) {
    return action.value;
  }

  return state;
};
