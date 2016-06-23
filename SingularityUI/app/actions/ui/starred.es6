export const CHANGE_REQUEST_STAR = 'CHANGE_REQUEST_STAR';

export const changeRequestStar = (requestId) => ({
  type: CHANGE_REQUEST_STAR,
  value: requestId
});

export const changeRequestStarAndSave = (requestId) => {
  return (dispatch, getState) => {
    dispatch(changeRequestStar(requestId));
    window.localStorage.starredRequests = JSON.stringify(getState().ui.starred);
  };
};
