export const CHANGE_REQUEST_STAR = 'CHANGE_REQUEST_STAR';

export const changeRequestStar = (requestId) => {
  return (dispatch, getState) => {
    dispatch({
      type: CHANGE_REQUEST_STAR,
      value: requestId
    });
    window.localStorage.starredRequests = JSON.stringify(getState().ui.starred);
  };
};
