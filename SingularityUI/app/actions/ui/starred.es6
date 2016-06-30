export const TOGGLE_REQUEST_STAR = 'TOGGLE_REQUEST_STAR';

export const toggleRequestStar = (requestId) => {
  return (dispatch, getState) => {
    dispatch({
      type: TOGGLE_REQUEST_STAR,
      value: requestId
    });
    window.localStorage.starredRequests = JSON.stringify(getState().ui.starred);
  };
};
