export const TOGGLE_REQUEST_STAR = 'TOGGLE_REQUEST_STAR';

export const ToggleRequestStar = (requestId) => {
  return (dispatch) => {
    let starredRequests = JSON.parse(localStorage.getItem('starredRequests')) || [];
    if (_.contains(starredRequests, requestId)) {
      starredRequests = _.without(starredRequests, requestId);
    } else {
      starredRequests.push(requestId);
    }
    localStorage.setItem('starredRequests', JSON.stringify(starredRequests));

    dispatch({
      type: TOGGLE_REQUEST_STAR,
      value: starredRequests
    });
  };
};
